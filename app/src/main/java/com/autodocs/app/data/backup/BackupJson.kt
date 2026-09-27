package com.autodocs.app.data.backup

import com.autodocs.app.data.entity.Car
import com.autodocs.app.data.entity.CarDocument
import com.autodocs.app.data.entity.DocumentType
import com.autodocs.app.data.entity.FuelType
import com.autodocs.app.data.entity.MaintenanceRule
import com.autodocs.app.data.entity.MileageEntry
import com.autodocs.app.data.entity.Photo
import com.autodocs.app.data.entity.PhotoKind
import com.autodocs.app.data.entity.PhotoOwnerType
import com.autodocs.app.data.entity.PlannedTask
import com.autodocs.app.data.entity.ServiceRecord
import com.autodocs.app.data.entity.ServiceRecordItem
import com.autodocs.app.data.entity.Station
import com.autodocs.app.data.entity.TireSeason
import com.autodocs.app.data.entity.TireSet
import com.autodocs.app.data.entity.TireSwap
import com.autodocs.app.data.entity.TransmissionType
import com.autodocs.app.data.entity.WorkItemCategory
import com.autodocs.app.data.entity.WorkType
import org.json.JSONArray
import org.json.JSONObject

/**
 * Перетворення сутностей Room ↔ JSON для data.json у бекапі.
 * Читання навмисно поблажливе (opt*): старіший бекап без нового поля
 * відновиться зі значенням за замовчуванням.
 * Шляхи до фото тут — уже «відносні» (photos/xxx.jpg) або null:
 * їх підставляє/розв'язує [BackupManager].
 */
internal object BackupJson {

    // ---- helpers ----
    private fun JSONObject.putNullable(key: String, value: Any?): JSONObject =
        put(key, value ?: JSONObject.NULL)

    private fun JSONObject.optStringOrNull(key: String): String? =
        if (!has(key) || isNull(key)) null else getString(key)

    private fun JSONObject.optLongOrNull(key: String): Long? =
        if (!has(key) || isNull(key)) null else getLong(key)

    private fun JSONObject.optIntOrNull(key: String): Int? =
        if (!has(key) || isNull(key)) null else getInt(key)

    private fun JSONObject.optDoubleOrNull(key: String): Double? =
        if (!has(key) || isNull(key)) null else getDouble(key).takeUnless { it.isNaN() }

    private inline fun <reified E : Enum<E>> enumOr(name: String?, default: E): E =
        name?.let { n -> enumValues<E>().firstOrNull { it.name == n } } ?: default

    fun <T> JSONArray?.mapObjects(transform: (JSONObject) -> T): List<T> {
        if (this == null) return emptyList()
        return (0 until length()).map { transform(getJSONObject(it)) }
    }

    fun <T> List<T>.toJsonArray(transform: (T) -> JSONObject): JSONArray =
        JSONArray().also { arr -> forEach { arr.put(transform(it)) } }

    // ---- Car ----
    fun carToJson(c: Car, photoPath: String?): JSONObject = JSONObject()
        .put("id", c.id)
        .put("name", c.name)
        .put("make", c.make)
        .put("model", c.model)
        .put("engine", c.engine)
        .put("fuelType", c.fuelType.name)
        .put("transmissionType", c.transmissionType.name)
        .put("licensePlate", c.licensePlate)
        .put("vin", c.vin)
        .putNullable("photo", photoPath)
        .put("mileage", c.mileage)
        .put("mileageUpdatedAt", c.mileageUpdatedAt)
        .put("isArchived", c.isArchived)
        .put("createdAt", c.createdAt)

    fun carFromJson(o: JSONObject, photoUri: String?): Car = Car(
        id = o.getLong("id"),
        name = o.optString("name"),
        make = o.optString("make"),
        model = o.optString("model"),
        engine = o.optString("engine"),
        fuelType = enumOr(o.optStringOrNull("fuelType"), FuelType.UNKNOWN),
        transmissionType = enumOr(o.optStringOrNull("transmissionType"), TransmissionType.UNKNOWN),
        licensePlate = o.optString("licensePlate"),
        vin = o.optString("vin"),
        photoUri = photoUri,
        mileage = o.optInt("mileage", 0),
        mileageUpdatedAt = o.optLong("mileageUpdatedAt", System.currentTimeMillis()),
        isArchived = o.optBoolean("isArchived", false),
        createdAt = o.optLong("createdAt", System.currentTimeMillis())
    )

    // ---- WorkType ----
    fun workTypeToJson(w: WorkType): JSONObject = JSONObject()
        .put("id", w.id)
        .put("name", w.name)
        .put("category", w.category.name)
        .put("isCustom", w.isCustom)

