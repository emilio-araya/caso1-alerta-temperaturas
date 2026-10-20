# Caso 1 — Alerta temprana por altas temperaturas

[![CI](https://github.com/emilio-araya/caso1-alerta-temperaturas/actions/workflows/ci.yml/badge.svg)](https://github.com/emilio-araya/caso1-alerta-temperaturas/actions/workflows/ci.yml)

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
| F5 | Historial de alertas (con medición, nivel y estado de atención) y acciones, filtrable por galpón, tipo de alerta y fecha | Supervisor, Jefatura |
| F6 | Registro de acción con formulario validado y pantalla de confirmación | Operario |
| F7 | Resumen: conteo por estado, alertas activas y alertas del día | Jefatura |
| F8 | Funciona sin conexión: la UI siempre lee de DataStore; aviso "Sin conexión" con la hora de los datos y reconexión automática | Todos |
| F10 | Consumo de la API del sistema de monitoreo con Retrofit (JSON), deslizar para actualizar | Todos |
| F9 | Diseño adaptable con Window Size Classes (1 columna / 2 columnas / lista + detalle; barra inferior o riel lateral) | Todos |
| F11 | Qué hacer según el estado en el detalle y en cada alerta (advertencia: revisar ventilación; crítico: alerta prioritaria y botón *Registrar acción correctiva*). Las notificaciones llegan solo a operario y supervisor; la jefatura consulta el panel | Todos (registra el operario) |
| F12 | Simular medición: formulario validado (galpón, °C y %) con vista previa del estado; la lectura ficticia se guarda en DataStore y se evalúa en el teléfono, también sin conexión | Todos |
| F13 | Mediciones automáticas cada 1 minuto mientras la app está abierta y gráficos de temperatura y humedad por período (6 h / 24 h / todo) | Todos |

**Umbrales** (valores predeterminados de `ConfigUmbrales` en `data/model/Modelos.kt`). Son ficticios y configurables (respuesta 1 del docente): supervisor y jefatura los editan en el menú ⋮ → *Configurar umbrales* (formulario validado), se guardan en DataStore (`umbrales`) y el dispositivo evalúa con ellos el estado y las alertas de cada galpón a partir de su última medición. Los umbrales de `mock-api` solo simulan el servidor.

| Estado | Temperatura | Humedad |
|--------|-------------|---------|
| Normal | 18–28 °C | 50–70 % |
| Advertencia | 28–32 °C o < 18 °C | 70–80 % o < 50 % |
| Crítico | > 32 °C | > 80 % |

## Arquitectura

MVVM con DataStore como fuente única de verdad (persistencia local solo con DataStore):

```
UI (Compose) → ViewModel (StateFlow) → RepositorioGalpones → { API (Retrofit + JSON), DataStore }
                                                ↑
                         AlertasWorker (WorkManager, cada 15 min)
```

- `data/api/MonitorApi.kt` — endpoints de la API (Retrofit): `GET galpones`,
  `GET galpones/{id}/mediciones`, `GET alertas/activas`, `POST simulaciones/pico-calor`.
- `mock-api/` — la API simulada del sistema de monitoreo (punto 3.4 del caso), servida con
  **json-server** en el PC. La app la consume por HTTP con Retrofit; la URL viene de
  `BuildConfig.API_URL_BASE` (propiedad Gradle `caso1.apiUrl`). Ver `mock-api/README.md`.
- `data/api/InterceptorConectividad.kt` — corta la petición sin red o con el modo demo
  "sin conexión", para que la app use los datos guardados en DataStore.
- `data/api/ServidorSimulado.kt` — alternativa sin PC: un interceptor de OkHttp que responde el
  mismo JSON dentro de la app. Se activa compilando con `-Pcaso1.apiSimulada=true`.
- `data/MonitorConectividad.kt` y `data/Sincronizador.kt` — detectan la red del teléfono y
  sincronizan la API con DataStore (al abrir, al deslizar, al volver la conexión y cada 15 min).
- `data/SimuladorAutomatico.kt` — respuesta 10 del docente: con la sesión iniciada, cada minuto
  agrega una medición ficticia por galpón (temperatura y humedad) y la evalúa en el dispositivo.
  Como WorkManager no puede despertar cada minuto, el ciclo vive mientras el proceso está abierto.
  `data/model/SimulacionAmbiental.kt` genera los valores de forma determinista (misma entrada, misma
  salida), así la demo es reproducible.
- `viewmodel/PeriodoGrafico.kt` — respuesta 11: filtra las mediciones a 6 h, 24 h o todo; el detalle
  grafica temperatura y humedad del período elegido.
- `data/api/MockMonitorApi.kt` — datos del servidor simulado, deterministas: estado, alertas e
  historial siempre coinciden. Ciclo suave de 24 h con una medición cada 15 min.
- `data/local/` — caché local en DataStore tipado: `monitoreo.json` (JSON con kotlinx.serialization)
  guarda galpones, mediciones (últimas 24 h), alertas y eventos. Cada sincronización se escribe en
  un solo `updateData`, atómico: o se guarda todo o nada. La sesión usa Preferences DataStore
  (`sesion`, Guía 12). Room se quitó porque la rúbrica del curso evalúa DataStore.
- `data/repository/GalponRepository.kt` — sincroniza la API con DataStore; una alerta
  activa por galpón, sin duplicados; confirmación de alertas.
- `AlertaTemperaturasApp.kt` — inyección de dependencias manual: `ContenedorApp`
  crea una sola vez el repositorio y la sesión; cada ViewModel los recibe por
  constructor mediante su `Factory`.
- `data/repository/RepositorioGalpones.kt` — interfaz del repositorio; los
  ViewModels dependen de ella y en los tests se usa una versión falsa en memoria.
- `viewmodel/` — un ViewModel por pantalla; `SessionViewModel` para la sesión.
  `AlertasViewModel` entrega `AlertaUi` (modelo de pantalla) en vez del registro guardado.
- `res/values/strings.xml` — todos los textos de la app, con plurales.
- `ui/screen/` — pantallas Compose; `ui/navigation/` — Navigation Compose.
- `work/AlertasWorker.kt` — revisión en segundo plano y notificaciones.
- `notifications/NotificationHelper.kt` — canal y notificación de alertas críticas.

**Alertas:** *confirmar* significa acusar recibo (queda quién y cuándo, y una
entrada en el historial). La alerta sigue activa hasta que el galpón vuelve a la
normalidad; si empeora (advertencia → crítico) se genera una alerta nueva.

**Historial:** cada alerta guarda la medición que la produjo (temperatura y humedad)
y su estado de atención (*Pendiente*, *Atendida* con quién y cuándo, o *Resuelta*).
Se filtra por galpón, tipo de alerta y rango de fecha (*Hoy*, *Últimos 7 días*, *Todo*).
Cada acción registra el rol de quien la hizo, el galpón, la fecha y la hora; supervisor
y jefatura consultan las acciones de los operarios (respuestas 6, 12 y 14 del docente).

## Cómo ejecutar

1. Levantar la API simulada (requiere Node.js 18+):
   ```bash
   cd mock-api && npm install && npm start
   npm run android   # en otra terminal: adb reverse tcp:3000 tcp:3000
   ```
2. Abrir el proyecto en Android Studio (Gradle descarga el JDK 25 configurado; minSdk 24, targetSdk 37).
3. Ejecutar la configuración `app` en un emulador o dispositivo.
4. Aceptar el permiso de notificaciones (Android 13+).

Sin el PC disponible, compilar con el servidor simulado interno:
`./gradlew installDebug -Pcaso1.apiSimulada=true`.

Tests unitarios (umbrales, coherencia del mock, validación del formulario, `RegistroAccionViewModel` con `runTest` y un repositorio falso, `GalponRepository` sobre un archivo DataStore real: sin red, error 500 o servidor apagado se siguen entregando los datos guardados; reglas de alertas, retención, mediciones automáticas y persistencia al reiniciar):

```bash
./gradlew testDebugUnitTest
cd mock-api && npm test   # endpoints y umbrales de la API simulada
```

## Guion de demo

Las opciones *Simular pico de calor* y *Simular sin conexión* del menú ⋮ solo aparecen en la versión **debug** (la que instala Android Studio). *Simular medición* está en todas las versiones y perfiles.

0. **Simular medición** (respuesta 5 del docente): menú ⋮ → *Simular medición* → elegir galpón,
   escribir 35 °C y 60 % (la vista previa dice cómo quedaría) → *Simular medición*. El galpón pasa a
   alerta roja y llega la notificación (operario o supervisor). Funciona también sin conexión.
   Con la sesión iniciada, además, cada minuto se agrega una lectura automática por galpón; en el
   detalle se grafican temperatura y humedad con el período *6 h*, *24 h* o *Todo*.

1. Entrar como **Operario** → menú ⋮ → *Simular pico de calor (demo)* → botón Inicio del teléfono.
2. A los 10 s llega la notificación → tocarla abre directo **Alertas** → **Confirmar**.
   La tarjeta dice qué hacer y ofrece *Registrar acción correctiva* con el galpón ya elegido.
   Con **Jefatura** no llegan notificaciones: las alertas se consultan en el panel.
3. Botón **Registrar acción** (formulario validado: galpón, tipo y descripción) → confirmación.
   "Atrás" desde la confirmación vuelve a Galpones, sin poder guardar la acción dos veces.
4. Menú ⋮ → *Cerrar sesión* → **Supervisor** → pestaña **Historial**: alerta confirmada y acciones.
5. **Jefatura** → resumen por estado y alertas del día.
6. **Sin conexión**: activar el modo avión (o menú ⋮ → *Simular sin conexión*): aparece el aviso
   "Sin conexión · mostrando datos guardados de las …" y la app sigue funcionando.
   Al volver la red se actualiza sola.
7. Girar el dispositivo o usar una tablet: 2 columnas / lista + detalle con riel lateral.

> En el emulador con Android 16 puede aparecer el aviso "Android App Compatibility (16 KB)":
> es por librerías nativas de Google (DataStore y Compose), no por la app. Tocar
> *Don't Show Again* antes de la presentación.

> El modo demo guarda el pico de temperatura en memoria: si Android cierra el
> proceso antes de los 10 s, la notificación no llega. La revisión periódica
> puede retrasarse por las optimizaciones de batería de Android (Doze).

## Diseño

Estilo inspirado en los avisos oficiales de alerta de Chile: el estado de cada
galpón se muestra como una franja de color pleno (verde normal, alerta amarilla,
alerta roja), con tipografía Barlow (licencia OFL, `licenses/Barlow-OFL.txt`) e
íconos Material Symbols. Modo claro y oscuro. Detalles en [DESIGN.md](DESIGN.md).

## Documentación

- [Plan de desarrollo](docs/plan-de-desarrollo.md)
- [Producto](PRODUCT.md) · [Sistema de diseño](DESIGN.md)

## Licencia

MIT — uso libre con atribución. Trabajo universitario; no usar para fines comerciales sin autorización.
