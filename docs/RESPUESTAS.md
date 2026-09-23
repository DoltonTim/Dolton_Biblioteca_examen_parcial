# BiblioAndes — Banco Oficial de Preguntas de Defensa Técnica

Documento de sustentación técnica correspondiente a la **Parte III** del Examen Parcial de la Unidad 1 (Versión B - Caso BiblioAndes).  
Cada respuesta incluye la ubicación exacta del archivo en el código fuente, la justificación arquitectónica y el fragmento relevante para mostrar al docente.

---

### Pregunta 1
**Muéstrame dónde vive la regla RN-01 y explica por qué la ubicaste allí y no en la pantalla.**

* **Ubicación en el código:**  
  `shared/src/commonMain/kotlin/pe/upeu/biblioandes/domain/usecase/SolicitarPrestamoUseCase.kt` (Líneas 48 a 56)

* **Código a mostrar en pantalla:**
  ```kotlin
  // REGLA DE NEGOCIO RN-01:
  // Un estudiante no puede tener más de tres préstamos en estado Activo de forma simultánea.
  val totalPrestamosActivos = prestamosActuales.count { it.estado is EstadoPrestamo.Activo }
  if (totalPrestamosActivos >= 3) {
      return ResultadoPrestamo.Error(
          mensaje = "Límite alcanzado: no puedes tener más de tres préstamos activos simultáneamente.",
          regla = "RN-01"
      )
  }
  ```

* **Justificación técnica:**  
  La regla RN-01 vive en la capa de **Dominio** dentro del caso de uso `SolicitarPrestamoUseCase` porque constituye una regla de negocio fundamental e inviolable del sistema. Ubicarla en la pantalla (`Composable`) o en el `ViewModel` violaría el principio de responsabilidad única (SRP) y Clean Architecture: si mañana se agrega otra pantalla, un deep link, una tarea en segundo plano o una versión web/desktop, la regla tendría que duplicarse o podría eludirse. Al centralizarla en el caso de uso, el dominio se auto-protege garantizando que ninguna operación pueda crear un cuarto préstamo activo sin importar desde qué cliente o plataforma se invoque.

---

### Pregunta 2
**Si mañana el servicio web estuviera listo, ¿qué archivos tendrías que crear o modificar para conectarlo? Nómbralos uno por uno.**

* **Archivos a CREAR (Capa Data):**
  1. `data/repository/BibliotecaRepositoryRemote.kt`: Nueva clase que implementa la interfaz `BibliotecaRepository` existente, utilizando un cliente HTTP (como Ktor Client) para consumir los endpoints REST del backend.
  2. `data/remote/dto/LibroDto.kt`: DTOs de transferencia con serialización `@Serializable` y funciones de extensión para mapear a los modelos de dominio `Libro`.

* **Archivos a MODIFICAR (Configuración e Inyección):**
  1. `di/AppModule.kt`: En `dataModule`, sustituir una sola línea:
     ```kotlin
     // Antes: single<BibliotecaRepository> { BibliotecaRepositoryFake() }
     single<BibliotecaRepository> { BibliotecaRepositoryRemote(get()) }
     ```
  2. `gradle/libs.versions.toml` y `shared/build.gradle.kts`: Agregar las librerías de red (Ktor Client y Serialization) una vez autorizadas para la Unidad 2.

* **Archivos que quedan 100% INTACTOS (Sin tocar ni una sola línea):**
  - **Capa Domain:** `Libro.kt`, `Prestamo.kt`, `EstadoPrestamo.kt`, `Estudiante.kt`, `BibliotecaRepository.kt` (interfaz), `ObtenerCatalogoUseCase.kt`, `SolicitarPrestamoUseCase.kt`, `ObtenerPrestamosUseCase.kt`.
  - **Capa Presentation:** `CatalogoViewModel.kt`, `CatalogoScreen.kt`, `DetalleLibroViewModel.kt`, `DetalleLibroScreen.kt`, `PrestamosViewModel.kt`, `PrestamosScreen.kt`, `InicioScreen.kt`, `PerfilScreen.kt`, `AppNavHost.kt`.
  - **Justificación:** Se cumple la Regla de Dependencia de Clean Architecture: las capas internas (dominio y presentación) dependen de la abstracción (`BibliotecaRepository`), jamás de la implementación concreta.

---

### Pregunta 3
**¿Por qué el estado del préstamo es una sealed class y no un enum o una cadena de texto?**

