# Caso 1 — Preguntas y respuestas del docente

Respuestas oficiales a las consultas sobre el caso "Alerta temprana por altas temperaturas", transcritas tal cual.
La columna **En la app** de la tabla final indica cómo se aborda cada respuesta en el MVP.

## 1. Monitoreo de temperatura y humedad

**1. ¿Cuáles son los rangos de temperatura y humedad que definen los estados normal, advertencia y crítico en los galpones?**

Los rangos deben considerar la edad de las aves, la humedad relativa y las condiciones de crianza. No existe un único rango adecuado para todas las etapas productivas.
Para el MVP, se recomienda utilizar umbrales ficticios y configurables, definidos o validados por el docente, que permitan simular estados normales, de advertencia y críticos.

**2. ¿Qué medidas o acciones se deben tomar cuando la temperatura o la humedad alcanzan niveles de advertencia o críticos?**

En estado de advertencia, los operarios deberán revisar las condiciones del galpón y verificar los sistemas de ventilación.
En estado crítico, la aplicación deberá generar una alerta prioritaria, notificar al supervisor y permitir registrar acciones correctivas. Estas acciones serán simuladas y no implicarán controlar equipos reales.

**3. ¿En qué unidades y formato deben visualizarse los valores de temperatura y humedad (por ejemplo, °C y % de humedad relativa)?**

La temperatura se mostrará en grados Celsius (°C) y la humedad relativa en porcentaje (%).
Se recomienda incluir fecha, hora de medición e indicadores visuales que permitan identificar rápidamente el estado de cada galpón.

## 2. Alertas y notificaciones

**4. ¿Las alertas deben notificarse únicamente a un grupo designado de personas o equipos especializados, en lugar de notificarse a todos los usuarios?**

Sí. Se recomienda dirigir las notificaciones a los operarios y supervisores responsables de los galpones afectados.
Las jefaturas podrán consultar las alertas mediante un panel general. Los permisos y destinatarios definitivos serán establecidos según el alcance académico

**5. Dado que la aplicación utilizará datos ficticios y no estará conectada a sistemas reales, ¿debe incluir una funcionalidad que permita a los usuarios generar alertas manualmente para probar las alertas críticas y las notificaciones?**

Sí. Se recomienda incorporar un mecanismo de simulación que permita modificar valores ficticios de temperatura y humedad para provocar alertas.
Esto facilitará demostrar el funcionamiento de la aplicación sin necesidad de conectarse a sensores reales.

**6. ¿Los eventos críticos y las alertas deben contar con un historial independiente que permita consultarlos rápidamente?**

Sí. La aplicación debería mantener un historial de alertas que permita consultar el galpón afectado, fecha, hora, medición registrada, nivel de criticidad y estado de atención.
También se recomienda permitir filtros por fecha, galpón y tipo de alerta.

## 3. Administración y organización de los galpones

**7. ¿Los fundos pueden contar con varios sectores de galpones? En caso afirmativo, ¿pueden existir distintos tipos de productos en un mismo sector o en sectores diferentes que requieran condiciones y medidas específicas?**

Se recomienda trabajar inicialmente con producción de pollos de engorda, considerando que las condiciones ambientales pueden variar según la edad de las aves. La cantidad de fundos, sectores y galpones será definida por el docente.

**8. ¿La aplicación debe ofrecer un medio de contacto rápido con los supervisores o responsables de la administración de los galpones?**

Se recomienda incorporar información de contacto ficticia de los supervisores responsables, permitiendo identificar rápidamente a quién corresponde atender una alerta.
La incorporación de llamadas o mensajería directa podrá considerarse una funcionalidad adicional.

**9. ¿La aplicación debe permitir la comunicación directa con los empleados para agilizar la coordinación de tareas, o estos deben ser localizados manualmente en sus respectivos sectores?**

No es indispensable para el MVP. La aplicación debe priorizar el monitoreo, la visualización de alertas y el registro de acciones.
Un módulo de comunicación interna podrá incorporarse en futuras versiones, según las indicaciones del docente.

## 4. Registros, historial y reportes

**10. ¿El historial de mediciones debe generarse automáticamente a intervalos definidos o debe ser registrado manualmente por los usuarios? Si debe generarse automáticamente, ¿con qué frecuencia?**

