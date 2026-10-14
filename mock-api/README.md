# API simulada del sistema de monitoreo (json-server)

Servidor REST con datos ficticios para el MVP (punto 3.4 del caso: "API y datos simulados").
La app lo consume con Retrofit igual que consumiría el sistema de monitoreo real.

## Uso

Requiere Node.js 18 o superior.

```bash
cd mock-api
npm install
npm start          # API en http://localhost:3000/api/v1/
npm run android    # en otra terminal, con el emulador o el celular (USB) conectado
```

`npm run android` ejecuta `adb reverse tcp:3000 tcp:3000`: el `localhost:3000` del teléfono
apunta al PC. Hay que repetirlo cada vez que se reconecta el dispositivo o se reinicia el emulador.

## Endpoints

| Método | Ruta | Respuesta |
|--------|------|-----------|
| GET | `/api/v1/galpones` | Galpones con su estado actual (`NORMAL`, `ADVERTENCIA`, `CRITICO`) |
| GET | `/api/v1/galpones/{id}/mediciones` | Últimas 24 mediciones del galpón, de la más reciente a la más antigua; 404 si no existe |
| GET | `/api/v1/alertas/activas` | Alertas de los galpones fuera de rango |
| POST | `/api/v1/simulaciones/pico-calor` | Demo: fuerza un pico de calor crítico; responde `{ "galponId": n }` |

## Cómo se generan los datos

`datos.js` genera las mediciones a partir de la hora actual (una cada 15 minutos, con un ciclo
diario y algo de ruido), así la app siempre muestra datos "recientes". Por eso no hay un `db.json`
fijo: un archivo estático quedaría con fechas viejas.

Los umbrales están duplicados de `Umbrales` en `app/src/main/java/com/example/caso1/data/model/Modelos.kt`.
Si cambian allí, hay que cambiarlos aquí también; `npm test` verifica los bordes.

## Otras formas de conectarse

| Situación | Cómo compilar la app |
|-----------|---------------------|
| Emulador o celular por USB (por defecto) | Sin cambios, más `npm run android` |
| Emulador sin `adb reverse` | `./gradlew installDebug -Pcaso1.apiUrl=http://10.0.2.2:3000/api/v1/` |
| Sin PC (servidor simulado dentro de la app) | `./gradlew installDebug -Pcaso1.apiSimulada=true` |

Para un celular por WiFi con la IP del PC, hay que agregar esa IP a
`app/src/main/res/xml/network_security_config.xml` (la app solo permite HTTP sin cifrar hacia
`localhost` y `10.0.2.2`).

## Tests

```bash
npm test
```
