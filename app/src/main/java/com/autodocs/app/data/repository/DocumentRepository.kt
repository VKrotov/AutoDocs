package com.autodocs.app.data.repository

import androidx.room.withTransaction
import com.autodocs.app.data.AppDatabase
import com.autodocs.app.data.entity.CarDocument
import com.autodocs.app.data.entity.PhotoOwnerType
import com.autodocs.app.data.plan.DocumentDeadlines
import com.autodocs.app.data.plan.DocumentDue
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate

/**
 * Етап 9: документи з терміном дії (страховка, техогляд…). Фото поліса — у [PhotoRepository]
 * з ownerType = CAR_DOCUMENT (зовнішнього ключа немає, тож фото видаляємо тут разом із документом).
 */
class DocumentRepository(private val db: AppDatabase, private val photos: PhotoRepository) {
    private val dao = db.carDocumentDao()

    fun observeForCar(carId: Long): Flow<List<CarDocument>> = dao.observeForCar(carId)

    /** Документи зі станом на сьогодні (див. [DocumentDeadlines.evaluate]). */
    fun observeDue(carId: Long, warnDays: () -> Int, today: () -> LocalDate = { LocalDate.now() }): Flow<List<DocumentDue>> =
        dao.observeForCar(carId).map { DocumentDeadlines.evaluate(it, today(), warnDays()) }

    suspend fun getForCar(carId: Long): List<CarDocument> = dao.getForCar(carId)

    fun observeCompanies(): Flow<List<String>> = dao.observeCompanies()

    suspend fun get(id: Long): CarDocument? = dao.getById(id)

    /** Зберегти документ і привести його фото до [photoRefs]. Повертає id. */
    suspend fun save(doc: CarDocument, photoRefs: List<PhotoRef>): Long {
        val id = db.withTransaction {
            if (doc.id == 0L) dao.insert(doc) else { dao.update(doc); doc.id }
        }
        photos.replaceFor(PhotoOwnerType.CAR_DOCUMENT, id, photoRefs)
        return id
    }

    suspend fun delete(id: Long) {
        dao.deleteById(id)
        photos.deleteAllFor(PhotoOwnerType.CAR_DOCUMENT, id)
    }
}
