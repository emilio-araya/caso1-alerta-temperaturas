---
name: Alerta Temperaturas
description: Alerta temprana por calor en galpones avícolas, con la gramática de los avisos oficiales chilenos.
colors:
  Marino: "#1B3A5C"
  MarinoProfundo: "#0B1F36"
  TintaClara: "#13233A"
  FondoClaro: "#F3F5F8"
  TintaOscura: "#E6ECF3"
  FondoOscuro: "#0C141E"
  AlertaVerde: "#1E7A3C"
  AlertaAmarilla: "#F2C200"
  AlertaRoja: "#C8102E"
  TintaSobreAmarillo: "#1A1A1A"
  TextoVerdeClaro: "#1E7A3C"
  TextoAmarilloClaro: "#7A5D00"
  TextoRojoClaro: "#B00E28"
  TextoVerdeOscuro: "#5BC981"
  TextoAmarilloOscuro: "#F2C200"
  TextoRojoOscuro: "#FF7083"
  primary-container: "#D6E3F3"
  secondary: "#4A5D73"
  secondary-container: "#E0E7F0"
  on-surface-variant: "#4F5E70"
  surface-container-lowest: "#FFFFFF"
  outline: "#8593A4"
  outline-variant: "#D5DCE4"
  error-container: "#FCDDE1"
  dark-primary: "#9CC2EE"
  dark-on-surface-variant: "#A9B6C6"
  dark-surface-container-lowest: "#09101A"
  dark-surface-container-low: "#131D2A"
  dark-outline-variant: "#2A394C"
typography:
  displayLarge:
    fontFamily: "Barlow Condensed"
    fontSize: "64sp"
    fontWeight: 700
    lineHeight: "68sp"
    letterSpacing: "-0.01em"
    fontFeature: "tnum"
  displaySmall:
    fontFamily: "Barlow Condensed"
    fontSize: "40sp"
    fontWeight: 700
    lineHeight: "44sp"
    letterSpacing: "-0.01em"
    fontFeature: "tnum"
  headlineMedium:
    fontFamily: "Barlow Condensed"
    fontSize: "28sp"
    fontWeight: 600
    lineHeight: "34sp"
    letterSpacing: "-0.01em"
    fontFeature: "tnum"
  titleLarge:
    fontFamily: "Barlow"
    fontSize: "22sp"
    fontWeight: 600
    lineHeight: "28sp"
  titleMedium:
    fontFamily: "Barlow"
    fontSize: "17sp"
    fontWeight: 600
    lineHeight: "24sp"
    letterSpacing: "0.005em"
  titleSmall:
    fontFamily: "Barlow"
    fontSize: "15sp"
    fontWeight: 600
    lineHeight: "20sp"
    letterSpacing: "0.005em"
  bodyLarge:
    fontFamily: "Barlow"
    fontSize: "17sp"
    fontWeight: 400
    lineHeight: "24sp"
  bodyMedium:
    fontFamily: "Barlow"
    fontSize: "15sp"
    fontWeight: 400
    lineHeight: "21sp"
  bodySmall:
    fontFamily: "Barlow"
    fontSize: "13sp"
    fontWeight: 400
    lineHeight: "18sp"
    letterSpacing: "0.01em"
  labelLarge:
    fontFamily: "Barlow"
    fontSize: "15sp"
    fontWeight: 600
    lineHeight: "20sp"
    letterSpacing: "0.01em"
  labelMedium:
    fontFamily: "Barlow"
    fontSize: "13sp"
    fontWeight: 600
    lineHeight: "16sp"
    letterSpacing: "0.02em"
  labelSmall:
    fontFamily: "Barlow"
    fontSize: "12sp"
    fontWeight: 500
    lineHeight: "16sp"
    letterSpacing: "0.03em"
  EstiloEstado:
    fontFamily: "Barlow Condensed"
    fontSize: "15sp"
    fontWeight: 700
    lineHeight: "18sp"
    letterSpacing: "0.06em"
  EstiloCifra:
    fontFamily: "Barlow"
    fontSize: "15sp"
    fontWeight: 500
    lineHeight: "20sp"
    fontFeature: "tnum"
rounded:
  extraSmall: "4dp"
  small: "8dp"
  medium: "12dp"
  large: "16dp"
  extraLarge: "24dp"
  full: "50%"
spacing:
  xxs: "4dp"
  xs: "6dp"
  sm: "8dp"
  md: "12dp"
  lg: "16dp"
  xl: "24dp"
  xxl: "32dp"
  xxxl: "48dp"
