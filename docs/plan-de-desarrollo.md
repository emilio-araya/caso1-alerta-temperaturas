# Plan de Desarrollo — Caso 1: Alerta temprana por altas temperaturas

**Asignatura:** DSY1105 · Desarrollo de Aplicaciones Móviles
**Stack:** Kotlin + Jetpack Compose + Material 3 · minSdk 24 · targetSdk 37 · MVVM
**Persistencia:** solo DataStore — Preferences (sesión) + tipado JSON `monitoreo.json` (datos) · **API:** Retrofit contra json-server (datos simulados)

---

## 1. Objetivo del MVP

App Android que permita a operarios, supervisores y jefaturas consultar el estado ambiental de los galpones (temperatura, humedad, estado normal/advertencia/crítico), recibir alertas, revisar historial y registrar acciones, consumiendo una API con datos simulados y funcionando sin conexión.

El caso exige demostrar **navegación, gestión de estado, persistencia local, consumo de API y notificaciones** (punto 5.1). La sección 3 muestra dónde se cubre cada uno.

## 2. Alcance funcional

| ID | Funcionalidad | Perfil | Estado |
|----|---------------|--------|--------|
| F1 | Login por rol (simulado), sesión recordada con DataStore | Todos | Hecho |
| F2 | Galpones con semáforo de estado y última temperatura/humedad | Todos | Hecho |
| F3 | Detalle del galpón con gráfico de temperatura y umbrales | Todos | Hecho |
| F4 | Alertas activas, notificación local de eventos críticos y confirmación | Todos | Hecho |
| F5 | Historial de alertas (medición, nivel y estado de atención) y acciones con usuario, filtrable por galpón, tipo y fecha | Supervisor, Jefatura | Hecho |
| F6 | Registro de acción con formulario validado y confirmación (ViewModel compartido) | Operario | Hecho |
| F7 | Resumen: conteo por estado, alertas activas y del día | Jefatura | Hecho |
| F8 | Sin conexión: la UI lee siempre de DataStore; aviso con la hora de los datos | Todos | Hecho |
| F9 | Diseño adaptable con Window Size Classes | Todos | Hecho |
| F10 | Consumo de la API con Retrofit (JSON) desde json-server; deslizar para actualizar | Todos | Hecho |
| F11 | Acción recomendada según el estado (detalle y alertas); notificaciones solo para operario y supervisor (`debeNotificar`) | Todos | Hecho |
| F12 | Simular medición: formulario validado; lectura ficticia guardada en DataStore y evaluada en el teléfono, sin red (`SimulacionScreen`, `registrarMedicionSimulada`) | Todos | Hecho |
| F13 | Mediciones automáticas cada minuto mientras la app está abierta y gráficos de temperatura y humedad por período (`SimuladorAutomatico`, `PeriodoGrafico`) | Todos | Hecho |
| F14 | Foto de evidencia opcional con la cámara del sistema al registrar una acción (permiso `CAMERA` en tiempo de ejecución, `FileProvider`, miniatura en formulario e historial, ruta en DataStore) | Operario | Hecho |
| F15 | Contacto ficticio del supervisor responsable en el detalle del galpón y en cada alerta (solo texto, sin llamadas) | Todos | Hecho |

**Historial y trazabilidad (respuestas 6, 12 y 14):** cada alerta guarda la medición que la produjo y su estado de atención; cada acción guarda el rol del usuario, el galpón, la fecha y la hora, y la foto de evidencia si se adjuntó. El historial filtra por galpón, tipo de alerta y rango (hoy / últimos 7 días / todo), y supervisor y jefatura ven las acciones de los operarios.

## 3. Trazabilidad: guías → funcionalidades → requisitos del caso

| Guía | Concepto | Dónde se usa | Requisito del caso |
|------|----------|--------------|--------------------|
| 9 | Diseño adaptable | F9: `WindowWidthSizeClass` en `HomeScreen` (1 columna / 2 columnas / lista + detalle) | Uso en celulares Android |
| 10 | Navigation Compose, rutas centralizadas, `Scaffold`, `NavigationBar` | `ui/navigation/AppRoutes.kt`, `AppNavigation.kt`; pestañas según rol | Navegación |
| 11 | Formulario reactivo, validación en ViewModel, ViewModel compartido | F6: `RegistroAccionViewModel` + `ConfirmacionScreen` | Gestión de estado |
| 12 | `StateFlow`, `stateIn`, DataStore, `animateColorAsState` | F1 (sesión), boletín y franjas de estado animadas | Gestión de estado / persistencia |
| 12 | DataStore tipado (`Serializer` + JSON) | F8: fuente única de verdad, `data/local/` | Persistencia local |
| — | Retrofit + json-server | `data/api/`, `mock-api/` | Consumo de API |
| — | Notificaciones + WorkManager | F4: `NotificationHelper`, `AlertasWorker` | Notificaciones |

