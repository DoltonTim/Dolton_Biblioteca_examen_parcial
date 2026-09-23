package pe.upeu.biblioandes.presentation.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.ui.graphics.vector.ImageVector

sealed interface Destino {
    val ruta: String
    val titulo: String

    data object Inicio : Destino {
        override val ruta = "inicio"
        override val titulo = "Inicio"
    }

    data object Catalogo : Destino {
        override val ruta = "catalogo"
        override val titulo = "Catálogo"
    }

    data object Prestamos : Destino {
        override val ruta = "prestamos"
        override val titulo = "Mis Préstamos"
    }

    data object Perfil : Destino {
        override val ruta = "perfil"
        override val titulo = "Perfil"
    }

    data class Detalle(val libroId: Int) : Destino {
        override val ruta = "detalle/$libroId"
        override val titulo = "Detalle del Libro"
    }
}

data class BarraNavegacionItem(
    val destino: Destino,
    val titulo: String,
    val icono: ImageVector
)

val ITEMS_BARRA_INFERIOR = listOf(
    BarraNavegacionItem(Destino.Inicio, "Inicio", Icons.Default.Home),
    BarraNavegacionItem(Destino.Catalogo, "Catálogo", Icons.AutoMirrored.Filled.MenuBook),
    BarraNavegacionItem(Destino.Prestamos, "Préstamos", Icons.AutoMirrored.Filled.Assignment),
    BarraNavegacionItem(Destino.Perfil, "Perfil", Icons.Default.Person)
)