components:
  franja-estado-normal:
    backgroundColor: "{colors.AlertaVerde}"
    textColor: "#FFFFFF"
    typography: "{typography.EstiloEstado}"
    padding: "7dp 16dp"
  franja-estado-advertencia:
    backgroundColor: "{colors.AlertaAmarilla}"
    textColor: "{colors.TintaSobreAmarillo}"
    typography: "{typography.EstiloEstado}"
    padding: "7dp 16dp"
  franja-estado-critico:
    backgroundColor: "{colors.AlertaRoja}"
    textColor: "#FFFFFF"
    typography: "{typography.EstiloEstado}"
    padding: "7dp 16dp"
  boletin-critico:
    backgroundColor: "{colors.AlertaRoja}"
    textColor: "#FFFFFF"
    padding: "16dp"
  boton-boletin:
    textColor: "#FFFFFF"
    rounded: "{rounded.extraSmall}"
    typography: "{typography.labelLarge}"
  tarjeta:
    backgroundColor: "{colors.surface-container-lowest}"
    rounded: "{rounded.medium}"
    padding: "16dp"
  button-primary:
    backgroundColor: "{colors.Marino}"
    textColor: "#FFFFFF"
    typography: "{typography.labelLarge}"
    rounded: "{rounded.full}"
    height: "52dp"
  fab-registrar:
    backgroundColor: "{colors.Marino}"
    textColor: "#FFFFFF"
    rounded: "{rounded.large}"
  filter-chip:
    rounded: "{rounded.small}"
    typography: "{typography.labelLarge}"
  text-field:
    rounded: "{rounded.extraSmall}"
    typography: "{typography.bodyLarge}"
  navigation-bar:
    backgroundColor: "{colors.surface-container-lowest}"
    textColor: "{colors.on-surface-variant}"
---

# Design System: Alerta Temperaturas

## Overview

**Creative North Star: "El aviso oficial"**

La app habla con la gramática de las alertas públicas chilenas: normal verde, alerta amarilla, alerta roja. El estado de un galpón no es un punto pequeño ni un acento en una tarjeta gris; es una franja plana de color pleno, a todo el ancho, con el nombre del estado en mayúsculas condensadas, como el encabezado de un boletín. Todo lo demás (fondos, tinta, controles) se mantiene en un registro institucional neutro, azul marino sobre gris frío, para que el color de estado sea lo único que grita.

La densidad es de herramienta de terreno: cifras grandes condensadas que se leen a distancia (la demo se proyecta en sala) y a pleno sol, textos en Barlow con ADN de señalética, y una escala térmica idéntica en todas las tarjetas para comparar galpones sin leer números. Hay modo claro y oscuro (azul noche); los colores de estado son los mismos en ambos y nunca dependen del color dinámico del teléfono.

**Key Characteristics:**
- Franjas de estado planas, a todo el ancho, en verde #1E7A3C, amarillo #F2C200 (tinta negra) y rojo #C8102E (tinta blanca).
- Tinta y marca en azul marino institucional; fondos gris frío; tarjetas blancas con borde fino.
- Barlow para texto, Barlow Condensed en mayúsculas para estados y en cifras grandes con números tabulares.
- Plano: profundidad por tono y borde, sin sombras en tarjetas.
- Un solo movimiento: el cambio de color del boletín del inicio.

## Colors

Neutros fríos e institucionales con tres colores de estado de significado fijo; el color de estado es la única voz fuerte de la pantalla.

