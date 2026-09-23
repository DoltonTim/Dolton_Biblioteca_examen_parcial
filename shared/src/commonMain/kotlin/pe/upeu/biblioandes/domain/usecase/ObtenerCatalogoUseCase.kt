package pe.upeu.biblioandes.domain.usecase

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import pe.upeu.biblioandes.domain.model.Libro
import pe.upeu.biblioandes.domain.repository.BibliotecaRepository

class ObtenerCatalogoUseCase(
    private val repository: BibliotecaRepository
) {
    operator fun invoke(categoria: String? = null, busqueda: String? = null): Flow<List<Libro>> {
        return repository.getCatalogo().map { libros ->
            var resultado = libros

            // Filtro por categoría (RF-02)
            if (!categoria.isNullOrBlank() && categoria != "Todas") {
                resultado = resultado.filter {
                    it.categoria.equals(categoria, ignoreCase = true)
                }
            }

            // Filtro por búsqueda (RF-05: sin distinguir mayúsculas ni tildes)
            if (!busqueda.isNullOrBlank()) {
                val queryNormalizada = normalizar(busqueda)
                resultado = resultado.filter { libro ->
                    normalizar(libro.titulo).contains(queryNormalizada) ||
                            normalizar(libro.autor).contains(queryNormalizada)
                }
            }

            resultado
        }
    }

    private fun normalizar(texto: String): String {
        return texto.lowercase()
            .replace('á', 'a')
            .replace('é', 'e')
            .replace('í', 'i')
            .replace('ó', 'o')
            .replace('ú', 'u')
            .replace('ü', 'u')
            .trim()
    }
}
