# Product

<!-- impeccable:product-schema 1 -->

## Platform

android

## Users

- **Operarios** de galpones avícolas: consultan el estado de cada galpón, identifican alertas y registran o confirman las acciones que realizan. Usan la app con miradas rápidas mientras recorren los galpones.
- **Supervisores**: revisan varios galpones a la vez, consultan eventos críticos e históricos y verifican el estado de las alertas.
- **Jefaturas**: necesitan información resumida para el seguimiento y la toma de decisiones.

## Product Purpose

Alerta temprana por altas temperaturas en galpones de una empresa avícola y ganadera chilena. Reemplaza las rondas de inspección manuales por una app móvil que muestra temperatura, humedad y estado (normal / advertencia / crítico) de cada galpón, notifica los eventos críticos y guarda el historial. Éxito: acceso rápido al estado de los galpones, condiciones críticas visibles de inmediato, alertas oportunas, históricos consultables y menos tiempo para identificar y gestionar un evento.

## Positioning

Proyecto académico (DSY1105 · Desarrollo de Aplicaciones Móviles, Duoc UC, caso CITT Sede Melipilla). MVP con datos simulados; no se conecta a sistemas productivos reales.

## Operating Context

- Celulares Android, dentro de los galpones (luz artificial o escasa) y fuera de ellos a pleno sol, con la misma frecuencia: la app debe leerse bien en ambas condiciones.
- Conectividad limitada o intermitente: la información reciente queda guardada en el teléfono.
- La demo se presenta en un emulador de Android Studio proyectado en sala, por lo que el estado de cada galpón debe leerse a distancia.

## Capabilities and Constraints

- Login simulado por perfil (Operario, Supervisor, Jefatura); sesión recordada.
- Galpones con semáforo de estado y última temperatura/humedad; detalle con gráfico de temperatura y umbrales; alertas activas con confirmación (operario/supervisor); historial de alertas y acciones filtrable por galpón; registro de acciones con formulario validado; resumen para jefatura; notificaciones locales y revisión periódica en segundo plano; modo demo que simula un evento crítico.
- Umbrales: normal 18–28 °C y 50–70 % HR; advertencia 28–32 °C o 70–80 % HR (o bajo el mínimo); crítico > 32 °C o > 80 % HR.
- Stack: Kotlin, Jetpack Compose, Material 3, MVVM, Room, DataStore, WorkManager, Navigation Compose. minSdk 24.
- Terminología: galpón, granja, medición, alerta, acción, confirmar.

## Brand Commitments

- Nombre de la app: **Alerta Temperaturas** (se mantiene tal cual).
- Idioma: español de Chile.

## Evidence on Hand

- Caso del curso: `Caso 1. DSY1105 Aplicaciones Móviles - Alerta temprana por altas temperaturas.pdf`.
- Todos los datos (granjas, galpones, mediciones) son ficticios y generados por `MockMonitorApi`. No hay logo, nombre de empresa ni datos reales; no deben inventarse.

## Product Principles

1. El estado de un galpón se entiende en un vistazo, sin leer números.
2. Lo crítico nunca se pierde: la alerta lleva directo a la acción.
3. Funciona igual sin conexión.
4. Cada perfil ve lo que necesita para su tarea, nada más.