* **Ubicación en el código:**  
  `shared/src/commonMain/kotlin/pe/upeu/biblioandes/domain/model/EstadoPrestamo.kt`

* **Código a mostrar en pantalla:**
  ```kotlin
  sealed class EstadoPrestamo {
      data class Activo(val diasRestantes: Int) : EstadoPrestamo()
      data class Devuelto(val fechaDevolucion: String) : EstadoPrestamo()
      data class Vencido(val diasDeAtraso: Int) : EstadoPrestamo()
  }
  ```

* **Justificación técnica:**  
  Cada estado del ciclo de vida de un préstamo transporta **información heterogénea exclusiva**:
  - `Activo` necesita conocer cuántos días restan (`diasRestantes: Int`).
  - `Devuelto` necesita la fecha exacta de devolución (`fechaDevolucion: String`).
  - `Vencido` necesita los días de mora acumulados (`diasDeAtraso: Int`).
  
  Si utilizáramos un `enum` o un `String`, tendríamos que arrastrar todas esas propiedades como campos anulables dentro de la entidad `Prestamo` (`val diasRestantes: Int?`, `val fechaDevolucion: String?`, `val diasDeAtraso: Int?`), lo cual llenaría el modelo de valores nulos y permitiría estados imposibles (por ejemplo, un préstamo con estado "Devuelto" que simultáneamente tenga 5 días de atraso). Con una `sealed class`, el compilador de Kotlin garantiza **seguridad estricta de tipos** y **exhaustividad** en las sentencias `when` en la interfaz gráfica, evitando bugs en tiempo de ejecución.

---

### Pregunta 4
**Explica el recorrido completo de un libro desde la fuente simulada hasta el pixel que se dibuja en el catálogo.**

* **Paso a paso del flujo unidireccional de datos:**
  1. **Fuente de Datos (`data/local/DatosSimulados.kt`):** El libro se define en la lista inmutable de semillas `DatosSimulados.libros`.
  2. **Repositorio Simulado (`data/repository/BibliotecaRepositoryFake.kt`):** Se almacena dentro de un `MutableStateFlow(_libros)` reactivo. La función `getCatalogo(): Flow<List<Libro>>` introduce un `delay(800)` para simular la latencia de red antes de emitir los datos.
  3. **Caso de Uso (`domain/usecase/ObtenerCatalogoUseCase.kt`):** Consume el flujo del repositorio con el operador `.map { ... }`. Si hay filtros activos, filtra la lista por la categoría elegida y aplica búsqueda insensible a mayúsculas y acentos (`normalizar(...)`).
  4. **ViewModel (`presentation/catalogo/CatalogoViewModel.kt`):** En `cargarCatalogo()`, inicia una corrutina en `viewModelScope`, recolecta el flujo emitido y transforma la lista en el estado inmutable `_uiState.value = CatalogoUiState.Success(libros = ...)`.
  5. **Pantalla Composable (`presentation/catalogo/CatalogoScreen.kt`):** Se suscribe al estado mediante `viewModel.uiState.collectAsState()`. Al recibir `CatalogoUiState.Success`, evalúa el `when` e instancia un `LazyColumn`.
  6. **Componente Reutilizable (`LibroCardItem`):** Por cada libro en la lista, se ejecuta `LibroCardItem(libro = libro, onClick = ...)`, donde Jetpack Compose calcula las mediciones de layout (medir, posicionar) y el motor gráfico dibuja el texto del título, autor y badge de ejemplares en el píxel de la pantalla.

---

### Pregunta 5
**¿Qué diferencia hay entre tu UiState y tu modelo de dominio? Muéstrame ambos.**

* **Ubicación en el código:**
  - Modelo de Dominio: `shared/src/commonMain/kotlin/pe/upeu/biblioandes/domain/model/Libro.kt`
  - UiState: `shared/src/commonMain/kotlin/pe/upeu/biblioandes/presentation/catalogo/CatalogoUiState.kt`

* **Código a mostrar en pantalla:**
  ```kotlin
  // Modelo de Dominio (Libro.kt): Entidad pura del negocio
  data class Libro(
      val id: Int,
      val titulo: String,
      val autor: String,
      val anio: Int,
      val categoria: String,
      val sede: String,
      val ejemplaresDisponibles: Int
  )

  // UiState (CatalogoUiState.kt): Estado completo de la vista
  sealed interface CatalogoUiState {
      data object Loading : CatalogoUiState
      data class Success(
          val libros: List<Libro>,
          val categorias: List<String>,
          val categoriaSeleccionada: String = "Todas",
          val busqueda: String = "",
          val simularError: Boolean = false
      ) : CatalogoUiState
      data class Empty(val mensaje: String, val categorias: List<String>, ...) : CatalogoUiState
      data class Error(val mensaje: String) : CatalogoUiState
  }
  ```

