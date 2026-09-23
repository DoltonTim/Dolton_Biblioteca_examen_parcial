package pe.upeu.biblioandes.domain.model

data class Prestamo(
    val id: Int,
    val libro: Libro,
    val fechaPrestamo: String,
    val fechaLimite: String,
    val estado: EstadoPrestamo
)
