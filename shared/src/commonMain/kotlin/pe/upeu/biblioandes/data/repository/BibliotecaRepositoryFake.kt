package pe.upeu.biblioandes.data.repository

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import pe.upeu.biblioandes.data.local.DatosSimulados
import pe.upeu.biblioandes.domain.model.EstadoPrestamo
import pe.upeu.biblioandes.domain.model.Estudiante
import pe.upeu.biblioandes.domain.model.Libro
import pe.upeu.biblioandes.domain.model.Prestamo
import pe.upeu.biblioandes.domain.repository.BibliotecaRepository

class BibliotecaRepositoryFake : BibliotecaRepository {

    private val _estudiante = MutableStateFlow(DatosSimulados.estudiante)
    private val _categorias = MutableStateFlow(DatosSimulados.categorias)
    private val _libros = MutableStateFlow(DatosSimulados.libros)
    private val _prestamos = MutableStateFlow(DatosSimulados.prestamos)

    // Bandera para simular error en el catálogo según la sección 3.2 del PDF
    private var simularErrorCatalogo: Boolean = false

    override fun getEstudiante(): Flow<Estudiante> = flow {
        delay(800) // Simulación de carga asíncrona
        _estudiante.collect { emit(it) }
    }

    override fun getCategorias(): Flow<List<String>> = flow {
        _categorias.collect { emit(it) }
    }

    override fun getCatalogo(): Flow<List<Libro>> = flow {
        delay(800) // Simulación de carga requerida de 800ms
        if (simularErrorCatalogo) {
            throw IllegalStateException("Error simulado: No se pudo conectar con el catálogo de libros.")
        }
        _libros.collect { emit(it) }
    }

    override fun getPrestamos(): Flow<List<Prestamo>> = flow {
        delay(800) // Simulación de carga asíncrona
        _prestamos.collect { emit(it) }
    }

    override fun getLibroPorId(id: Int): Flow<Libro?> = flow {
        _libros.collect { lista ->
            emit(lista.find { it.id == id })
        }
    }

    override suspend fun registrarPrestamo(libro: Libro): Prestamo {
        delay(800)
        val nuevoId = (_prestamos.value.maxOfOrNull { it.id } ?: 0) + 1

        // Regla RN-03: Todo préstamo dura siete días
        val nuevoPrestamo = Prestamo(
            id = nuevoId,
            libro = libro,
            fechaPrestamo = "2026-09-23",
            fechaLimite = "2026-09-30",
            estado = EstadoPrestamo.Activo(diasRestantes = 7)
        )

        // Actualizamos ejemplares disponibles del libro
        _libros.value = _libros.value.map {
            if (it.id == libro.id) {
                it.copy(ejemplaresDisponibles = maxOf(0, it.ejemplaresDisponibles - 1))
            } else {
                it
            }
        }

        // Agregamos el préstamo a la lista
        _prestamos.value = listOf(nuevoPrestamo) + _prestamos.value

        return nuevoPrestamo
    }

    override suspend fun devolverPrestamo(prestamoId: Int) {
        delay(400)
        var libroADevolver: Libro? = null
        _prestamos.value = _prestamos.value.map { prestamo ->
            if (prestamo.id == prestamoId && prestamo.estado !is EstadoPrestamo.Devuelto) {
                libroADevolver = prestamo.libro
                prestamo.copy(estado = EstadoPrestamo.Devuelto("2026-09-23"))
            } else {
                prestamo
            }
        }

        libroADevolver?.let { libro ->
            _libros.value = _libros.value.map {
                if (it.id == libro.id) {
                    it.copy(ejemplaresDisponibles = it.ejemplaresDisponibles + 1)
                } else {
                    it
                }
            }
        }
    }

    override fun setSimularError(simular: Boolean) {
        simularErrorCatalogo = simular
    }

    override fun isSimulandoError(): Boolean = simularErrorCatalogo
}
