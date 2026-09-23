package pe.upeu.biblioandes

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import pe.upeu.biblioandes.data.repository.BibliotecaRepositoryFake
import pe.upeu.biblioandes.domain.model.EstadoPrestamo
import pe.upeu.biblioandes.domain.usecase.ObtenerCatalogoUseCase
import pe.upeu.biblioandes.domain.usecase.ObtenerPrestamosUseCase
import pe.upeu.biblioandes.domain.usecase.ResultadoPrestamo
import pe.upeu.biblioandes.domain.usecase.SolicitarPrestamoUseCase
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class BiblioAndesReglasNegocioTest {

    private lateinit var repository: BibliotecaRepositoryFake
    private lateinit var solicitarPrestamoUseCase: SolicitarPrestamoUseCase
    private lateinit var obtenerCatalogoUseCase: ObtenerCatalogoUseCase
    private lateinit var obtenerPrestamosUseCase: ObtenerPrestamosUseCase

    @BeforeTest
    fun setUp() {
        repository = BibliotecaRepositoryFake()
        solicitarPrestamoUseCase = SolicitarPrestamoUseCase(repository)
        obtenerCatalogoUseCase = ObtenerCatalogoUseCase(repository)
        obtenerPrestamosUseCase = ObtenerPrestamosUseCase(repository)
    }

    @Test
    fun testRN02_NoSePuedeSolicitarLibroConCeroEjemplares() = runTest {
        // Libro 2 ("Estructuras de datos") tiene 0 ejemplares disponibles en datos semilla
        val resultado = solicitarPrestamoUseCase(libroId = 2)

        assertIs<ResultadoPrestamo.Error>(resultado)
        assertEquals("RN-02", resultado.regla)
        assertTrue(resultado.mensaje.contains("no cuenta con ejemplares disponibles"))
    }

    @Test
    fun testRN04_EstudianteConPrestamoVencidoNoPuedeSolicitarLibro() = runTest {
        // En los datos semilla, el estudiante tiene el préstamo 5 en estado Vencido (18 días de atraso)
        // Intentar solicitar el Libro 1 (que sí tiene 3 ejemplares) debe ser bloqueado por morosidad
        val resultado = solicitarPrestamoUseCase(libroId = 1)

        assertIs<ResultadoPrestamo.Error>(resultado)
        assertEquals("RN-04", resultado.regla)
        assertTrue(resultado.mensaje.contains("tienes préstamos vencidos"))
    }

    @Test
    fun testRN01_NoMasDeTresPrestamosActivosSimultaneos() = runTest {
        // 1. Regularizamos el préstamo vencido (id 5) para que no aplique RN-04
        repository.devolverPrestamo(5)

        // En datos semilla hay 2 préstamos activos (id 1 y id 2)
        // 2. Solicitamos un 3er libro disponible (Libro 4 o 7)
        val resultado3 = solicitarPrestamoUseCase(libroId = 7)
        assertIs<ResultadoPrestamo.Exito>(resultado3)

        // 3. Con 3 préstamos activos, intentar solicitar un 4to libro debe ser rechazado por RN-01
        val resultado4 = solicitarPrestamoUseCase(libroId = 8)
        assertIs<ResultadoPrestamo.Error>(resultado4)
        assertEquals("RN-01", resultado4.regla)
        assertTrue(resultado4.mensaje.contains("límite", ignoreCase = true))
    }

    @Test
    fun testRN03_PrestamoDuraSieteDias() = runTest {
        // Regularizamos el préstamo vencido para poder solicitar
        repository.devolverPrestamo(5)

        val resultado = solicitarPrestamoUseCase(libroId = 7)
        assertIs<ResultadoPrestamo.Exito>(resultado)

        val prestamo = resultado.prestamo
        assertIs<EstadoPrestamo.Activo>(prestamo.estado)
        assertEquals(7, prestamo.estado.diasRestantes)
        assertEquals("2026-09-23", prestamo.fechaPrestamo)
        assertEquals("2026-09-30", prestamo.fechaLimite)
    }

    @Test
    fun testRF05_BusquedaSinDistinguirMayusculasNiTildes() = runTest {
        // Búsqueda con "calculo" sin tilde y en minúscula debe encontrar "Cálculo aplicado"
        val libros = obtenerCatalogoUseCase(busqueda = "calculo").first()
        assertTrue(libros.any { it.titulo == "Cálculo aplicado" })

        // Búsqueda con "GESTION" en mayúsculas sin tilde debe encontrar "Gestión de proyectos"
        val librosGestion = obtenerCatalogoUseCase(busqueda = "GESTION").first()
        assertTrue(librosGestion.any { it.titulo == "Gestión de proyectos" })
    }
}