## 4. Arquitectura

```
UI (Compose) → ViewModel (StateFlow) → RepositorioGalpones → { MonitorApi (Retrofit), DataStore }
                                                ↑                ↑
                         AlertasWorker (WorkManager, cada 15 min)  SimuladorAutomatico (cada 1 min, app abierta)
```

```
com.example.caso1
├── data/
│   ├── api/         MonitorApi, ClienteApi, InterceptorConectividad, DTOs, ServidorSimulado (alternativa sin PC)
│   ├── local/       DataStore tipado: DatosMonitoreo (monitoreo.json), registros guardados
│   ├── model/       Modelos de dominio y Umbrales
│   ├── repository/  RepositorioGalpones (interfaz) + GalponRepository
│   └── SessionManager (DataStore), Sincronizador, MonitorConectividad
├── ui/
│   ├── navigation/  AppRoutes, AppNavigation
│   ├── screen/      una pantalla por archivo + componentes compartidos
│   └── theme/       solo colores, tipografía y tema
├── viewmodel/       un ViewModel por pantalla (+ NavegacionViewModel y SessionViewModel)
├── notifications/   NotificationHelper
└── work/            AlertasWorker
```

### 4.1 API simulada (json-server)

- `mock-api/` contiene un servidor json-server que genera los datos **relativos a la hora actual** (una medición cada 15 min, ciclo diario). Un `db.json` fijo mostraría fechas viejas, por eso no existe.
- Rutas compatibles con `MonitorApi`: `GET galpones`, `GET galpones/{id}/mediciones`, `GET alertas/activas` y `POST simulaciones/pico-calor` (demo).
- La app se conecta a `http://localhost:3000/api/v1/` mediante `adb reverse tcp:3000 tcp:3000`, que funciona con emulador y con celular por USB. La URL sale de `BuildConfig.API_URL_BASE` (propiedad Gradle `caso1.apiUrl`, validada al compilar).
- HTTP sin cifrar solo para `localhost` y `10.0.2.2` (`network_security_config.xml`); el resto exige HTTPS. El servidor escucha solo en loopback.
- Sin PC: `-Pcaso1.apiSimulada=true` usa `ServidorSimulado`, un interceptor de OkHttp que responde el mismo JSON dentro de la app.

### 4.2 Estrategia offline (fuente única de verdad)

DataStore es la única fuente que la UI observa (`Flow`). El `Sincronizador` descarga todo de la API y lo guarda en un solo `updateData` (atómico): si algo falla, DataStore no se toca. Room se quitó porque la rúbrica del curso evalúa DataStore (persistencia avanzada fuera de alcance). Sincroniza al abrir, al deslizar, al volver la red y cada 15 min. Sin conexión, la UI muestra lo guardado con un aviso y la hora de los datos.

### 4.3 Navegación (Guía 10)

- Rutas en `AppRoutes`, sin strings sueltos.
- Pestañas con `popUpTo(HOME) { saveState = true }`, `launchSingleTop` y `restoreState`: no se apilan pantallas repetidas y cada pestaña conserva su estado.
- Las pantallas reciben callbacks; ningún ViewModel conoce el `NavController`.

### 4.4 Estado y formularios (Guías 11 y 12)

- Cada pantalla expone un único estado con `StateFlow` y se observa con `collectAsStateWithLifecycle()`.
- F6 valida galpón, tipo de acción y descripción en el ViewModel; cada error se limpia al editar su campo. El texto se mantiene en memoria (no se lee de vuelta desde DataStore en cada tecla, el problema del ejemplo de la Guía 12).

### 4.5 Diseño adaptable (Guía 9)

La Guía 9 enseña `BoxWithConstraints` con corte en 600dp. La app usa la herramienta oficial **Window Size Classes**, que define los mismos cortes: compact (< 600dp) una columna con barra inferior; medium (600–839dp) dos columnas con barra inferior; expanded (≥ 840dp) riel lateral y lista y detalle lado a lado.

## 5. Reglas de negocio (umbrales)

Se evalúan por separado temperatura y humedad; **el estado del galpón es el peor de los dos**.

| Estado | Temperatura (T, °C) | Humedad (H, %) |
|--------|---------------------|----------------|
| Normal | 18 ≤ T ≤ 28 | 50 ≤ H ≤ 70 |
| Advertencia | 28 < T ≤ 32 o T < 18 | 70 < H ≤ 80 o H < 50 |
| Crítico | T > 32 | H > 80 |

