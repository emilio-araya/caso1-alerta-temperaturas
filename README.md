# Caso 1 — Alerta temprana por altas temperaturas

Proyecto académico (DSY1105 · Desarrollo de Aplicaciones Móviles, Duoc UC).

App Android con Jetpack Compose para que operarios, supervisores y jefaturas de
una empresa avícola consulten las condiciones ambientales de los galpones,
reciban alertas tempranas y revisen el historial. Funciona con datos simulados,
persistencia local (offline) y notificaciones.

## Funcionalidades

| ID | Funcionalidad | Perfil |
|----|---------------|--------|
| F1 | Login por rol (simulado), sesión recordada con DataStore | Todos |
| F2 | Galpones con semáforo de estado y última temperatura/humedad | Todos |
| F3 | Detalle del galpón con gráfico de temperatura y umbrales | Todos |
| F4 | Alertas activas, notificación local de eventos críticos y confirmación | Todos (confirma operario/supervisor) |
| F5 | Historial de alertas (activas/resueltas) y acciones, filtrable por galpón | Supervisor, Jefatura |
| F6 | Registro de acción con formulario validado y pantalla de confirmación | Operario |
| F7 | Resumen: conteo por estado, alertas activas y alertas del día | Jefatura |
| F8 | Funciona sin conexión: la UI siempre lee de Room | Todos |
| F9 | Diseño adaptable con Window Size Classes (1 columna / 2 columnas / lista + detalle) | Todos |

**Umbrales** (`Umbrales` en `data/model/Modelos.kt`):

| Estado | Temperatura | Humedad |
|--------|-------------|---------|
| Normal | 18–28 °C | 50–70 % |
| Advertencia | 28–32 °C o < 18 °C | 70–80 % o < 50 % |
| Crítico | > 32 °C | > 80 % |

## Arquitectura

MVVM con Room como fuente única de verdad:

```
UI (Compose) → ViewModel (StateFlow) → GalponRepository → { MockMonitorApi, Room }
                                                ↑
                         AlertasWorker (WorkManager, cada 15 min)
```

- `data/api/MockMonitorApi.kt` — API simulada y determinista: estado, alertas e
  historial siempre coinciden. Ciclo suave de 24 h con una medición cada 15 min.
- `data/db/` — Room: galpones, mediciones, alertas y eventos (con migración 2→3).
- `data/repository/GalponRepository.kt` — sincroniza la API con Room; una alerta
  activa por galpón, sin duplicados; confirmación de alertas.
- `viewmodel/` — un ViewModel por pantalla; `SessionViewModel` para la sesión.
- `ui/screen/` — pantallas Compose; `ui/navigation/` — Navigation Compose.
- `work/AlertasWorker.kt` — revisión en segundo plano y notificaciones.
- `notifications/NotificationHelper.kt` — canal y notificación de alertas críticas.

**Alertas:** *confirmar* significa acusar recibo (queda quién y cuándo, y una
entrada en el historial). La alerta sigue activa hasta que el galpón vuelve a la
normalidad; si empeora (advertencia → crítico) se genera una alerta nueva.

## Cómo ejecutar

1. Abrir el proyecto en Android Studio (Gradle descarga el JDK 25 configurado; minSdk 24, targetSdk 37).
2. Ejecutar la configuración `app` en un emulador o dispositivo.
3. Aceptar el permiso de notificaciones (Android 13+).

Tests unitarios (umbrales y coherencia del mock):

```bash
./gradlew testDebugUnitTest
```

## Guion de demo

1. Entrar como **Operario** → menú ⋮ → *Simular evento crítico (demo)* → botón Inicio.
2. A los 10 s llega la notificación → abrir **Alertas** → **Confirmar**.
3. **Registrar acción** (formulario validado → confirmación).
4. Cerrar sesión → **Supervisor** → *Ver historial*: alerta confirmada y acciones.
5. **Jefatura** → resumen por estado y alertas del día.
6. Girar el dispositivo o usar una tablet: 2 columnas / lista + detalle.

> El modo demo guarda el pico de temperatura en memoria: si Android cierra el
> proceso antes de los 10 s, la notificación no llega. La revisión periódica
> puede retrasarse por las optimizaciones de batería de Android (Doze).

## Documentación

- [Plan de desarrollo](<Plan de desarrollo - Caso 1.md>)

## Licencia

MIT — uso libre con atribución. Trabajo universitario; no usar para fines comerciales sin autorización.
