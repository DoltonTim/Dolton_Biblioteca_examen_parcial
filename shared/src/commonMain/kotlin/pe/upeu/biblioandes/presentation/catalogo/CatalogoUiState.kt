package pe.upeu.biblioandes.presentation.catalogo

import pe.upeu.biblioandes.domain.model.Libro

sealed interface CatalogoUiState {
    data object Loading : CatalogoUiState

    data class Success(
        val libros: List<Libro>,
        val categorias: List<String>,
        val categoriaSeleccionada: String = "Todas",
        val busqueda: String = "",
        val simularError: Boolean = false
    ) : CatalogoUiState

    data class Empty(
        val mensaje: String = "No se encontraron libros disponibles con los filtros aplicados.",
        val categorias: List<String> = emptyList(),
        val categoriaSeleccionada: String = "Todas",
        val busqueda: String = ""
    ) : CatalogoUiState

    data class Error(
        val mensaje: String
    ) : CatalogoUiState
}
