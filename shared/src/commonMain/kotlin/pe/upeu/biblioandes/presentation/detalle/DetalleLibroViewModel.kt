package pe.upeu.biblioandes.presentation.detalle

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import pe.upeu.biblioandes.domain.model.Libro
import pe.upeu.biblioandes.domain.repository.BibliotecaRepository
import pe.upeu.biblioandes.domain.usecase.ResultadoPrestamo
import pe.upeu.biblioandes.domain.usecase.SolicitarPrestamoUseCase

sealed interface DetalleUiState {
    data object Loading : DetalleUiState
    data class Content(
        val libro: Libro,
        val solicitando: Boolean = false,
        val mensajeExito: String? = null,
        val mensajeError: String? = null
    ) : DetalleUiState
    data class Error(val mensaje: String) : DetalleUiState
}

class DetalleLibroViewModel(
    private val repository: BibliotecaRepository,
    private val solicitarPrestamoUseCase: SolicitarPrestamoUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<DetalleUiState>(DetalleUiState.Loading)
    val uiState: StateFlow<DetalleUiState> = _uiState.asStateFlow()

    private var libroIdActual: Int = 0

    fun cargarLibro(id: Int) {
        libroIdActual = id
        _uiState.value = DetalleUiState.Loading
        viewModelScope.launch {
            repository.getLibroPorId(id).collect { libro ->
                if (libro != null) {
                    val current = _uiState.value
                    if (current is DetalleUiState.Content) {
                        _uiState.value = current.copy(libro = libro)
                    } else {
                        _uiState.value = DetalleUiState.Content(libro = libro)
                    }
                } else {
                    _uiState.value = DetalleUiState.Error("Libro no encontrado en el sistema.")
                }
            }
        }
    }

    fun confirmarSolicitudPrestamo() {
        val currentState = _uiState.value as? DetalleUiState.Content ?: return
        _uiState.value = currentState.copy(solicitando = true, mensajeExito = null, mensajeError = null)

        viewModelScope.launch {
            val resultado = solicitarPrestamoUseCase(libroIdActual)
            val updatedState = _uiState.value as? DetalleUiState.Content ?: return@launch

            when (resultado) {
                is ResultadoPrestamo.Exito -> {
                    // Refrescar el libro desde el repositorio
                    val libroActualizado = repository.getLibroPorId(libroIdActual).first() ?: updatedState.libro
                    _uiState.value = updatedState.copy(
                        libro = libroActualizado,
                        solicitando = false,
                        mensajeExito = "${resultado.mensaje} Devolución requerida antes del 30/09/2026."
                    )
                }
                is ResultadoPrestamo.Error -> {
                    _uiState.value = updatedState.copy(
                        solicitando = false,
                        mensajeError = "[${resultado.regla}] ${resultado.mensaje}"
                    )
                }
            }
        }
    }

    fun limpiarMensajes() {
        val current = _uiState.value as? DetalleUiState.Content ?: return
        _uiState.value = current.copy(mensajeExito = null, mensajeError = null)
    }
}
