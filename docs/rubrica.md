# Trazabilidad de la rúbrica

Mapa entre cada indicador de evaluación (IE) de EP2 y EP3, el lugar del proyecto donde se evidencia y cómo mostrarlo en la defensa.
Rutas relativas a `app/src/main/java/com/example/caso1/`.

## EP2 — Encargo (8 %)

| IE | Peso | Dónde está | Cómo demostrarlo |
|----|------|-----------|------------------|
| 2.1.1 Interfaz coherente, jerárquica y con navegación funcional | 15 % | `ui/screen/` (pantallas y `Marco`), `ui/navigation/AppNavigation.kt`, `ui/theme/` | Recorrer Login → Galpones → Detalle → Alertas → Historial y mostrar que la barra inferior y el botón atrás respetan la jerarquía; en pantalla expandida el detalle se muestra como panel lateral |
| 2.1.2 Formularios completos con validación visual por campo, retroalimentación e íconos | 15 % | `ui/screen/RegistroAccionScreen.kt`, `ui/screen/UmbralesScreen.kt`, `ui/screen/SimulacionScreen.kt` | Enviar cada formulario vacío y mostrar `isError`, `supportingText` e ícono de advertencia por campo; luego corregir un campo y ver que su error desaparece |
| 2.2.1 Validación centralizada y desacoplada de la interfaz | 10 % | `viewmodel/ValidacionRegistro.kt`, `viewmodel/ValidacionUmbrales.kt`, `viewmodel/ValidacionSimulacion.kt` | Mostrar que las tres funciones son puras (sin Compose ni Android) y que sus tests las cubren; cambiar una regla ahí y ver que la pantalla responde |
| 2.2.2 Animaciones visuales funcionales | 10 % | `ui/screen/Componentes.kt` (`animateColorAsState` en la franja de estado, `AnimatedVisibility`) y `ui/screen/HomeScreen.kt` (boletín de estado) | Cambiar de estado un galpón con el simulador y mostrar cómo la franja y el boletín transicionan de color |
| 2.3.1 Modularidad, separación de responsabilidades y persistencia local | 15 % | Paquetes `data/api` (red), `data/local` (DataStore), `data/repository` (reglas), `data/model` (dominio), `viewmodel/`, `ui/` | Explicar el flujo DTO → repositorio → DataStore → `StateFlow` → pantalla; mostrar los tres archivos de DataStore |
| 2.3.2 Uso de herramientas de colaboración y control de versiones | 20 % | Historial de git del repositorio | **No aplica como trabajo grupal**: el proyecto se desarrolló de forma individual. El historial de commits evidencia el uso de Git; si se evalúa participación grupal, este criterio no puede cumplirse en solitario |
| 2.4.1 Al menos dos recursos del dispositivo, integrados con coherencia | 15 % | **Notificaciones**: `notifications/NotificationHelper.kt`, permiso `POST_NOTIFICATIONS`, canal de importancia alta. **Cámara**: `RegistroAccionScreen.kt`, `data/local/Evidencias.kt`, permiso `CAMERA` y `FileProvider` | Provocar una alerta crítica (simulador o medición automática) y mostrar la notificación; luego registrar una acción con foto y ver la miniatura en el formulario y en el historial |

## EP3 — Presentación y defensa (32 %)

| IE | Peso | Dónde está | Cómo demostrarlo |
|----|------|-----------|------------------|
| 2.1.1 Lógica de validación, cambios justificados y corrección de errores en tiempo real | 20 % | Las tres funciones de validación puras en `viewmodel/` y los 119 tests unitarios | Elegir un campo, romper una regla, mostrar el error; corregirlo y ver que el error desaparece mientras se escribe |
| 2.1.2 Dominio del diseño visual, navegación, formularios y usabilidad | 7 % | `ui/screen/`, `ui/theme/`, `ui/navigation/` | Recorrido completo de la app en el emulador, incluyendo pantalla expandida |
| 2.2.1 Estructura modular con una clase, módulo, componente o animación nueva | 22 % | `data/local/` (DataStore tipado), `data/SimuladorAutomatico.kt`, `viewmodel/PeriodoGrafico.kt`, `ui/screen/Componentes.kt` | Mostrar el archivo nuevo y la interfaz que lo usa; explicar por qué quedó en ese paquete |
| 2.2.2 Estructuras de control, gestión de estado y respuesta dinámica de la interfaz | 7 % | ViewModels con `StateFlow` y `collectAsStateWithLifecycle`; bucle de simulación con `delay` | Simular una medición y ver cambiar el estado del galpón, su color, el gráfico y, si es crítica, la notificación |
| 2.3.1 Almacenamiento local: cómo se guardan y recuperan los datos | 18 % | `data/SessionManager.kt` (sesión, Preferences DataStore), `data/local/RepositorioUmbrales.kt` (umbrales), `data/local/DatosMonitoreo.kt` (cache JSON con galpones, mediciones, alertas y eventos) | Cerrar la app, reabrirla y mostrar que la sesión, los umbrales y el historial siguen ahí; mostrar `updateData` como escritura atómica |
| 2.3.2 Arquitectura, patrones y decisiones de organización | 7 % | `docs/plan-de-desarrollo.md` (capas y decisiones), `data/repository/GalponRepository.kt` (regla de negocio en el dispositivo) | Explicar por qué la regla de umbrales vive en el repositorio y no en el servidor |
| 2.4.1 Integración de recursos nativos: permisos, flujos y medidas de seguridad | 7 % | Permisos en `AndroidManifest.xml`; permiso en tiempo de ejecución en `RegistroAccionScreen.kt`; `res/xml/file_paths.xml` | Mostrar el diálogo de permiso de cámara, la explicación cuando se niega y el `FileProvider` (la cámara no puede escribir en un archivo privado sin él) |
| 2.4.2 Resultado del recurso nativo en la interfaz en tiempo real | 12 % | Miniatura de la evidencia en el formulario y en el historial; notificación con el galpón y la medición | Tomar la foto, ver la miniatura, guardar y encontrarla en Historial → Acciones |

## Respuestas del docente

Las 14 preguntas y respuestas están transcritas en `docs/preguntas-respuestas-caso.md`, con la columna **En la app** que indica dónde se cumple cada una.
