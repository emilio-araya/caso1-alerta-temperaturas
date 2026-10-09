---
version: 1
slug: "com-example-caso1-ui-screen-homescreen-kt-a8557530"
primary_target: "app/src/main/java/com/example/caso1/ui/screen/HomeScreen.kt"
related_targets: []
---

Scope: app completa (Operate). Perfiles operario, supervisor, jefatura; uso dentro del galpón y a pleno sol; demo en emulador proyectado.

## Direction contract

THESIS: El estado de cada galpón se comunica con la gramática de las alertas oficiales chilenas (normal verde, alerta amarilla, alerta roja). Rechaza el dashboard gris de tarjetas con un acento y los semáforos de punto pequeño.

OWN-WORLD: Fondo claro neutro frío y tinta azul marino institucional; modo oscuro azul noche. Estados como franjas planas de color pleno a todo el ancho: verde #1E7A3C, amarillo #F2C200 con tinta negra, rojo #C8102E con tinta blanca. Barlow (DNA de señalética pública) para textos y Barlow Condensed para cifras y nombres de estado en mayúsculas. Íconos Material Symbols.

STORY: Un operario sabe en un segundo si algo arde, qué galpón es y qué hacer; el supervisor ve primero lo más grave; la jefatura lee el resumen.

FIRST VIEWPORT: Top bar con nombre de app y perfil. Debajo, el boletín: una franja de estado general a todo el ancho con el color del peor galpón y una sola próxima acción. Luego las tarjetas de galpón: franja superior con el estado, cifra condensada grande de temperatura, humedad y una escala térmica 14–38 °C a la misma escala en todas. Navigation bar (rail en expandido): Galpones, Alertas con badge, Historial. Operario: FAB extendido "Registrar acción".

FORM: Alerta SENAPRED, candidato 1 de mi lista (pick), seed fbc299f8. Code-led.

FINISH: unreviewed and undocumented is unfinished; this build ends with the finish review, the verdict, DESIGN.md, and every shipping raster carrying its provenance
