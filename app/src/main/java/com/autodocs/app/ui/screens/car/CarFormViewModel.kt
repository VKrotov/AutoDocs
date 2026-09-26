package com.autodocs.app.ui.screens.car

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.autodocs.app.data.entity.Car
import com.autodocs.app.data.entity.FuelType
import com.autodocs.app.data.entity.TransmissionType
import com.autodocs.app.data.repository.CarRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class CarFormState(
    val carId: Long? = null,
    val name: String = "",
    val make: String = "",
    val model: String = "",
    val engine: String = "",
    val fuelType: FuelType = FuelType.PETROL,
    val transmissionType: TransmissionType = TransmissionType.MANUAL,
    val licensePlate: String = "",
    val vin: String = "",
    val mileageText: String = "",
    val photoUri: String? = null,
    val isLoading: Boolean = false,
    val isSaved: Boolean = false
) {
    val isValid: Boolean
        get() = name.isNotBlank() && make.isNotBlank() && model.isNotBlank() &&
            licensePlate.isNotBlank() && vin.isNotBlank() && mileageText.toIntOrNull() != null
}

class CarFormViewModel(private val repository: CarRepository) : ViewModel() {

    private val _state = MutableStateFlow(CarFormState())
    val state: StateFlow<CarFormState> = _state.asStateFlow()

    fun loadForEdit(carId: Long) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            val car = repository.getCar(carId)
            if (car != null) {
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
                    mileageText = car.mileage.toString(),
                    photoUri = car.photoUri
                )
            } else {
                _state.value = _state.value.copy(isLoading = false)
            }
        }
    }

    fun onNameChange(value: String) { _state.value = _state.value.copy(name = value) }
    fun onMakeChange(value: String) { _state.value = _state.value.copy(make = value) }
    fun onModelChange(value: String) { _state.value = _state.value.copy(model = value) }
    fun onEngineChange(value: String) { _state.value = _state.value.copy(engine = value) }
    fun onFuelTypeChange(value: FuelType) { _state.value = _state.value.copy(fuelType = value) }
    fun onTransmissionTypeChange(value: TransmissionType) { _state.value = _state.value.copy(transmissionType = value) }
    fun onLicensePlateChange(value: String) { _state.value = _state.value.copy(licensePlate = value.uppercase()) }
    fun onVinChange(value: String) { _state.value = _state.value.copy(vin = value.uppercase()) }
    fun onMileageChange(value: String) {
        if (value.all { it.isDigit() }) _state.value = _state.value.copy(mileageText = value)
    }
    fun onPhotoPicked(uri: String?) { _state.value = _state.value.copy(photoUri = uri) }

    fun save() {
        val s = _state.value
        if (!s.isValid) return
        viewModelScope.launch {
            _state.value = s.copy(isLoading = true)
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
                }
            }
            _state.value = _state.value.copy(isLoading = false, isSaved = true)
        }
    }
}

class CarFormViewModelFactory(private val repository: CarRepository) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = CarFormViewModel(repository) as T
}
