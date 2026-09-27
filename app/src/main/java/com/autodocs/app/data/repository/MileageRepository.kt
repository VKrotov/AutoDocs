package com.autodocs.app.data.repository

import androidx.room.withTransaction
import com.autodocs.app.data.AppDatabase
import com.autodocs.app.data.entity.Car
import com.autodocs.app.data.stats.MileageHistory
import com.autodocs.app.data.stats.MileageHistoryPoint
import com.autodocs.app.data.stats.MileageSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset

/**
 * Етап 8: історія пробігу (для графіка) і виправлення помилкових відміток.
 * Точки збираються з трьох джерел: відмітки «Оновити пробіг», записи журналу, картка авто.
 */
class MileageRepository(private val db: AppDatabase) {
    private val mileageDao = db.mileageEntryDao()
    private val recordDao = db.serviceRecordDao()
    private val carDao = db.carDao()

    companion object {
        private fun timestampToLocalDate(millis: Long): LocalDate =
            Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()

        private fun utcToLocalDate(millis: Long): LocalDate =
            Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
    }

    fun observeHistory(car: Car): Flow<List<MileageHistoryPoint>> = combine(
        mileageDao.observeForCar(car.id),
        recordDao.observeForCar(car.id)
    ) { entries, records ->
        MileageHistory.build(
            entries = entries.map { MileageHistoryPoint(timestampToLocalDate(it.date), it.mileage, MileageSource.ENTRY, it.id, at = it.date) },
            records = records.map { MileageHistoryPoint(utcToLocalDate(it.date), it.mileage, MileageSource.RECORD, it.id, at = it.date) },
            car = if (car.mileage > 0) {
                MileageHistoryPoint(timestampToLocalDate(car.mileageUpdatedAt), car.mileage, MileageSource.CAR, car.id, at = car.mileageUpdatedAt)
            } else null
        )
    }

    /**
     * Видалити помилкову відмітку. Якщо поточний пробіг авто взявся саме з неї — повертаємо
     * авто до останньої узгодженої точки, що лишилась (відмітки або записи журналу).
     */
    suspend fun deleteEntry(entryId: Long) = db.withTransaction {
        val entry = mileageDao.getById(entryId) ?: return@withTransaction
        mileageDao.deleteById(entryId)
        val car = carDao.getById(entry.carId) ?: return@withTransaction
        if (car.mileage != entry.mileage) return@withTransaction

        // (точка, мітка часу для mileageUpdatedAt)
        val candidates = mileageDao.getForCar(car.id).map {
            MileageHistoryPoint(timestampToLocalDate(it.date), it.mileage, MileageSource.ENTRY, it.id, at = it.date) to it.date
        } + recordDao.getForCar(car.id).map {
            MileageHistoryPoint(utcToLocalDate(it.date), it.mileage, MileageSource.RECORD, it.id, at = it.date) to it.date
        }
        if (candidates.isEmpty()) return@withTransaction
        val stamps = candidates.associate { (p, t) -> (p.source to p.refId) to t }
        val latest = MileageHistory.markSuspicious(candidates.map { it.first })
            .lastOrNull { !it.suspicious && it.mileage > 0 } ?: return@withTransaction
        carDao.updateMileage(car.id, latest.mileage, stamps.getValue(latest.source to latest.refId))
    }
}