* **Diferencia conceptual:**  
  - El **Modelo de Dominio** modela las reglas, entidades y propiedades del negocio (los atributos intrínsecos de un libro). Es completamente agnóstico de la interfaz de usuario: no sabe si se está mostrando en un móvil, en una consola o si la pantalla está cargando.
  - El **UiState** modela la realidad visual de una pantalla específica en un instante del tiempo. Contiene los estados de la interfaz (`Loading`, `Empty`, `Error`, `Success`) y variables de control visual (qué chip está marcado, el texto escrito en el buscador o la bandera de simulación de error).

---

### Pregunta 6
**¿Dónde se ejecuta la corrutina que simula el retardo de carga y qué ocurre si la pantalla se destruye mientras corre?**

* **Ubicación:**  
  Se ejecuta en `BibliotecaRepositoryFake.kt` mediante la función de suspensión `delay(800)` y es orquestada en `CatalogoViewModel.kt` dentro de `viewModelScope.launch { ... }`.

* **Qué ocurre si la pantalla se destruye:**  
  `viewModelScope` está atado al ciclo de vida del `ViewModel`. Si la pantalla se destruye definitivamente (el usuario presiona Atrás o cierra la aplicación), el framework invoca automáticamente `viewModel.onCleared()`, cancelando el `Job` raíz de `viewModelScope`. La corrutina que estaba suspendida en `delay(800)` recibe una excepción `CancellationException`, deteniéndose de inmediato y liberando todos los recursos en memoria sin provocar fugas (*memory leaks*) ni intentar actualizar una UI inexistente. Si la pantalla se destruye únicamente por rotación de pantalla (*configuration change*), el `ViewModel` sobrevive y la corrutina continúa su ejecución sin interrupciones.

---

### Pregunta 7
**¿Por qué el ViewModel expone un StateFlow y no una variable mutable pública?**

* **Ubicación en el código:**  
  `shared/src/commonMain/kotlin/pe/upeu/biblioandes/presentation/catalogo/CatalogoViewModel.kt` (Líneas 18 a 20)

* **Código a mostrar:**
  ```kotlin
  private val _uiState = MutableStateFlow<CatalogoUiState>(CatalogoUiState.Loading)
  val uiState: StateFlow<CatalogoUiState> = _uiState.asStateFlow()
  ```

* **Justificación técnica:**  
  Para respetar el principio de **Encapsulamiento** y el patrón de **Flujo Unidireccional de Datos (UDF)**.  
  Si expusiéramos una variable mutable pública (`MutableStateFlow`), cualquier composable o clase externa podría alterar el estado arbitrariamente (`viewModel.uiState.value = ...`), eludiendo las validaciones del ViewModel y provocando estados inconsistentes o condiciones de carrera. Al exponer una interfaz de solo lectura `StateFlow`, la vista tiene prohibido modificar el estado directamente y solo puede emitir eventos mediante funciones públicas del ViewModel (`seleccionarCategoria`, `onBusquedaCambio`).

---

### Pregunta 8
**Muéstrame un composable que hayas hecho reutilizable y explica qué recibe y qué no sabe.**

* **Ubicación en el código:**  
  `LibroCardItem` en `shared/src/commonMain/kotlin/pe/upeu/biblioandes/presentation/catalogo/CatalogoScreen.kt`

* **Código a mostrar:**
  ```kotlin
  @Composable
  fun LibroCardItem(
      libro: Libro,
      onClick: () -> Unit
  ) { ... }
  ```

* **Qué recibe:**
  1. La entidad inmutable de dominio `Libro` con sus datos listos para pintar.
  2. Una función lambda `onClick: () -> Unit` para notificar cuando el usuario presiona la tarjeta (patrón *State Hoisting*).

* **Qué NO sabe:**
  - No sabe de dónde proviene el libro (si es de memoria local, SQLite o una API en la nube).
  - No conoce a Koin, ni a los ViewModels, ni a los Casos de Uso.
  - No sabe a qué pantalla navegará la aplicación: desconoce por completo la existencia de `AppNavHost` o `NavController`.  
  Esta ignorancia intencional permite que `LibroCardItem` sea 100% reutilizable en el catálogo, en los resultados de búsqueda, en una sección de favoritos o en pruebas de interfaz.

