# Plan de Desarrollo — Caso 1: Alerta temprana por altas temperaturas

**Asignatura:** DSY1105 · Desarrollo de Aplicaciones Móviles
**Plazo:** 1–2 semanas (MVP)
**Stack:** Kotlin + Jetpack Compose · minSdk 24 · Arquitectura MVVM
**Persistencia:** Room · **API:** Repositorio fake en la app

---

## 1. Objetivo del MVP

App Android que permita a operarios, supervisores y jefaturas consultar el estado ambiental de los galpones (temperatura, humedad, estado normal/advertencia/crítico), recibir alertas, revisar historial y registrar acciones, funcionando con datos simulados y acceso offline.

## 2. Alcance funcional (MVP)

| ID | Funcionalidad | Perfil |
|----|---------------|--------|
| F1 | Login por rol (operario / supervisor / jefatura) — simulado | Todos |
| F2 | Lista de granjas y galpones con estado actual (semáforo) | Todos |
| F3 | Detalle de galpón: temperatura, humedad, última actualización, estado | Todos |
| F4 | Alertas activas (notificación local al detectar evento crítico) | Todos |
| F5 | Historial de mediciones y eventos (gráfico simple + lista) | Supervisor, Jefatura |
| F6 | Registrar/confirmar acción realizada ante un evento — **formulario reactivo con validaciones** (descripción obligatoria, tipo de acción, galpón) y confirmación en pantalla con ViewModel compartido | Operario |
| F9 | Adaptabilidad con **Window Size Classes** (Material 3): ajustar layouts a pantallas compact/medium/expanded | Todos |
| F7 | Vista resumen para jefatura (conteo por estado, alertas del día) | Jefatura |
| F8 | Persistencia local: consultas recientes disponibles sin conexión | Todos |

## 3. Arquitectura propuesta

```
UI (Compose) → ViewModel → Repository → { ApiFake (simulada), Room (cache local) }
```

- **data/**: `api/MockMonitorApi.kt`, `db/` (Room: Entidades, DAOs), `repository/GalponRepository.kt`
- **domain/** (opcional simple): modelos `Galpon`, `Medicion`, `Alerta`, `Evento`, `Umbrales`
- **ui/**: pantallas + ViewModels por pantalla, navegación con `Navigation Compose`
- **notifications/**: `NotificationHelper.kt` para alertas críticas
- **Adaptabilidad (Guía 9):** usar `WindowSizeClass` de Material 3 para elegir layout según `WindowWidthSizeClass` (compact/medium/expanded); p. ej. lista + detalle lado a lado en pantallas expandidas, apilados en compactas.
- **Formularios reactivos (Guía 11):** F6 implementado con `data class` de estado, validaciones en el ViewModel expuestas con `StateFlow`/`collectAsState`, y pantalla de confirmación que recibe los datos mediante ViewModel compartido (sin pasar argumentos por rutas).

**Estrategia offline (single source of truth):** Room es la única fuente que la UI observa (Flows). El repositorio refresca desde `MockMonitorApi` al entrar a cada pantalla y guarda en Room; sin conexión, la UI muestra los últimos datos cacheados.

**Sesión:** el rol del login se persiste con `DataStore` para no pedir login en cada reinicio.

## 4. Modelos de datos (Room)

- `Galpon(id, granja, nombre, estado)`
- `Medicion(id, galponId, temperatura, humedad, fechaHora)`
- `Alerta(id, galponId, tipo, nivel, activa, fechaHora)`
- `Evento(id, galponId, descripcion, accionRegistrada, fechaHora)`

## 4.1 Reglas de negocio (umbrales)

| Estado | Temperatura | Humedad |
|--------|-------------|---------|
| Normal | 18–28 °C | 50–70 % |
| Advertencia | 28–32 °C o 70–80 % | — |
| Crítico | > 32 °C o > 80 % | — |

- La detección de alertas ocurre en el repositorio/caso de uso: al guardar una `Medicion` nueva se evalúa contra estos umbrales y se genera una `Alerta` con nivel correspondiente.
- Al detectar nivel crítico se dispara `NotificationHelper` (canal de notificaciones, permiso POST_NOTIFICATIONS en Android 13+).

## 4.2 Simulación de alertas en tiempo real

Como no hay backend real, las alertas se simulan con un `WorkManager` periódico (o un `LaunchedEffect` con delay en la pantalla principal) que consulta el mock, genera mediciones nuevas y dispara la notificación local cuando corresponde. Esto permite demostrar el flujo completo en la demo.

## 5. Cronograma sugerido (10 días hábiles)

| Día | Actividad |
|-----|-----------|
| 1 | Crear proyecto, dependencias (Room, Navigation, Lifecycle, Notificaciones, DataStore), estructura de paquetes |
| 2 | Modelos + MockMonitorApi con datos JSON simulados + reglas de umbrales |
| 3 | Room: entidades, DAOs, base de datos, Repository (single source of truth) |
| 4 | Navegación + Login por rol (simulado) + persistencia de sesión con DataStore |
| 5 | Lista de galpones con estados (F2, F8) |
| 6 | Detalle de galpón (F3) |
| 7 | Alertas + notificaciones locales + simulación periódica (F4) |
| 8 | Historial + registro de acciones (F5, F6) — gráfico con Vico o Canvas + formulario validado |
| 9 | Vista jefatura (F7) + adaptabilidad con Window Size Classes (F9) + modo offline verificado |
| 10 | Pruebas en dispositivo, 1–2 tests unitarios (ViewModel/Repository), corrección de bugs, documentación y video demo |

## 6. Dependencias a agregar

```kotlin
implementation("androidx.room:room-runtime:2.6.1")
implementation("androidx.room:room-ktx:2.6.1")
ksp("androidx.room:room-compiler:2.6.1")
implementation("androidx.navigation:navigation-compose:2.8.x")
implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.6.1")
implementation("androidx.datastore:datastore-preferences:1.1.1")
implementation("androidx.work:work-runtime-ktx:2.9.1")
// Gráfico (opcional): implementation("com.patrykandpatrick.vico:compose:1.13.0")
```

## 7. Riesgos y mitigación

- **Conectividad intermitente** → Room como única fuente, leer siempre desde BD.
- **Notificaciones** → manejar permisos (Android 13+) y canal de notificaciones.
- **Poco tiempo** → priorizar F2–F4 y F8; F5–F7 pueden simplificarse.
- **Simulación poco realista** → usar WorkManager/periodicidad para que las alertas aparezcan durante la demo.

## 8. Entregables

- Repositorio/proyecto Android Studio funcional
- Demo en dispositivo/emulador con datos simulados
- Documentación breve: arquitectura, pantallas, decisiones técnicas, reglas de umbrales
- Video o presentación según indicaciones del docente