Se propone simular mediciones automáticas cada 1 minuto, registrando temperatura, humedad, galpón, fecha y hora.
Los estudiantes podrán generar estos datos mediante una API simulada. La frecuencia definitiva deberá validarse con el docente.

**11. ¿La aplicación debe permitir generar reportes históricos para elaborar gráficos y realizar estudios sobre el comportamiento de los galpones durante períodos determinados?**

Sí. Se recomienda incorporar gráficos básicos de temperatura y humedad que permitan visualizar su comportamiento durante períodos determinados.
La exportación de reportes a PDF o Excel podrá considerarse una funcionalidad adicional, según el alcance establecido por el docente.

**12. ¿Las acciones registradas por los operarios deben ser visibles para todos los usuarios autorizados, mantenerse como registros personales o permitir ambas modalidades?**

Se recomienda que las acciones registradas sean visibles para los supervisores y jefaturas autorizados.
Cada registro debería identificar al usuario, galpón, fecha, hora y acción realizada, permitiendo mantener trazabilidad de los eventos simulados.

**13. ¿La jefatura debe contar con un historial de turnos que permita consultar qué supervisor estuvo a cargo de cada sector y en qué horario?**

No es obligatorio para el MVP. Se podrá simular la asignación de supervisores a determinados sectores o galpones.

**14. ¿La jefatura debe tener acceso a los registros específicos de otros funcionarios? De ser así, ¿qué tipos de registros podrá consultar y qué restricciones de acceso se deben aplicar?**

Sí, según los permisos definidos para cada perfil.
Se recomienda permitir a las jefaturas consultar alertas, mediciones y acciones registradas por los operarios. Todos los usuarios y registros deberán ser ficticios, sin utilizar información personal real.

## Cumplimiento en la app

| # | En la app | Estado |
|---|-----------|--------|
| 1 | Umbrales ficticios guardados en DataStore (`umbrales`) y editables desde un formulario validado: menú ⋮ → Configurar umbrales (supervisor y jefatura); el dispositivo evalúa estados y alertas con ellos | Hecho |
| 2 | Advertencia: recomendación de revisar ventilación. Crítico: alerta prioritaria (canal de importancia alta, primeras en Alertas), notificación y botón *Registrar acción correctiva* para el operario. Dónde: `accionRecomendadaRes()` en `ui/screen/Formato.kt`, `AccionRecomendada` en `Componentes.kt`, detalle del galpón y tarjetas de Alertas | Hecho |
| 3 | °C y % con fecha y hora de medición; semáforo de colores con texto | Pendiente |
| 4 | Notificaciones solo para operario y supervisor; jefatura consulta el panel (resumen de inicio y pestaña Alertas). Dónde: `debeNotificar()` en `notifications/NotificationHelper.kt`, aplicado en `ContenedorApp.notificarCritico` (también en segundo plano) | Hecho |
| 5 | Simulación de temperatura y humedad desde la app: menú ⋮ → Simular medición (todos los perfiles). La lectura ficticia se guarda en DataStore y se evalúa con los umbrales, también sin conexión. Dónde: `ui/screen/SimulacionScreen.kt`, `viewmodel/SimulacionViewModel.kt`, `GalponRepository.registrarMedicionSimulada` | Hecho |
| 6 | Historial con medición, nivel y estado de atención; filtros por fecha, galpón y tipo | Pendiente |
| 7 | Supuesto: solo pollos de engorda; fundos y galpones ficticios fijos, sin sectores | Supuesto |
| 8 | Contacto ficticio del supervisor responsable visible en el detalle y en la alerta | Pendiente |
| 9 | Fuera del alcance del MVP | No aplica |
| 10 | Mediciones simuladas cada 1 minuto | Pendiente |
| 11 | Gráficos de temperatura y humedad por período; sin exportación | Pendiente |
| 12 | Cada acción guarda usuario, galpón, fecha, hora y acción; visible para supervisor y jefatura | Pendiente |
| 13 | Asignación ficticia de supervisor a galpones (ligada a la respuesta 8) | Pendiente |
| 14 | Jefatura consulta alertas, mediciones y acciones; todos los datos son ficticios | Parcial |
