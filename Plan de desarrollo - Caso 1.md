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
| F6 | Registrar/confirmar acción realizada ante un evento | Operario |
| F7 | Vista resumen para jefatura (conteo por estado, alertas del día) | Jefatura |
| F8 | Persistencia local: consultas recientes disponibles sin conexión | Todos |

## 3. Arquitectura propuesta

```
UI (Compose) → ViewModel → Repository → { ApiFake (simulada), Room (cache local) }
```

- **data/**: `api/MockMonitorApi.kt`, `db/` (Room: Entidades, DAOs), `repository/GalponRepository.kt`
- **domain/** (opcional simple): modelos `Galpon`, `Medicion`, `Alerta`, `Evento`
- **ui/**: pantallas + ViewModels por pantalla, navegación con `Navigation Compose`
- **notifications/**: `NotificationHelper.kt` para alertas críticas

## 4. Modelos de datos (Room)

- `Galpon(id, granja, nombre, estado)`
- `Medicion(id, galponId, temperatura, humedad, fechaHora)`
- `Alerta(id, galponId, tipo, nivel, activa, fechaHora)`
- `Evento(id, galponId, descripcion, accionRegistrada, fechaHora)`

## 5. Cronograma sugerido (10 días hábiles)

| Día | Actividad |
|-----|-----------|
| 1 | Crear proyecto, dependencias (Room, Navigation, Lifecycle, Notificaciones), estructura de paquetes |
| 2 | Modelos + MockMonitorApi con datos JSON simulados |
| 3 | Room: entidades, DAOs, base de datos, Repository |
| 4 | Navegación + Login por rol (simulado) |
| 5 | Lista de galpones con estados (F2, F8) |
| 6 | Detalle de galpón (F3) |
| 7 | Alertas + notificaciones locales (F4) |
| 8 | Historial + registro de acciones (F5, F6) |
| 9 | Vista jefatura (F7) + modo offline verificado |
| 10 | Pruebas en dispositivo, corrección de bugs, documentación y video demo |

## 6. Dependencias a agregar

```kotlin
implementation("androidx.room:room-runtime:2.6.1")
implementation("androidx.room:room-ktx:2.6.1")
ksp("androidx.room:room-compiler:2.6.1")
implementation("androidx.navigation:navigation-compose:2.8.x")
implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.6.1")
```

## 7. Riesgos y mitigación

- **Conectividad intermitente** → Room como caché, leer siempre desde BD.
- **Notificaciones** → manejar permisos (Android 13+) y canal de notificaciones.
- **Poco tiempo** → priorizar F2–F4 y F8; F5–F7 pueden simplificarse.

## 8. Entregables

- Repositorio/proyecto Android Studio funcional
- Demo en dispositivo/emulador con datos simulados
- Documentación breve: arquitectura, pantallas, decisiones técnicas
- Video o presentación según indicaciones del docente