    fun workTypeFromJson(o: JSONObject): WorkType = WorkType(
        id = o.getLong("id"),
        name = o.optString("name"),
        category = enumOr(o.optStringOrNull("category"), WorkItemCategory.ROBOTA),
        isCustom = o.optBoolean("isCustom", false)
    )

    // ---- ServiceRecord ----
    fun recordToJson(r: ServiceRecord): JSONObject = JSONObject()
        .put("id", r.id)
        .put("carId", r.carId)
        .put("date", r.date)
        .put("mileage", r.mileage)
        .putNullable("stoName", r.stoName)
        .putNullable("notes", r.notes)
        .put("createdAt", r.createdAt)

    fun recordFromJson(o: JSONObject): ServiceRecord = ServiceRecord(
        id = o.getLong("id"),
        carId = o.getLong("carId"),
        date = o.getLong("date"),
        mileage = o.optInt("mileage", 0),
        stoName = o.optStringOrNull("stoName"),
        notes = o.optStringOrNull("notes"),
        createdAt = o.optLong("createdAt", System.currentTimeMillis())
    )

    // ---- ServiceRecordItem ----
    fun itemToJson(i: ServiceRecordItem): JSONObject = JSONObject()
        .put("id", i.id)
        .put("recordId", i.recordId)
        .putNullable("workTypeId", i.workTypeId)
        .putNullable("customName", i.customName)
        .put("category", i.category.name)
        .put("price", i.price)

    fun itemFromJson(o: JSONObject): ServiceRecordItem = ServiceRecordItem(
        id = o.getLong("id"),
        recordId = o.getLong("recordId"),
        workTypeId = o.optLongOrNull("workTypeId"),
        customName = o.optStringOrNull("customName"),
        category = enumOr(o.optStringOrNull("category"), WorkItemCategory.ROBOTA),
        price = o.optDouble("price", 0.0).takeUnless { it.isNaN() } ?: 0.0
    )

    // ---- MaintenanceRule ----
    fun ruleToJson(r: MaintenanceRule): JSONObject = JSONObject()
        .put("id", r.id)
        .put("carId", r.carId)
        .put("workTypeId", r.workTypeId)
        .putNullable("intervalKm", r.intervalKm)
        .putNullable("intervalMonths", r.intervalMonths)
        .putNullable("lastDoneMileage", r.lastDoneMileage)
        .putNullable("lastDoneDate", r.lastDoneDate)
        .put("isActive", r.isActive)

    fun ruleFromJson(o: JSONObject): MaintenanceRule = MaintenanceRule(
        id = o.getLong("id"),
        carId = o.getLong("carId"),
        workTypeId = o.getLong("workTypeId"),
        intervalKm = o.optIntOrNull("intervalKm"),
        intervalMonths = o.optIntOrNull("intervalMonths"),
        lastDoneMileage = o.optIntOrNull("lastDoneMileage"),
        lastDoneDate = o.optLongOrNull("lastDoneDate"),
        isActive = o.optBoolean("isActive", true)
    )

    // ---- MileageEntry ----
    fun mileageToJson(m: MileageEntry): JSONObject = JSONObject()
        .put("id", m.id)
        .put("carId", m.carId)
        .put("mileage", m.mileage)
        .put("date", m.date)

    fun mileageFromJson(o: JSONObject): MileageEntry = MileageEntry(
        id = o.getLong("id"),
        carId = o.getLong("carId"),
        mileage = o.optInt("mileage", 0),
        date = o.optLong("date", System.currentTimeMillis())
    )

    // ---- Photo ----
    fun photoToJson(p: Photo, path: String): JSONObject = JSONObject()
        .put("id", p.id)
        .put("ownerType", p.ownerType.name)
        .put("ownerId", p.ownerId)
        .put("kind", p.kind.name)
        .put("photo", path)
        .put("createdAt", p.createdAt)

    /** null, якщо тип власника/фото невідомий (з новішої версії) — такий запис пропускаємо. */
    fun photoFromJson(o: JSONObject, uri: String): Photo? {
        val owner = o.optStringOrNull("ownerType")?.let { n -> PhotoOwnerType.entries.firstOrNull { it.name == n } } ?: return null
        val kind = o.optStringOrNull("kind")?.let { n -> PhotoKind.entries.firstOrNull { it.name == n } } ?: return null
        return Photo(
            id = o.getLong("id"),
            ownerType = owner,
            ownerId = o.getLong("ownerId"),
            kind = kind,
            uri = uri,
            createdAt = o.optLong("createdAt", System.currentTimeMillis())
        )
    }