- **Supuesto (confirmar con el docente):** frío (T < 18) y ambiente seco (H < 50) son *Advertencia*; el caso solo menciona el calor.
- Son los valores **predeterminados**: los umbrales son ficticios y configurables (respuesta 1 del docente). Supervisor y jefatura los cambian en menú ⋮ → *Configurar umbrales*; la validación está en `viewmodel/ValidacionUmbrales.kt` y se guardan en DataStore (`umbrales`, `data/local/RepositorioUmbrales.kt`).
- La regla se evalúa **en el dispositivo** (`ConfigUmbrales` en `data/model/Modelos.kt`): `GalponRepository` calcula el estado y la alerta de cada galpón con su última medición; al guardar umbrales nuevos se recalcula sin red. Los umbrales de `mock-api/datos.js` solo simulan el servidor. Tests en los bordes (17.9/18, 28/28.1, 32/32.1 °C; 49.9/50, 70/70.1, 80/80.1 %) y con umbrales personalizados.
- Una alerta crítica nueva genera una notificación; una alerta que sigue activa no se vuelve a notificar.

## 6. Notificaciones y demo

- Canal de notificaciones creado al iniciar la app; permiso `POST_NOTIFICATIONS` pedido en Android 13+.
- `AlertasWorker` periódico cada 15 min con restricción de red.
- **Mediciones automáticas (respuesta 10):** con la sesión iniciada, `SimuladorAutomatico` agrega cada minuto una lectura ficticia por galpón (temperatura, humedad, galpón, fecha y hora) y el repositorio la evalúa con los umbrales; funciona sin red. Los valores los genera `valoresSimulados()` de forma determinista. WorkManager no puede despertar cada minuto (mínimo 15), por eso el ciclo corre mientras el proceso de la app está abierto.
- **Gráficos (respuesta 11):** el detalle muestra dos gráficos (temperatura y humedad) con el período *6 h*, *24 h* o *Todo*, los umbrales sombreados y la última lectura marcada.
- Menú ⋮ → *Simular pico de calor* y *Simular sin conexión*: **solo en builds debug**. El pico se pide al servidor (`POST simulaciones/pico-calor`) y el worker lo detecta a los 10 s.
- **Foto de evidencia (rúbrica EP3, recurso nativo cámara):** en el registro de una acción, *Tomar foto* pide el permiso `CAMERA` en tiempo de ejecución si hace falta y abre la app de cámara del sistema con `ActivityResultContracts.TakePicture`; la foto se escribe en `files/evidencias/` y se comparte con `FileProvider` (`res/xml/file_paths.xml`). La ruta queda en `EventoEntity.fotoRuta` (DataStore) y la miniatura se muestra en el formulario y en el historial. Si se niega el permiso, el formulario explica que la foto es opcional y la acción se guarda igual. No se usa CameraX ni una dependencia nueva.
- **Contacto del supervisor (respuesta 8):** `data/model/Supervisores.kt` asigna siempre el mismo supervisor ficticio a cada galpón. Se muestra en la tarjeta *Supervisor responsable* del detalle y en una línea de cada alerta. Solo texto: sin llamadas ni permiso `CALL_PHONE`.

## 7. Calidad

| Tipo | Qué cubre | Cómo correrlo |
|------|-----------|---------------|
| Unitarios (JVM) | Umbrales y bordes, cliente HTTP y conectividad, API simulada interna, validación y ViewModel de F6, sincronizador, valores y período de los gráficos, repositorio sobre DataStore (sin red, error 500 o servidor apagado → se siguen sirviendo los datos guardados; alertas, retención, mediciones automáticas y persistencia) | `./gradlew testDebugUnitTest` (también en CI) |
| API simulada | Endpoints, orden, 404, pico de calor y umbrales | `cd mock-api && npm test` |

Las dependencias están en `gradle/libs.versions.toml`, sin versiones escritas a mano en `build.gradle.kts`.

## 8. Riesgos y mitigación

| Riesgo | Mitigación |
|--------|------------|
| Conectividad intermitente | DataStore como fuente única, aviso con la hora de los datos y reconexión automática |
| Permiso de notificaciones denegado | La alerta se ve igual dentro de la app, con contador en la barra |
| Permiso de cámara denegado | La foto es opcional: se explica y la acción se guarda sin foto |
| WorkManager tarda (≥ 15 min) | Simulación de pico de calor en builds debug |
| json-server no disponible en la demo | Compilar con `-Pcaso1.apiSimulada=true` |
| `adb reverse` se pierde al reconectar el dispositivo | Repetir `npm run android` en `mock-api/` |
| Instrucciones contradictorias entre guías | Confirmar con el docente y registrar la decisión en el README |

## 9. Entregables

- Repositorio con CI, ramas `feature/...` o `feat/...` y Conventional Commits.
- `mock-api/` con instrucciones para levantar la API.
- Demo: login por rol → semáforo → pico de calor simulado → notificación → confirmar → registrar acción → historial → modo sin conexión.
- README: arquitectura, umbrales, decisiones técnicas y supuestos.
- Video o presentación según indique el docente.
