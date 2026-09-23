package pe.upeu.biblioandes

import androidx.compose.runtime.Composable

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform

@Composable
expect fun SystemBackHandler(enabled: Boolean = true, onBack: () -> Unit)
