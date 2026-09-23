package pe.upeu.biblioandes.presentation.prestamos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import pe.upeu.biblioandes.domain.model.Prestamo
import pe.upeu.biblioandes.domain.repository.BibliotecaRepository
import pe.upeu.biblioandes.domain.usecase.ObtenerPrestamosUseCase

sealed interface PrestamosUiState {
    data object Loading : PrestamosUiState
    data class Success(
        val prestamos: List<Prestamo>,
        val filtroSeleccionado: String = "Todos"
    ) : PrestamosUiState
    data class Empty(
        val filtroSeleccionado: String = "Todos"
    ) : PrestamosUiState
    data class Error(val mensaje: String) : PrestamosUiState
}

class PrestamosViewModel(
    private val obtenerPrestamosUseCase: ObtenerPrestamosUseCase,
    private val repository: BibliotecaRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<PrestamosUiState>(PrestamosUiState.Loading)
    val uiState: StateFlow<PrestamosUiState> = _uiState.asStateFlow()

    private var filtroActual: String = "Todos"

    init {
        cargarPrestamos()
    }

    fun cargarPrestamos() {
        _uiState.value = PrestamosUiState.Loading
        viewModelScope.launch {
            obtenerPrestamosUseCase(filtroActual)
                .catch { error ->
                    _uiState.value = PrestamosUiState.Error(
                        error.message ?: "Error al obtener la lista de préstamos."
                    )
                }
                .collect { lista ->
                    if (lista.isEmpty()) {
                        _uiState.value = PrestamosUiState.Empty(filtroSeleccionado = filtroActual)
                    } else {
                        _uiState.value = PrestamosUiState.Success(
                            prestamos = lista,
                            filtroSeleccionado = filtroActual
                        )
                    }
                }
        }
    }

    fun filtrarPorEstado(estado: String) {
        filtroActual = estado
        cargarPrestamos()
    }

    fun devolverPrestamo(prestamoId: Int) {
        viewModelScope.launch {
            repository.devolverPrestamo(prestamoId)
            cargarPrestamos()
        }
    }
}
