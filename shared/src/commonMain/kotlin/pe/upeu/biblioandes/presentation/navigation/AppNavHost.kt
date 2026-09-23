package pe.upeu.biblioandes.presentation.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.unit.dp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import pe.upeu.biblioandes.SystemBackHandler
import pe.upeu.biblioandes.domain.repository.BibliotecaRepository
import pe.upeu.biblioandes.domain.usecase.ObtenerPrestamosUseCase
import pe.upeu.biblioandes.presentation.catalogo.CatalogoScreen
import pe.upeu.biblioandes.presentation.catalogo.CatalogoViewModel
import pe.upeu.biblioandes.presentation.detalle.DetalleLibroScreen
import pe.upeu.biblioandes.presentation.detalle.DetalleLibroViewModel
import pe.upeu.biblioandes.presentation.inicio.InicioScreen
import pe.upeu.biblioandes.presentation.perfil.PerfilScreen
import pe.upeu.biblioandes.presentation.prestamos.PrestamosScreen
import pe.upeu.biblioandes.presentation.prestamos.PrestamosViewModel
import pe.upeu.biblioandes.presentation.theme.BiblioAndesTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavHost() {
    var destinoActual by remember { mutableStateOf<Destino>(Destino.Inicio) }
    var pilaNavegacion by remember { mutableStateOf(listOf<Destino>(Destino.Inicio)) }
    var darkTheme by remember { mutableStateOf(false) }

    val repository: BibliotecaRepository = koinInject()
    val obtenerPrestamosUseCase: ObtenerPrestamosUseCase = koinInject()

    // Manejo del botón atrás del sistema (RF-07)
    SystemBackHandler(enabled = pilaNavegacion.size > 1) {
        if (pilaNavegacion.size > 1) {
            val nuevaPila = pilaNavegacion.dropLast(1)
            pilaNavegacion = nuevaPila
            destinoActual = nuevaPila.last()
        }
    }

    fun navegarA(nuevoDestino: Destino) {
        if (nuevoDestino is Destino.Detalle) {
            pilaNavegacion = pilaNavegacion + nuevoDestino
            destinoActual = nuevoDestino
        } else {
            // Al seleccionar una pestaña de la barra inferior, reiniciamos la pila a ese destino
            pilaNavegacion = listOf(nuevoDestino)
            destinoActual = nuevoDestino
        }
    }

    fun volverAtras() {
        if (pilaNavegacion.size > 1) {
            val nuevaPila = pilaNavegacion.dropLast(1)
            pilaNavegacion = nuevaPila
            destinoActual = nuevaPila.last()
        } else {
            pilaNavegacion = listOf(Destino.Inicio)
            destinoActual = Destino.Inicio
        }
    }

    BiblioAndesTheme(darkTheme = darkTheme) {
        // En la pantalla de detalle ocultamos la barra inferior para una vista inmersiva
        val esDetalle = destinoActual is Destino.Detalle

        Scaffold(
            topBar = {
                if (!esDetalle) {
                    TopAppBar(
                        title = {
                            Text(
                                text = "BiblioAndes • ${destinoActual.titulo}",
                                style = MaterialTheme.typography.titleLarge
                            )
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                            titleContentColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }
            },
            bottomBar = {
                if (!esDetalle) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        ITEMS_BARRA_INFERIOR.forEach { item ->
                            val seleccionado = destinoActual == item.destino
                            NavigationBarItem(
                                selected = seleccionado,
                                onClick = { navegarA(item.destino) },
                                icon = {
                                    Icon(
                                        imageVector = item.icono,
                                        contentDescription = item.titulo
                                    )
                                },
                                label = { Text(item.titulo) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.primary,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    indicatorColor = MaterialTheme.colorScheme.primaryContainer
                                )
                            )
                        }
                    }
                }
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(if (esDetalle) androidx.compose.foundation.layout.PaddingValues(0.dp) else paddingValues)
            ) {
                when (val destino = destinoActual) {
                    is Destino.Inicio -> {
                        InicioScreen(
                            repository = repository,
                            obtenerPrestamosUseCase = obtenerPrestamosUseCase,
                            onNavegar = { navegarA(it) },
                            onVerDetalleLibro = { libroId -> navegarA(Destino.Detalle(libroId)) }
                        )
                    }

                    is Destino.Catalogo -> {
                        val catalogoViewModel = koinViewModel<CatalogoViewModel>()
                        CatalogoScreen(
                            viewModel = catalogoViewModel,
                            onLibroClick = { libroId -> navegarA(Destino.Detalle(libroId)) }
                        )
                    }

                    is Destino.Prestamos -> {
                        val prestamosViewModel = koinViewModel<PrestamosViewModel>()
                        PrestamosScreen(viewModel = prestamosViewModel)
                    }

                    is Destino.Perfil -> {
                        PerfilScreen(
                            repository = repository,
                            darkTheme = darkTheme,
                            onDarkThemeChange = { darkTheme = it }
                        )
                    }

                    is Destino.Detalle -> {
                        val detalleViewModel = koinViewModel<DetalleLibroViewModel>()
                        DetalleLibroScreen(
                            libroId = destino.libroId,
                            viewModel = detalleViewModel,
                            onVolver = { volverAtras() }
                        )
                    }
                }
            }
        }
    }
}
