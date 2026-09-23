package pe.upeu.biblioandes

import androidx.compose.runtime.Composable
import org.koin.compose.KoinContext
import pe.upeu.biblioandes.presentation.navigation.AppNavHost

@Composable
fun App() {
    KoinContext {
        AppNavHost()
    }
}
