# Guion de defensa (6–7 minutos)

Recorrido pensado para tocar los indicadores de EP2 y EP3 sin improvisar. Cada paso tiene **qué decís** y **qué hacés**; al final está el mapa indicador → paso, las preguntas probables y un plan B.

## Antes de entrar

```bash
cd mock-api && npm start                 # API simulada en http://localhost:3000/api/v1
adb reverse tcp:3000 tcp:3000            # el emulador llega al localhost del equipo
./gradlew installDebug
adb shell pm clear com.example.caso1     # arranca limpio: sin sesión, sin datos, umbrales por defecto
adb shell am start -n com.example.caso1/.MainActivity
```

Dejá el emulador con la cámara virtual configurada y el teléfono sin la bandeja de notificaciones abierta.

## 0:00–0:30 · Encuadre

**Decís:** "Es una app de monitoreo de galpones con alerta temprana por calor. Tres perfiles: Operario, Supervisor y Jefatura. Consume una API simulada con Retrofit y **toda la persistencia es local con DataStore**: si se cae la red, la app sigue mostrando y registrando datos. La regla de estados y umbrales se evalúa **en el dispositivo**, no en el servidor."

**Hacés:** mostrás la pantalla de login ("¿Con qué perfil ingresas?") y las tres tarjetas con su descripción.

## 0:30–1:20 · Validación en vivo

**Hacés:** entrás como **Operario** → FAB "Registrar acción" → tocás "Registrar acción" sin completar nada. Aparecen los tres errores con ícono de advertencia: *"Elige el galpón donde hiciste la acción"*, *"Elige el tipo de acción"*, *"Describe qué hiciste (mínimo 5 caracteres)"*. Escribís 3 letras → el error sigue. Completás → desaparece.

**Decís:** "Las reglas son funciones puras en `viewmodel/`: `validarRegistro`, `validarUmbrales` y `validarSimulacion`. No conocen Compose ni Android, se testean solas, y la pantalla únicamente dibuja el mapa de errores que devuelve el ViewModel. Por eso el error se corrige mientras escribís."

## 1:20–2:10 · Persistencia (cerrar y reabrir)

**Hacés:** volvés al inicio, `adb shell am force-stop com.example.caso1`, reabrís. La sesión sigue iniciada, los galpones y el historial siguen ahí.

**Decís:** "Hay tres almacenes: la sesión y los umbrales en Preferences DataStore, y el monitoreo en un DataStore **tipado** con un documento JSON (`files/datastore/monitoreo.json`). Cada escritura es un `updateData` atómico: o se guarda todo o no se guarda nada."

**Opcional (evidencia fuerte):**

```bash
adb shell "run-as com.example.caso1 find files -type f"
```

## 2:10–3:00 · La regla vive en el teléfono

**Hacés:** ⋮ → *Configurar umbrales* → cambiás **Máximo de advertencia** de 32 a **40** → *Guardar umbrales* → volvés. La alerta crítica por temperatura desaparece y el galpón queda en advertencia **sin ninguna llamada al servidor**. Después *Restaurar valores predeterminados*.

**Decís:** "El servidor entrega mediciones; el estado y las alertas los calculo acá con los umbrales guardados. Eso permite configurarlos, recalcular al instante y funcionar sin conexión. Fijate que el galpón que queda en amarillo a 25 °C es por **humedad**, no por temperatura: son dos reglas independientes."

## 3:00–4:00 · Simulación y notificación

**Hacés:** ⋮ → *Simular medición* → elegís un galpón → 35 °C y 60 % → mostrás la vista previa *"Con estos valores el galpón quedaría: …"* → *Simular medición*. La tarjeta pasa a rojo, aparece el snackbar y baja la notificación.

**Decís:** "La medición se guarda en el teléfono y se evalúa con las mismas reglas, así que la demo funciona sin red. Solo se notifica cuando aparece una alerta **crítica nueva**, y solo a Operario y Supervisor: Jefatura consulta. El canal es de importancia alta para que aparezca como aviso."

## 4:00–5:10 · Cámara como evidencia

**Hacés:** *Registrar acción* → completás galpón, tipo y descripción → *Tomar foto* → aparece el permiso → aceptás → foto → vista previa → *Registrar acción*. Después Historial → **Acciones**: la tarjeta muestra la miniatura y *"Operario · granja · galpón · hora"*.

**Decís:** "Segundo recurso nativo. Uso la cámara del sistema con `TakePicture` y un `FileProvider`: la app de cámara no puede escribir en el almacenamiento privado, así que le doy una URI con permiso temporal para ese archivo. El permiso `CAMERA` se pide en runtime; si se niega, la foto es opcional y la acción se guarda igual, con un mensaje que lo explica. La ruta queda en DataStore y el archivo en `files/evidencias/`."

**Opcional:**

```bash
adb shell "run-as com.example.caso1 grep -o '\"fotoRuta\":\"[^\"]*\"' files/datastore/monitoreo.json"
```

## 5:10–6:00 · Historial y filtros

