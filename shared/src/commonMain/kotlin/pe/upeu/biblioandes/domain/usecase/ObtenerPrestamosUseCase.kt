package pe.upeu.biblioandes.domain.usecase

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import pe.upeu.biblioandes.domain.model.EstadoPrestamo
import pe.upeu.biblioandes.domain.model.Prestamo
import pe.upeu.biblioandes.domain.repository.BibliotecaRepository

class ObtenerPrestamosUseCase(
    private val repository: BibliotecaRepository
) {
    /**
     * Retorna los préstamos ordenados por fecha de devolución más próxima (RF-04),
     * con opción de filtrado por estado (Activo, Devuelto, Vencido).
     */
    operator fun invoke(filtroEstado: String? = null): Flow<List<Prestamo>> {
        return repository.getPrestamos().map { lista ->
            // Ordenar por fecha de devolución más próxima:
            // Activos primero con menos días restantes, luego vencidos, luego devueltos
            val ordenados = lista.sortedWith { p1, p2 ->
                when {
                    p1.estado is EstadoPrestamo.Activo && p2.estado is EstadoPrestamo.Activo -> {
                        p1.estado.diasRestantes.compareTo(p2.estado.diasRestantes)
                    }
                    p1.estado is EstadoPrestamo.Activo -> -1
                    p2.estado is EstadoPrestamo.Activo -> 1
                    p1.estado is EstadoPrestamo.Vencido && p2.estado is EstadoPrestamo.Vencido -> {
                        p2.estado.diasDeAtraso.compareTo(p1.estado.diasDeAtraso)
                    }
                    p1.estado is EstadoPrestamo.Vencido -> -1
                    p2.estado is EstadoPrestamo.Vencido -> 1
                    else -> p1.fechaLimite.compareTo(p2.fechaLimite)
                }
            }

            if (filtroEstado.isNullOrBlank() || filtroEstado.equals("Todos", ignoreCase = true)) {
                ordenados
            } else {
                ordenados.filter { prestamo ->
                    when (filtroEstado.lowercase()) {
                        "activo" -> prestamo.estado is EstadoPrestamo.Activo
                        "devuelto" -> prestamo.estado is EstadoPrestamo.Devuelto
                        "vencido" -> prestamo.estado is EstadoPrestamo.Vencido
                        else -> true
                    }
                }
            }
        }
    }

    /**
     * RF-01: Retorna el préstamo activo cuya devolución vence primero (menor número de días restantes).
     */
    fun obtenerPrestamoQueVencePrimero(): Flow<Prestamo?> {
        return repository.getPrestamos().map { lista ->
            lista.filter { it.estado is EstadoPrestamo.Activo }
                .minByOrNull { (it.estado as EstadoPrestamo.Activo).diasRestantes }
        }
    }
}
