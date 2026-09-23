package pe.upeu.biblioandes.domain.usecase

import kotlinx.coroutines.flow.first
import pe.upeu.biblioandes.domain.model.EstadoPrestamo
import pe.upeu.biblioandes.domain.model.Prestamo
import pe.upeu.biblioandes.domain.repository.BibliotecaRepository

sealed interface ResultadoPrestamo {
    data class Exito(val prestamo: Prestamo, val mensaje: String = "¡Préstamo registrado exitosamente!") : ResultadoPrestamo
    data class Error(val mensaje: String, val regla: String) : ResultadoPrestamo
}

class SolicitarPrestamoUseCase(
    private val repository: BibliotecaRepository
) {

    /**
     * Valida y ejecuta la solicitud de un préstamo aplicando estrictamente
     * las reglas de negocio del dominio (RN-01, RN-02, RN-03, RN-04).
     */
    suspend operator fun invoke(libroId: Int): ResultadoPrestamo {
        val libro = repository.getLibroPorId(libroId).first()
            ?: return ResultadoPrestamo.Error(
                mensaje = "El libro solicitado no fue encontrado en el catálogo.",
                regla = "ERROR_NOT_FOUND"
            )

        // =========================================================================
        // REGLA DE NEGOCIO RN-02:
        // No se puede solicitar un libro cuyo número de ejemplares disponibles sea cero.
        // =========================================================================
        if (libro.ejemplaresDisponibles <= 0) {
            return ResultadoPrestamo.Error(
                mensaje = "No es posible solicitar el préstamo: el libro no cuenta con ejemplares disponibles.",
                regla = "RN-02"
            )
        }

        val prestamosActuales = repository.getPrestamos().first()

        // =========================================================================
        // REGLA DE NEGOCIO RN-04:
        // Un estudiante con al menos un préstamo Vencido no puede solicitar un libro nuevo hasta regularizarlo.
        // =========================================================================
        val tienePrestamosVencidos = prestamosActuales.any { it.estado is EstadoPrestamo.Vencido }
        if (tienePrestamosVencidos) {
            return ResultadoPrestamo.Error(
                mensaje = "Solicitud denegada: tienes préstamos vencidos pendientes de devolución. Debes regularizarlos primero.",
                regla = "RN-04"
            )
        }

        // =========================================================================
        // REGLA DE NEGOCIO RN-01:
        // Un estudiante no puede tener más de tres préstamos en estado Activo de forma simultánea.
        // =========================================================================
        val totalPrestamosActivos = prestamosActuales.count { it.estado is EstadoPrestamo.Activo }
        if (totalPrestamosActivos >= 3) {
            return ResultadoPrestamo.Error(
                mensaje = "Límite alcanzado: no puedes tener más de tres préstamos activos simultáneamente.",
                regla = "RN-01"
            )
        }

        // =========================================================================
        // REGLA DE NEGOCIO RN-03:
        // Todo préstamo dura siete días (se fija EstadoPrestamo.Activo con 7 días restantes).
        // =========================================================================
        val nuevoPrestamo = repository.registrarPrestamo(libro)

        return ResultadoPrestamo.Exito(
            prestamo = nuevoPrestamo,
            mensaje = "Préstamo solicitado exitosamente por 7 días calendario."
        )
    }

    /**
     * Solicitud de Cambio SC-B:
     * Expone de forma reactiva si el estudiante alcanzó el límite de 3 préstamos
     * activos conforme a la regla RN-01 centralizada en Dominio.
     */


    fun limitePrestamosActivosAlcanzado(): kotlinx.coroutines.flow.Flow<Boolean> {
        return repository.getNumeroPrestamosActivos().let { flujo ->
            kotlinx.coroutines.flow.flow {
                repository.getPrestamos().collect { lista ->
                    val activos = lista.count { it.estado is pe.upeu.biblioandes.domain.model.EstadoPrestamo.Activo }
                    emit(activos >= 3)
                }
            }
        }
    }
}
