# BiblioAndes — Sistema de Préstamos de Biblioteca (KMP)

[![Kotlin](https://img.shields.io/badge/Kotlin-2.4.10-blue.svg?logo=kotlin)](https://kotlinlang.org)
[![Compose Multiplatform](https://img.shields.io/badge/Compose_Multiplatform-1.11.1-purple.svg)](https://www.jetbrains.com/lp/compose-multiplatform/)
[![Material 3](https://img.shields.io/badge/Material_3-1.11.0-green.svg)](https://m3.material.io/)
[![Koin](https://img.shields.io/badge/Koin-4.0.0-orange.svg)](https://insert-koin.io/)

Producto de la Unidad 1 del curso **Desarrollo de Aplicaciones Móviles** (Facultad de Ingeniería y Arquitectura, EP Ingeniería de Sistemas - UPeU).  
Aplicación Kotlin Multiplatform (KMP) ejecutándose en **Android** e **iOS** desde el mismo módulo compartido con navegación completa, theming Material 3 y arquitectura Clean + MVVM con datos simulados en memoria.

---

## 1. Presentación del Caso: BiblioAndes

BiblioAndes es la biblioteca de un instituto tecnológico con tres sedes (Central, Sede Norte, Sede Sur) y aproximadamente 1,800 estudiantes. El personal llevaba el control de préstamos en cuadernos físicos y el catálogo se consultaba únicamente en terminales de mostrador, ocasionando demoras y devoluciones tardías.

### Restricción Técnica del Proyecto
El servicio web que expondrá la API de catálogo se construirá en la segunda unidad del curso. Conforme a las especificaciones:
- **Prohibición explícita de librerías de red/persistencia**: No se utilizan Ktor, SQLDelight, Room ni Retrofit.
- Todos los datos provienen de una fuente simulada en memoria (`DatosSimulados.kt` y `BibliotecaRepositoryFake.kt`) con un retardo asíncrono simulado de 800 ms mediante corrutinas.
- La arquitectura Clean + MVVM garantiza que, una vez que la API real esté lista, bastará con sustituir la implementación del repositorio en la capa `data` sin tocar la interfaz ni los casos de uso.

---

## 2. Requerimientos Funcionales Implementados

| Código | Requerimiento | Implementación |
| :--- | :--- | :--- |
| **RF-01** | **Pantalla de inicio** | Saludo personalizado al estudiante ("Dolton Tom Meza Arevalo"), tarjeta destacada con el préstamo cuya devolución vence primero y accesos rápidos a Catálogo y Préstamos. |
| **RF-02** | **Catálogo de libros** | Listado de 12 libros repartidos en 5 categorías (Programación, Matemática, Redes, Gestión, Literatura), con título, autor, año, sede y badge de ejemplares disponibles. Filtrable por chips de categoría. |
| **RF-03** | **Detalle del libro** | Vista con información completa, acción «Solicitar préstamo» y diálogo de confirmación obligatorio antes del registro. |
| **RF-04** | **Mis préstamos** | Lista de préstamos ordenada cronológicamente por vencimiento más próximo, filtrable por chips (`Todos`, `Activo`, `Devuelto`, `Vencido`). |
| **RF-05** | **Búsqueda** | Campo de búsqueda en catálogo que filtra por título o autor, insensible a mayúsculas y tildes (`normalizarTexto`). |
| **RF-06** | **Perfil y ajustes** | Datos del estudiante, conmutador de tema claro/oscuro de efecto inmediato en toda la app y switch de simulación de error de red. |
| **RF-07** | **Navegación** | Scaffold con `NavigationBar` inferior (Inicio, Catálogo, Préstamos, Perfil), navegación al detalle del libro y soporte de retorno con botón atrás del sistema. |

---

## 3. Reglas de Negocio en la Capa Domain

Las cuatro reglas de negocio están implementadas estrictamente en la capa de **Dominio** (`SolicitarPrestamoUseCase.kt` y `BibliotecaRepositoryFake.kt`), no en los composables de UI:

* **RN-01**: *Un estudiante no puede tener más de tres préstamos en estado Activo de forma simultánea.*
* **RN-02**: *No se puede solicitar un libro cuyo número de ejemplares disponibles sea cero.*
* **RN-03**: *Todo préstamo dura siete días; si la fecha de devolución ya pasó, el préstamo se muestra como Vencido.*
* **RN-04**: *Un estudiante con al menos un préstamo Vencido no puede solicitar un libro nuevo hasta regularizarlo.*

---

## 4. Estructura de Paquetes (`commonMain`)

Siguiendo la especificación de la Sección 4.1 del examen:

```
shared/src/commonMain/kotlin/pe/upeu/biblioandes/
├── domain/
│   ├── model/
│   │   ├── Libro.kt
│   │   ├── Prestamo.kt
│   │   ├── EstadoPrestamo.kt
│   │   └── Estudiante.kt
│   ├── repository/
│   │   └── BibliotecaRepository.kt          # Interfaz pura del repositorio
│   └── usecase/
│       ├── ObtenerCatalogoUseCase.kt        # Filtros por categoría y búsqueda normalizada
│       ├── ObtenerPrestamosUseCase.kt       # Ordenamiento y préstamo urgente
│       └── SolicitarPrestamoUseCase.kt      # RN-01, RN-02, RN-03, RN-04
├── data/
│   ├── local/
│   │   └── DatosSimulados.kt                # 12 libros, 5 categorías, 5 préstamos semilla
│   └── repository/
│       └── BibliotecaRepositoryFake.kt      # Implementación en memoria con delay(800ms)
├── presentation/
│   ├── catalogo/
│   │   ├── CatalogoViewModel.kt
│   │   ├── CatalogoUiState.kt               # Loading, Success, Empty, Error
│   │   └── CatalogoScreen.kt
│   ├── detalle/
│   │   ├── DetalleLibroViewModel.kt
│   │   └── DetalleLibroScreen.kt
│   ├── prestamos/
│   │   ├── PrestamosViewModel.kt
│   │   └── PrestamosScreen.kt
│   ├── inicio/
│   │   └── InicioScreen.kt
│   ├── perfil/
│   │   └── PerfilScreen.kt
│   ├── navigation/
│   │   ├── AppNavHost.kt
│   │   └── Destinos.kt
│   └── theme/
│       ├── Color.kt                         # Paleta propia Material 3
│       ├── Type.kt                          # Tipografía M3
│       └── BiblioAndesTheme.kt              # Soporte claro y oscuro
├── di/
│   └── AppModule.kt                         # Inyección de dependencias Koin
├── App.kt                                   # Composable raíz compartido
└── Platform.kt                              # Expect/Actual para Android e iOS
```

---

## 5. Decisiones de Arquitectura y Justificación Técnica

### 5.1 ¿Por qué `EstadoPrestamo` es una `sealed class`?
Cada estado modela información distinta:
- `Activo`: lleva `val diasRestantes: Int`.
- `Devuelto`: lleva `val fechaDevolucion: String`.
- `Vencido`: lleva `val diasDeAtraso: Int`.

Un `enum` o un `String` obligarían a arrastrar propiedades vacías o nulas. Con una `sealed class`, el compilador de Kotlin verifica exhaustividad en expresiones `when`, garantizando seguridad de tipos y código libre de errores de casteo en tiempo de ejecución.

### 5.2 Estados de Interfaz
Toda pantalla que carga datos maneja tres estados distinguibles:
1. **Carga (`Loading`)**: Indicador de progreso circular con retardo simulado de 800 ms.
2. **Contenido (`Success`)**: Listados optimizados con `LazyColumn` y elevación de estado (*state hoisting*).
3. **Lista vacía (`Empty`)**: Ilustración y mensaje explicativo cuando no hay coincidencias.
4. **Error (`Error`)**: Simulado en el catálogo activando la bandera desde el perfil o el botón de prueba, con botón funcional para reintentar la conexión.

---

## 6. Instrucciones de Compilación y Ejecución

### Prerrequisitos
- JDK 17 o 21 configurado (`JAVA_HOME`).
- Android Studio Ladybug / Koala o superior.
- Android SDK 36 instalado (minSdk: 24, targetSdk: 36).
- Para iOS: macOS con Xcode 15+ y CocoaPods / Swift Package Manager (opcional si se ejecuta desde Mac).

### Ejecutar en Android desde Consola
```bash
# Compilar y empaquetar APK Debug
./gradlew :androidApp:assembleDebug

# Instalar y ejecutar en emulador o dispositivo conectado
./gradlew :androidApp:installDebug
```

### Ejecutar Pruebas Unitarias Automatizadas
```bash
# Ejecuta la suite de verificación de reglas de negocio
./gradlew :shared:testAndroidHostTest
```

---

## 7. Trabajo Colaborativo con Git

- **Repositorio**: `https://github.com/DoltonTim/Dolton_Biblioteca_examen_parcial.git`
- **Etiqueta evaluada**: `v1.0-unidad1`
- **Convención de commits**: `feat(...)`, `fix(...)`, `refactor(...)`, `style(...)`, `docs(...)`.
- **Ramas requeridas**:
  - `main`: Código estable.
  - `develop`: Rama base de integración.
  - `feature/catalogo-meza`: Requerimientos de catálogo y búsqueda.
  - `feature/prestamos-meza`: Préstamos y reglas de dominio.
  - `feature/inicio-perfil-meza`: Pantalla de inicio, perfil y theming.
  - `sc-b-meza`: Solicitud de cambio individual de la Parte II con al menos 3 commits.