### Primary
- **Azul marino institucional** (`Marino`): `primary` en tema claro. Botones llenos, FAB "Registrar acción", línea del gráfico, borde de la tarjeta seleccionada, encabezado del login y fondo del ícono de la app. En el login se usa en ambos temas porque es identidad, no superficie.
- **Marino profundo** (`MarinoProfundo`): texto sobre `primaryContainer` (#D6E3F3), p. ej. los íconos de perfil del login.
- **Azul cielo nocturno** (`dark-primary`, #9CC2EE): `primary` en tema oscuro.

### Secondary
- **Pizarra** (`secondary`, #4A5D73, también `tertiary`): controles secundarios; `secondaryContainer` (#E0E7F0) es el círculo de ícono de las acciones en el historial.

### Estados (escala oficial de alerta)
Se entregan con `LocalPaletaEstados` como `ColoresEstado(franja, sobreFranja, texto)`:
- **Verde normal** (`AlertaVerde`): franja normal, tinta blanca.
- **Amarillo alerta** (`AlertaAmarilla`): franja de alerta amarilla, siempre con `TintaSobreAmarillo` (#1A1A1A).
- **Rojo alerta** (`AlertaRoja`): franja de alerta roja con tinta blanca; también `error` en tema claro.
- **Versiones para texto sobre superficie**: en claro `TextoVerdeClaro`, `TextoAmarilloClaro` (#7A5D00, porque el amarillo puro no se lee sobre blanco), `TextoRojoClaro`; en oscuro `TextoVerdeOscuro`, `TextoAmarilloOscuro`, `TextoRojoOscuro`.

### Neutral
- **Fondo gris frío** (`FondoClaro`) con **tinta azul tinta** (`TintaClara`): `background`/`surface` y `onSurface` del tema claro.
- **Blanco tarjeta** (`surface-container-lowest`): tarjetas, barra de navegación y riel.
- **Texto atenuado** (`on-surface-variant`, #4F5E70): subtítulos, granja, ejes, etiquetas de tabla.
- **Borde fino** (`outline-variant`, #D5DCE4): borde de tarjetas, divisores, separador lista/detalle.
- **Noche** (`FondoOscuro`, `TintaOscura`, `dark-surface-container-*`, `dark-outline-variant`): los mismos roles en el tema oscuro.

### Named Rules
**The Significado Fijo Rule.** Verde, amarillo y rojo significan normal, alerta amarilla y alerta roja, y nada más. El tema no usa color dinámico y ningún control, decoración o acento puede tomar estos colores.

**The Tinta de la Franja Rule.** Sobre el amarillo siempre va tinta negra (#1A1A1A); sobre verde y rojo, blanca. Para texto de estado sobre una superficie se usa `ColoresEstado.texto`, nunca `franja`.

**The Cifra Que Delata Rule.** Cada cifra se colorea con su propio umbral (`colorCifra(estadoTemperatura(...))`, `colorCifra(estadoHumedad(...))`): normal queda en tinta, fuera de rango toma el color de texto del estado. Así se ve si el problema es el calor o la humedad.

## Typography

**Display Font:** Barlow Condensed (SemiBold 600, Bold 700)
**Body Font:** Barlow (Regular, Medium, SemiBold, Bold)

**Character:** Barlow viene de la señalética vial pública: clara a distancia y a pleno sol. Su versión condensada da cifras grandes que caben en una tarjeta de teléfono y nombres de estado en mayúsculas con voz de aviso oficial. Licencia OFL (`licenses/Barlow-OFL.txt`).

### Hierarchy
- **Display** (Barlow Condensed 700, 64/52/40 sp, `tnum`): cifras grandes. `displayLarge` en la placa de estado del detalle; `displaySmall` en la temperatura de cada tarjeta, los conteos del resumen de jefatura y el nombre de la app en el login.
- **Headline** (Barlow Condensed 600, 32/28/24 sp): unidades junto a la cifra grande (" °C" en `headlineMedium`).
- **Title** (Barlow 600, 22/17/15 sp): `titleLarge` en barras superiores y títulos de sección; `titleMedium` en nombre de galpón y frase principal del boletín; `titleSmall` en el encabezado de la lista ("4 galpones · más graves primero").
- **Body** (Barlow 400, 17/15/13 sp): mensajes, instrucciones del boletín, granja (`bodySmall`), errores de formulario.
- **Label** (Barlow 500–600, 15/13/12 sp, tracking 0.01–0.03 em): botones, perfil bajo el título, hora en la franja, ejes de gráficos (`labelSmall`).
- **EstiloEstado** (Barlow Condensed 700, 15/18 sp, 0.06 em, siempre en mayúsculas): el nombre del estado en franjas ("ALERTA ROJA"); en el boletín se agranda al tamaño de `titleLarge`.
- **EstiloCifra** (Barlow 500, 15/20 sp, `tnum`): cifras en tablas y listas para que las columnas alineen.

### Named Rules
**The Mayúscula de Aviso Rule.** Solo el nombre del estado va en mayúsculas condensadas con tracking; el resto del texto va en tipo oración.

**The Cifra Tabular Rule.** Toda cifra que se compara (temperatura, humedad, horas, conteos) usa números tabulares y coma decimal chilena (`cifra()`: 34,2).

## Layout

Margen de pantalla de 16 dp; separación entre tarjetas de 12 dp (8 dp en las listas del historial); relleno interno de tarjeta 16 dp. Escala de espaciado observada: 4, 6, 8, 12, 16, 24, 32, 48 dp. Formularios y confirmación se limitan a 640 dp y 520 dp de ancho; el login a 560 dp.

Adaptación por clase de ancho (`WindowWidthSizeClass`):
- **Compact**: una columna de tarjetas; `NavigationBar` inferior.
- **Medium**: dos columnas de tarjetas; `NavigationBar` inferior.
- **Expanded**: `NavigationRail` a la izquierda; en Galpones, lista (42 %) y detalle (58 %) lado a lado separados por un divisor vertical; la tarjeta elegida lleva borde marino de 2 dp.

El boletín del inicio sale del relleno de la grilla (`sangrado(16.dp)`) para ir de borde a borde. Con el FAB del operario, la lista reserva 96 dp abajo. Supervisor y jefatura ven los galpones ordenados por gravedad; el operario, en orden fijo. El operario no ve el destino Historial.

## Elevation & Depth

Sistema plano. Las tarjetas no tienen sombra: se separan del fondo gris frío por el tono blanco (`surfaceContainerLowest`) y un borde de 1 dp `outlineVariant`. La barra superior usa el mismo color que la superficie; barra de navegación y riel son blancos sin elevación tonal. Los únicos elementos con sombra son los que Material 3 eleva por defecto: el FAB extendido, menús y diálogos.

### Named Rules
**The Plano Por Tono Rule.** La profundidad se expresa con tono y borde, no con sombra. Una tarjeta nueva es blanca con borde fino, nunca elevada.

## Shapes

Esquinas moderadas de Material 3 redefinidas en `Formas`: 4 / 8 / 12 / 16 / 24 dp. Las tarjetas usan 12 dp y recortan la franja de estado superior, que queda con esquinas redondeadas arriba y corte recto abajo. Las franjas y placas a todo el ancho (boletín, placa del detalle, encabezado del login) son rectángulos sin redondeo, como un aviso impreso. La `MarcaEstado` de listas y tablas es un cuadrado de 12 dp con 4 dp de esquina. Los círculos solo aparecen como fondo de ícono (perfil 48 dp, acción 40 dp). Botones llenos en píldora (forma por defecto de M3); el botón contorneado del boletín usa 4 dp para leerse como parte del aviso.

## Components

### Franja de estado (`FranjaEstado`)
La firma del sistema. Fila a todo el ancho con el color `franja` del estado, relleno 7 × 16 dp, nombre del estado en `EstiloEstado` mayúsculas a la izquierda y un dato (hora, "Desde las 08:45") en `labelMedium` a la derecha, ambos en `sobreFranja`. Encabeza cada tarjeta de galpón y de alerta.

### Boletín de estado (`BoletinEstado`)
Bloque de color pleno de borde a borde bajo la barra superior del inicio, con el color del peor galpón. Ícono 22 dp (check o advertencia) y estado en `EstiloEstado` a tamaño `titleLarge`; luego galpón más grave con su temperatura (`titleMedium`), cuántos más están fuera de rango, la instrucción ("Revisa el galpón ahora y confirma la alerta.") y la hora de actualización. Fuera de normal ofrece una sola acción: "Ver alertas", botón contorneado de 1,5 dp en la tinta de la franja, esquina 4 dp.
- **Movimiento:** es el único elemento animado de la app. Fondo y tinta cambian con `animateColorAsState`, `tween(450)` (easing estándar), cuando cambia el peor estado.

### Tarjeta de galpón (`TarjetaGalpon`)
Tarjeta blanca de 12 dp con borde 1 dp `outlineVariant` (2 dp `primary` si está seleccionada en Expanded). Franja de estado arriba; nombre (`titleMedium`), granja (`bodySmall`) y humedad con ícono de gota (`EstiloCifra`, coloreada por su umbral) a la izquierda; temperatura en `displaySmall` coloreada por su umbral a la derecha; escala térmica abajo.

### Escala térmica (`EscalaTermica`)
Barra de 6 dp, de 14 a 38 °C, igual en todas las tarjetas: amarillo bajo 18 °C, verde 18–28, amarillo 28–32, rojo sobre 32, con cortes de 2 dp en el color de la tarjeta y etiquetas 18°/28°/32° en `labelSmall`. La lectura actual se marca con un triángulo de 5 dp y una línea de 3 px en `onSurface`. Lleva `contentDescription` con la cifra.

### Placa de estado (detalle)
Bloque de color pleno a todo el ancho: estado y causa en `EstiloEstado`, cifra en `displayLarge` con " °C" en `headlineMedium`, humedad en `titleMedium` y hora de lectura. Debajo, gráfico de línea de 190 dp: línea `primary` de 2,5 dp, zona amarilla (alfa 0,14) entre 28 y 32 °C, zona roja (alfa 0,10) sobre 32 °C, umbrales punteados y último punto con el color de su estado.

### Buttons
- **Primario:** `Button` M3 lleno en `primary`, píldora; en formularios a todo el ancho y 52 dp de alto ("Registrar acción", con indicador circular de 20 dp mientras guarda; "Volver a galpones"). "Confirmar" en las alertas lleva ícono de 18 dp.
- **FAB extendido:** "Registrar acción", solo operario, `primary`/`onPrimary`, 16 dp.
- **Tonal:** `FilledTonalButton` a todo el ancho para "Registrar acción" en el detalle.
- **Texto:** `TextButton` en diálogos (confirmar/cancelar).

### Chips
`FilterChip` M3 (8 dp) para filtrar el historial por galpón (fila desplazable) y elegir el tipo de acción en el registro (con check de 18 dp al seleccionar).

### Cards / Containers
- **Corner Style:** 12 dp.
- **Background:** `surfaceContainerLowest`.
- **Shadow Strategy:** ninguna (ver Elevation & Depth).
- **Border:** 1 dp `outlineVariant`.
- **Internal Padding:** 16 dp; listas internas separadas por `HorizontalDivider` `outlineVariant`.

### Inputs / Fields
`OutlinedTextField` M3 (4 dp). El error se muestra bajo el campo como fila con ícono de advertencia de 16 dp y texto `bodySmall` en `error`. La selección de galpón del registro es una lista de radios dentro de una tarjeta, con `MarcaEstado` y la cifra coloreada por estado.

### Navigation
- **Destinos:** Galpones (termostato), Alertas (campana, con `Badge` del número de alertas pendientes), Historial (reloj; solo supervisor y jefatura).
- **Barra superior:** título `titleLarge` y perfil en `labelMedium` atenuado debajo; menú de desbordamiento con "Simular pico de calor (demo)" y "Cerrar sesión". Pantallas secundarias usan `BarraSecundaria` con flecha de volver.
- **Inferior / riel:** `NavigationBar` en Compact y Medium, `NavigationRail` en Expanded, ambos en `surfaceContainerLowest` con el indicador de selección M3.
- **Historial:** `PrimaryTabRow` ("Alertas (n)" / "Acciones (n)") sobre la fila de chips.

### Login
Encabezado marino a todo el ancho con ícono termostato 40 dp, "Alerta Temperaturas" en `displaySmall` y la bajada en `bodyLarge`; debajo, una cinta de 6 dp con los tres colores de estado en tercios. Luego una tarjeta contorneada por perfil (círculo `primaryContainer` 48 dp con ícono, nombre, descripción y chevron).

### Estado vacío y confirmación
`EstadoVacio`: ícono 56 dp, título `titleLarge`, una línea `bodyMedium` atenuada, centrado. La confirmación de acción usa un check verde de 64 dp y una tarjeta de resumen con etiquetas en columna de 110 dp.

### Iconografía
Material Symbols Outlined como vector drawables (`ic_*`, Apache 2.0), usados con el composable `Icono`. El ícono de la app es un termostato blanco sobre marino con la cinta verde-amarillo-rojo.

## Do's and Don'ts

### Do:
- **Do** comunicar el estado con una franja plana de color pleno a todo el ancho y su nombre en `EstiloEstado` mayúsculas ("ALERTA ROJA", "ALERTA AMARILLA", "NORMAL").
- **Do** obtener los colores de estado de `LocalPaletaEstados` (`estado.colores()`), usando `franja`/`sobreFranja` para bloques y `texto` para texto sobre superficie.
- **Do** poner tinta #1A1A1A sobre el amarillo y blanco sobre verde y rojo.
- **Do** colorear cada cifra con su propio umbral mediante `colorCifra`.
- **Do** usar la misma `EscalaTermica` (14–38 °C) en toda tarjeta que compare galpones.
- **Do** mantener tarjetas blancas de 12 dp con borde 1 dp `outlineVariant` y sin sombra.
- **Do** usar números tabulares y coma decimal (`cifra()`) en toda cifra.
- **Do** adaptar la navegación por clase de ancho: barra inferior en Compact/Medium, riel y lista + detalle en Expanded.
- **Do** limitar cada pantalla a un solo bloque grande de color pleno (boletín en el inicio, placa en el detalle).

### Don't:
- **Don't** activar color dinámico ni reemplazar los colores de estado por tonos del tema.
- **Don't** usar verde, amarillo o rojo como acento decorativo, en botones o en controles.
- **Don't** representar el estado con un punto pequeño o solo con una cifra; la franja o la `MarcaEstado` siempre acompañan.
- **Don't** escribir texto en `AlertaAmarilla` sobre fondo claro; usar `TextoAmarilloClaro`.
- **Don't** agregar sombras a tarjetas ni animaciones fuera del cambio de color del boletín.
- **Don't** usar íconos que no sean Material Symbols Outlined, ni caracteres como íconos.