---

### Pregunta 9
**¿Cómo implementaste el cambio de tema claro/oscuro y en qué punto del árbol de composición se aplica?**

* **Ubicación en el código:**  
  `shared/src/commonMain/kotlin/pe/upeu/biblioandes/presentation/navigation/AppNavHost.kt` (Línea 40 y 68) y `pe/upeu/biblioandes/presentation/theme/BiblioAndesTheme.kt`

* **Código a mostrar:**
  ```kotlin
  // En AppNavHost.kt:
  var darkTheme by remember { mutableStateOf(false) }

  BiblioAndesTheme(darkTheme = darkTheme) {
      Scaffold(...) { ... }
  }
  ```

* **Explicación:**  
  Se implementó elevando el estado del tema a la cima del componente principal mediante `remember { mutableStateOf(false) }`. Se aplica en el **nodo raíz del árbol de composición**, envolviendo el `Scaffold` general dentro del composable `BiblioAndesTheme(darkTheme = darkTheme)`. Dentro de este tema personalizado, se selecciona dinámicamente `DarkColorScheme` o `LightColorScheme`, proveyendo los colores institucionales a todo el árbol de Compose mediante `MaterialTheme.colorScheme`. Cuando el usuario acciona el `Switch` en `PerfilScreen`, se ejecuta `onDarkThemeChange(nuevoValor)`, actualizando la variable en la raíz y recomponiendo toda la interfaz de inmediato.

---

### Pregunta 10
**Si la biblioteca agregara una sexta categoría, ¿cuántos archivos tocarías? Justifica la respuesta.**

* **Respuesta exacta:**  
  **Exactamente 1 archivo**: `shared/src/commonMain/kotlin/pe/upeu/biblioandes/data/local/DatosSimulados.kt`.

* **Justificación técnica:**  
  En `DatosSimulados.kt`, la lista de categorías es una colección dinámica:
  ```kotlin
  val categorias = listOf("Programación", "Matemática", "Redes", "Gestión", "Literatura", "Ciberseguridad")
  ```
  El repositorio expone este listado a través de `getCategorias(): Flow<List<String>>`. El `CatalogoViewModel` consulta las categorías disponibles desde el repositorio de forma reactiva y el `CatalogoScreen` las dibuja dinámicamente dentro de un contenedor horizontal desplazable (`horizontalScroll`) mediante `categorias.forEach { FilterChip(...) }`. Dado que ningún composable ni caso de uso tiene nombres de categorías fijos o quemados (*hardcoded*), agregar una nueva categoría solo requiere incluirla en los datos semilla y la nueva pestaña/chip aparecerá automáticamente en pantalla.

---

### Pregunta 11
**Muéstrame tu rama de trabajo y explica qué aportaste tú al producto, qué conflicto tuviste que resolver y cómo lo resolviste.**

* **Demostración en terminal Git:**
  ```bash
  git branch -a
  git log --graph --oneline --all --decorate
  git shortlog -sne
  ```

* **Explicación oral de defensa:**
  - "Mi rol en el equipo fue el desarrollo del módulo de préstamos y la lógica de dominio en la rama `feature/prestamos-meza`. Implementé el modelado del estado con la `sealed class EstadoPrestamo`, la pantalla de listado de préstamos ordenada por vencimiento más próximo, y el caso de uso `SolicitarPrestamoUseCase` donde centralicé las 4 reglas de negocio (RN-01 a RN-04), además de la suite de pruebas unitarias automatizadas."
  - "Mi compañero trabajó en `feature/catalogo-huaman` desarrollando el catálogo y el sistema de búsqueda con normalización de acentos."
  - "Al fusionar ambas ramas de funcionalidad hacia `develop`, tuvimos un conflicto en el archivo `di/AppModule.kt`, ya que ambos añadimos inyecciones simultáneas en `domainModule` y `presentationModule`."
  - "Resolvimos el conflicto manteniendo los aportes de ambos de forma estructurada (los casos de uso de catálogo y préstamos en `domainModule`, y sus respectivos ViewModels en `presentationModule`), ejecutamos `./gradlew test` para garantizar que la compilación y los tests pasarán al 100%, y cerramos la integración con un commit de fusión explícito `--no-ff`."