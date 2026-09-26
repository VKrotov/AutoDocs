package com.autodocs.app.data.repository

import androidx.room.withTransaction
import com.autodocs.app.data.AppDatabase
import com.autodocs.app.data.entity.MileageEntry
import com.autodocs.app.data.entity.RecordWithItems
import com.autodocs.app.data.entity.ServiceRecord
import com.autodocs.app.data.entity.ServiceRecordItem
import com.autodocs.app.data.entity.WorkItemCategory
import com.autodocs.app.data.entity.WorkType
import kotlinx.coroutines.flow.Flow

/** Позиція запису до збереження: назва + категорія + ціна (ідентифікатор довідника — опційно). */
data class ItemInput(
    val category: WorkItemCategory,
    val name: String,
    val price: Double,
    val workTypeId: Long? = null
)

/**
 * Журнал (F03/F04) і довідник робіт (F06).
 * Назву позиції завжди зберігаємо в [ServiceRecordItem.customName] — навіть якщо
 * вона вибрана з довідника. Так запис читається без join-ів і не "ламається",
 * якщо пункт довідника потім перейменують чи видалять.
 */
class ServiceRepository(private val db: AppDatabase) {
    private val recordDao = db.serviceRecordDao()
    private val itemDao = db.serviceRecordItemDao()
    private val workTypeDao = db.workTypeDao()
    private val carDao = db.carDao()
    private val mileageDao = db.mileageEntryDao()

    // ---- Журнал ----

    fun observeRecords(carId: Long): Flow<List<RecordWithItems>> = recordDao.observeWithItemsForCar(carId)

    fun observeRecord(id: Long): Flow<RecordWithItems?> = recordDao.observeWithItems(id)

    suspend fun getRecord(id: Long): RecordWithItems? = recordDao.getWithItems(id)

    fun observeStoNames(): Flow<List<String>> = recordDao.observeStoNames()

    /**
     * Створює (recordId == null) або оновлює запис разом із позиціями — однією транзакцією.
     * Нові назви, яких ще немає в довіднику, автоматично додаються туди як власні.
     * Якщо пробіг у записі більший за поточний пробіг авто — оновлюємо і пробіг авто.
     */
    suspend fun saveRecord(
        recordId: Long?,
        carId: Long,
        date: Long,
        mileage: Int,
        stoName: String?,
        notes: String?,
        items: List<ItemInput>
    ): Long = db.withTransaction {
        val id = if (recordId == null) {
            recordDao.insert(
                ServiceRecord(carId = carId, date = date, mileage = mileage, stoName = stoName, notes = notes)
            )
        } else {
            val existing = recordDao.getById(recordId)
            if (existing != null) {
                recordDao.update(existing.copy(date = date, mileage = mileage, stoName = stoName, notes = notes))
            }
            itemDao.deleteForRecord(recordId)
            recordId
        }

        val resolved = items.map { input ->
            val name = input.name.trim()
            val typeId = input.workTypeId
                ?: workTypeDao.findByName(input.category, name)?.id
                ?: workTypeDao.insert(WorkType(name = name, category = input.category, isCustom = true))
                    .takeIf { it > 0 }
            ServiceRecordItem(
                recordId = id,
                workTypeId = typeId,
                customName = name,
                category = input.category,
                price = input.price
            )
        }
        itemDao.insertAll(resolved)

        val car = carDao.getById(carId)
        if (car != null && mileage > car.mileage) {
            val now = System.currentTimeMillis()
            carDao.updateMileage(carId, mileage, now)
            mileageDao.insert(MileageEntry(carId = carId, mileage = mileage, date = now))
        }
        id
    }

    suspend fun deleteRecord(id: Long) = recordDao.deleteById(id)

    // ---- Довідник ----

    fun observeWorkTypes(): Flow<List<WorkType>> = workTypeDao.observeAll()

    /** Повертає false, якщо такий пункт у цій категорії вже є. */
    suspend fun addWorkType(name: String, category: WorkItemCategory): Boolean {
        val trimmed = name.trim()
        if (trimmed.isEmpty() || workTypeDao.findByName(category, trimmed) != null) return false
        workTypeDao.insert(WorkType(name = trimmed, category = category, isCustom = true))
        return true
    }

    suspend fun renameWorkType(workType: WorkType, newName: String): Boolean {
        val trimmed = newName.trim()
        if (trimmed.isEmpty()) return false
        val clash = workTypeDao.findByName(workType.category, trimmed)
        if (clash != null && clash.id != workType.id) return false
        workTypeDao.update(workType.copy(name = trimmed))
        return true
    }

    suspend fun deleteWorkType(workType: WorkType) = workTypeDao.delete(workType)

    suspend fun seedWorkTypesIfNeeded(alreadySeeded: Boolean): Boolean {
        if (alreadySeeded) return false
        val existing = workTypeDao.getAll().map { it.category to it.name.lowercase() }.toSet()
        val missing = com.autodocs.app.data.WorkTypeSeed.all.filter { (it.category to it.name.lowercase()) !in existing }
        workTypeDao.insertAll(missing)
        return true
    }
}
