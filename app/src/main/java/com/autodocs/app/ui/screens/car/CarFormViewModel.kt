package com.autodocs.app.ui.screens.car

import android.app.Application
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.autodocs.app.data.PhotoStorage
import com.autodocs.app.data.entity.Car
import com.autodocs.app.data.entity.FuelType
import com.autodocs.app.data.entity.TransmissionType
import com.autodocs.app.data.repository.CarRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CarFormState(
    val carId: Long? = null,
    val name: String = "",
    val make: String = "",
    val model: String = "",
    val engine: String = "",
    val fuelType: FuelType = FuelType.UNKNOWN,
    val transmissionType: TransmissionType = TransmissionType.UNKNOWN,
    val licensePlate: String = "",
    val vin: String = "",
    val mileageText: String = "",
    val photoUri: String? = null,
    val isPhotoImporting: Boolean = false,
    val isLoading: Boolean = false,
    val isSaved: Boolean = false
) {
    /** Обов'язкові лише назва, марка й модель — решту можна дописати потім. */
    val isValid: Boolean
        get() = name.isNotBlank() && make.isNotBlank() && model.isNotBlank()
}

class CarFormViewModel(
    private val app: Application,
    private val repository: CarRepository
) : ViewModel() {

    private val _state = MutableStateFlow(CarFormState())
    val state: StateFlow<CarFormState> = _state.asStateFlow()

    /** Фото, яке було в авто до редагування (щоб прибрати старий файл після заміни). */
    private var originalPhotoUri: String? = null
    private var loadedCarId: Long? = null

    fun loadForEdit(carId: Long) {
        if (loadedCarId == carId) return
        loadedCarId = carId
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            val car = repository.getCar(carId)
            if (car != null) {
                originalPhotoUri = car.photoUri
                _state.value = CarFormState(
                    carId = car.id,
                    name = car.name,
                    make = car.make,
                    model = car.model,
                    engine = car.engine,
                    fuelType = car.fuelType,
                    transmissionType = car.transmissionType,
                    licensePlate = car.licensePlate,
                    vin = car.vin,
                    mileageText = if (car.mileage > 0) car.mileage.toString() else "",
                    photoUri = car.photoUri
                )
            } else {
                _state.update { it.copy(isLoading = false) }
            }
        }
    }

    fun onNameChange(value: String) = _state.update { it.copy(name = value) }
    fun onMakeChange(value: String) = _state.update { it.copy(make = value) }
    fun onModelChange(value: String) = _state.update { it.copy(model = value) }
    fun onEngineChange(value: String) = _state.update { it.copy(engine = value) }
    fun onFuelTypeChange(value: FuelType) = _state.update { it.copy(fuelType = value) }
    fun onTransmissionTypeChange(value: TransmissionType) = _state.update { it.copy(transmissionType = value) }
    fun onLicensePlateChange(value: String) = _state.update { it.copy(licensePlate = value.uppercase()) }
    fun onVinChange(value: String) = _state.update { it.copy(vin = value.uppercase().filter { c -> !c.isWhitespace() }) }
    fun onMileageChange(value: String) {
        if (value.length <= 7 && value.all { it.isDigit() }) _state.update { it.copy(mileageText = value) }
    }

    /** Копіюємо вибране фото у сховище застосунку (зменшене), а не зберігаємо посилання на галерею. */
    fun onPhotoPicked(uri: Uri) {
        viewModelScope.launch {
            _state.update { it.copy(isPhotoImporting = true) }
            val stored = PhotoStorage.importImage(app, uri, "car")
            val previous = _state.value.photoUri
            if (stored != null) {
                // попередньо вибране, але ще не збережене фото більше не потрібне
                if (previous != null && previous != originalPhotoUri) PhotoStorage.deleteIfOwned(app, previous)
                _state.update { it.copy(photoUri = stored, isPhotoImporting = false) }
            } else {
                _state.update { it.copy(isPhotoImporting = false) }
            }
        }
    }

    fun onPhotoRemoved() {
        val previous = _state.value.photoUri
        if (previous != null && previous != originalPhotoUri) PhotoStorage.deleteIfOwned(app, previous)
        _state.update { it.copy(photoUri = null) }
    }

    fun save() {
        val s = _state.value
        if (!s.isValid || s.isLoading) return
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            val mileage = s.mileageText.toIntOrNull() ?: 0
            if (s.carId == null) {
                repository.addCar(
                    Car(
                        name = s.name.trim(),
                        make = s.make.trim(),
                        model = s.model.trim(),
                        engine = s.engine.trim(),
                        fuelType = s.fuelType,
                        transmissionType = s.transmissionType,
                        licensePlate = s.licensePlate.trim(),
                        vin = s.vin.trim(),
                        photoUri = s.photoUri,
                        mileage = mileage
                    )
                )
            } else {
                val existing = repository.getCar(s.carId)
                if (existing != null) {
                    repository.updateCar(
                        existing.copy(
                            name = s.name.trim(),
                            make = s.make.trim(),
                            model = s.model.trim(),
                            engine = s.engine.trim(),
                            fuelType = s.fuelType,
                            transmissionType = s.transmissionType,
                            licensePlate = s.licensePlate.trim(),
                            vin = s.vin.trim(),
                            photoUri = s.photoUri
                        )
                    )
                    // Пробіг змінюємо через репозиторій — так він потрапляє і в історію пробігу.
                    if (mileage > 0 && mileage != existing.mileage) {
                        repository.updateMileage(existing.id, mileage)
                    }
                    if (originalPhotoUri != null && originalPhotoUri != s.photoUri) {
                        PhotoStorage.deleteIfOwned(app, originalPhotoUri)
                    }
                }
            }
            _state.update { it.copy(isLoading = false, isSaved = true) }
        }
    }
}

class CarFormViewModelFactory(
    private val app: Application,
    private val repository: CarRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = CarFormViewModel(app, repository) as T
}
