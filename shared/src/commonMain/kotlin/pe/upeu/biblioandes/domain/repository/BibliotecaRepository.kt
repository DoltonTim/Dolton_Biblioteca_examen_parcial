package pe.upeu.biblioandes.domain.repository

import kotlinx.coroutines.flow.Flow
import pe.upeu.biblioandes.domain.model.Estudiante
import pe.upeu.biblioandes.domain.model.Libro
import pe.upeu.biblioandes.domain.model.Prestamo

interface BibliotecaRepository {
    fun getEstudiante(): Flow<Estudiante>
    fun getCategorias(): Flow<List<String>>
    fun getCatalogo(): Flow<List<Libro>>
    fun getPrestamos(): Flow<List<Prestamo>>
    fun getLibroPorId(id: Int): Flow<Libro?>
    suspend fun registrarPrestamo(libro: Libro): Prestamo
    suspend fun devolverPrestamo(prestamoId: Int)
    fun setSimularError(simular: Boolean)
    fun isSimulandoError(): Boolean
}
