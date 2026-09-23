package pe.upeu.biblioandes.presentation.catalogo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import pe.upeu.biblioandes.domain.repository.BibliotecaRepository
import pe.upeu.biblioandes.domain.usecase.ObtenerCatalogoUseCase

class CatalogoViewModel(
    private val obtenerCatalogoUseCase: ObtenerCatalogoUseCase,
    private val repository: BibliotecaRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<CatalogoUiState>(CatalogoUiState.Loading)
    val uiState: StateFlow<CatalogoUiState> = _uiState.asStateFlow()

    private var categoriaActual: String = "Todas"
    private var busquedaActual: String = ""
    private var categoriasDisponibles: List<String> = emptyList()

    init {
        cargarCategoriasYCatalogo()
    }

    private fun cargarCategoriasYCatalogo() {
        viewModelScope.launch {
            try {
                categoriasDisponibles = listOf("Todas") + repository.getCategorias().first()
                cargarCatalogo()
            } catch (e: Exception) {
                _uiState.value = CatalogoUiState.Error(
                    mensaje = e.message ?: "Error al inicializar el catálogo."
                )
            }
        }
    }

    fun cargarCatalogo() {
        _uiState.value = CatalogoUiState.Loading
        viewModelScope.launch {
            obtenerCatalogoUseCase(
                categoria = if (categoriaActual == "Todas") null else categoriaActual,
                busqueda = busquedaActual
            )
                .catch { error ->
                    _uiState.value = CatalogoUiState.Error(
                        mensaje = error.message ?: "Ocurrió un error inesperado al cargar el catálogo."
                    )
                }
                .collect { libros ->
                    if (libros.isEmpty()) {
                        _uiState.value = CatalogoUiState.Empty(
                            mensaje = "No se encontraron libros para '${if (busquedaActual.isNotBlank()) busquedaActual else categoriaActual}'.",
                            categorias = categoriasDisponibles,
                            categoriaSeleccionada = categoriaActual,
                            busqueda = busquedaActual
                        )
                    } else {
                        _uiState.value = CatalogoUiState.Success(
                            libros = libros,
                            categorias = categoriasDisponibles,
                            categoriaSeleccionada = categoriaActual,
                            busqueda = busquedaActual,
                            simularError = repository.isSimulandoError()
                        )
                    }
                }
        }
    }

    fun seleccionarCategoria(categoria: String) {
        categoriaActual = categoria
        cargarCatalogo()
    }

    fun onBusquedaCambio(nuevaBusqueda: String) {
        busquedaActual = nuevaBusqueda
        cargarCatalogo()
    }

    fun toggleSimularError() {
        val nuevoEstado = !repository.isSimulandoError()
        repository.setSimularError(nuevoEstado)
        cargarCatalogo()
    }

    fun reintentar() {
        // Al reintentar, desactivamos la simulación si estaba activa para permitir la recuperación
        repository.setSimularError(false)
        cargarCatalogo()
    }
}