    fun optPhotoPath(o: JSONObject): String? = o.optStringOrNull("photo")

    // ---- CarDocument (v2) ----
    fun documentToJson(d: CarDocument): JSONObject = JSONObject()
        .put("id", d.id)
        .put("carId", d.carId)
        .put("type", d.type.name)
        .put("title", d.title)
        .put("number", d.number)
        .put("company", d.company)
        .putNullable("validFrom", d.validFrom)
        .put("validUntil", d.validUntil)
        .putNullable("price", d.price)
        .putNullable("notes", d.notes)
        .put("createdAt", d.createdAt)

    fun documentFromJson(o: JSONObject): CarDocument = CarDocument(
        id = o.getLong("id"),
        carId = o.getLong("carId"),
        type = enumOr(o.optStringOrNull("type"), DocumentType.OTHER),
        title = o.optString("title"),
        number = o.optString("number"),
        company = o.optString("company"),
        validFrom = o.optLongOrNull("validFrom"),
        validUntil = o.getLong("validUntil"),
        price = o.optDoubleOrNull("price"),
        notes = o.optStringOrNull("notes"),
        createdAt = o.optLong("createdAt", System.currentTimeMillis())
    )

    // ---- PlannedTask (v2) ----
    fun taskToJson(t: PlannedTask): JSONObject = JSONObject()
        .put("id", t.id)
        .put("carId", t.carId)
        .put("title", t.title)
        .putNullable("dueDate", t.dueDate)
        .putNullable("dueMileage", t.dueMileage)
        .putNullable("notes", t.notes)
        .putNullable("doneAt", t.doneAt)
        .putNullable("doneRecordId", t.doneRecordId)
        .put("createdAt", t.createdAt)

    fun taskFromJson(o: JSONObject): PlannedTask = PlannedTask(
        id = o.getLong("id"),
        carId = o.getLong("carId"),
        title = o.optString("title"),
        dueDate = o.optLongOrNull("dueDate"),
        dueMileage = o.optIntOrNull("dueMileage"),
        notes = o.optStringOrNull("notes"),
        doneAt = o.optLongOrNull("doneAt"),
        doneRecordId = o.optLongOrNull("doneRecordId"),
        createdAt = o.optLong("createdAt", System.currentTimeMillis())
    )

    // ---- Station (v2) ----
    fun stationToJson(s: Station): JSONObject = JSONObject()
        .put("id", s.id)
        .put("name", s.name)
        .put("phone", s.phone)
        .put("address", s.address)
        .putNullable("notes", s.notes)
        .put("createdAt", s.createdAt)

    fun stationFromJson(o: JSONObject): Station = Station(
        id = o.getLong("id"),
        name = o.optString("name"),
        phone = o.optString("phone"),
        address = o.optString("address"),
        notes = o.optStringOrNull("notes"),
        createdAt = o.optLong("createdAt", System.currentTimeMillis())
    )

    // ---- TireSet / TireSwap (v2) ----
    fun tireSetToJson(t: TireSet): JSONObject = JSONObject()
        .put("id", t.id)
        .put("carId", t.carId)
        .put("name", t.name)
        .put("season", t.season.name)
        .put("brand", t.brand)
        .put("size", t.size)
        .putNullable("year", t.year)
        .putNullable("notes", t.notes)
        .put("isArchived", t.isArchived)
        .put("createdAt", t.createdAt)

    fun tireSetFromJson(o: JSONObject): TireSet = TireSet(
        id = o.getLong("id"),
        carId = o.getLong("carId"),
        name = o.optString("name"),
        season = enumOr(o.optStringOrNull("season"), TireSeason.ALL_SEASON),
        brand = o.optString("brand"),
        size = o.optString("size"),
        year = o.optIntOrNull("year"),
        notes = o.optStringOrNull("notes"),
        isArchived = o.optBoolean("isArchived", false),
        createdAt = o.optLong("createdAt", System.currentTimeMillis())
    )

    fun tireSwapToJson(t: TireSwap): JSONObject = JSONObject()
        .put("id", t.id)
        .put("carId", t.carId)
        .put("tireSetId", t.tireSetId)
        .put("date", t.date)
        .put("mileage", t.mileage)
        .putNullable("notes", t.notes)

    fun tireSwapFromJson(o: JSONObject): TireSwap = TireSwap(
        id = o.getLong("id"),
        carId = o.getLong("carId"),
        tireSetId = o.getLong("tireSetId"),
        date = o.getLong("date"),
        mileage = o.optInt("mileage", 0),
        notes = o.optStringOrNull("notes")
    )
}
