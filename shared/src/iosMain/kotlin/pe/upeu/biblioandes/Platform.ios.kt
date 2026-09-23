package pe.upeu.biblioandes

import androidx.compose.runtime.Composable
import platform.UIKit.UIDevice

class IOSPlatform : Platform {
    override val name: String =
        UIDevice.currentDevice.systemName() + " " + UIDevice.currentDevice.systemVersion
}

actual fun getPlatform(): Platform = IOSPlatform()

@Composable
actual fun SystemBackHandler(enabled: Boolean, onBack: () -> Unit) {
    // En iOS la navegación de retorno se realiza por gestos o el botón en TopBar
}