**Hacés:** Historial → filtrás por galpón, por tipo y por **Hoy / Últimos 7 días / Todo**. Mostrás una alerta con la medición que la disparó (temperatura y humedad), su estado *Pendiente / Atendida* y el usuario en cada acción.

**Decís:** "El historial no es un log suelto: cada alerta guarda la medición que la provocó. *Hoy* usa el día del calendario local, no las últimas 24 horas. Hay retención de 24 horas de mediciones para que el documento no crezca sin límite."

## 6:00–6:30 · Cierre técnico

**Decís:** "Capas: `data/api` con Retrofit y DTOs, `data/repository` con las reglas, `data/local` con DataStore, `viewmodel` con `StateFlow` y `ui` en Compose. Todo lo que escribe pasa por un `Sincronizador` con `Mutex`, así el sync, la simulación y el lote automático no se pisan. Son **122 tests unitarios** y verificación independiente sobre el diff completo."

## Mapa indicador → paso

| Indicador | Dónde se ve |
|---|---|
| EP2 2.1.1 / 2.1.2 interfaz y navegación | Recorrido completo, barra inferior y detalle |
| EP2 2.1.2 formularios con validación visual | Paso 0:30 |
| EP2 2.2.1 / EP3 2.2.1 validación centralizada y modularidad | Paso 0:30 y cierre |
| EP2 2.2.2 animaciones | Franja de estado que cambia de color al simular (3:00) |
| EP2 2.3.1 / EP3 2.3.1 persistencia local | Paso 1:20 |
| EP3 2.2.2 estado y respuesta dinámica | Pasos 2:10 y 3:00 |
| EP3 2.3.2 arquitectura y decisiones | Pasos 2:10 y cierre |
| EP2 2.4.1 / EP3 2.4.1 / 2.4.2 recursos nativos | Pasos 3:00 (notificación) y 4:00 (cámara) |
| EP2 2.3.2 trabajo colaborativo | **No aplica**: proyecto individual. Decilo sin vueltas y mostrá el historial de commits como evidencia de uso de Git |

## Preguntas probables

- **¿Por qué DataStore y no Room?** El modelo cabe en un documento y la escritura atómica resuelve la consistencia. Room agrega SQL, esquema y migraciones que acá no aportan; el propio encargo dejó la persistencia avanzada fuera de la rúbrica. Costo honesto: con muchas tablas o consultas complejas, Room sería mejor.
- **¿Por qué un solo documento?** Porque la alerta, la medición y el evento se crean juntos; un `updateData` deja un estado consistente. Se reescribe completo, pero la retención de 24 h lo mantiene acotado.
- **¿Y si dos cosas escriben a la vez?** Todas las escrituras pasan por `Sincronizador` con `Mutex` y usan `updateData`; no hay lectura-modificación-escritura perdida.
- **¿Qué pasa sin internet?** La UI lee siempre de DataStore. Si el sync falla, se mantienen los datos y la hora de la última lectura; simulación y acciones funcionan offline.
- **¿Por qué la regla de umbrales no está en el servidor?** Porque los umbrales son configurables y la app debe funcionar sin conexión. El backend además es simulado.
- **¿Por qué no WorkManager para la lectura por minuto?** WorkManager tiene un mínimo de 15 minutos en modo periódico. El simulador automático corre en un scope de la `Application` mientras la app está viva, y la pantalla lo avisa; WorkManager queda para la sincronización.
- **¿Cómo evitás alertas duplicadas?** Una alerta activa por galpón, tipo y nivel. Solo se notifica la crítica nueva. Confirmar deja registro pero no cierra la alerta: se cierra cuando el galpón vuelve a rango normal.
- **¿Y si el archivo se corrompe?** `ReplaceFileCorruptionHandler` lo reemplaza por un documento vacío y el próximo sync lo repuebla; hay un test que lo cubre.
- **¿Cómo funciona la foto?** `TakePicture` + `FileProvider` con permiso temporal para una URI; `CAMERA` en runtime; se guarda la ruta, no la imagen, dentro del documento.
- **Limitaciones que reconozco:** retención de 24 h (por eso "Todo" en los gráficos equivale a 24 h), no uso `java.time` porque minSdk 24 no lo trae sin desugaring, la simulación se reemplaza con la próxima lectura real y la foto no se comprime.

## Plan B

- **No arranca la API:** seguí igual. La app muestra lo guardado y el aviso de sin conexión; explicá que es el comportamiento esperado, no una falla.
- **No aparece la notificación:** bajá la bandeja. Si tampoco está, `adb shell dumpsys notification --noredact | grep caso1` lo muestra, y el diálogo de confirmación ya prueba el flujo.
- **La cámara no abre o el emulador no tiene escena:** explicá el flujo con el mensaje de permiso denegado y el `FileProvider`; la acción se registra sin foto, que es justamente el caso opcional.
- **Captura en negro:** es el renderer del emulador; repetí la captura.

## Evidencia a mano

- `docs/rubrica.md` — indicador por indicador, dónde está y cómo mostrarlo.
- `docs/preguntas-respuestas-caso.md` — las 14 respuestas del docente y dónde se cumplen.
- `docs/plan-de-desarrollo.md` — capas y decisiones.
