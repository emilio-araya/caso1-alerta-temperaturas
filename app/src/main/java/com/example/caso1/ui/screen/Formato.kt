package com.example.caso1.ui.screen

import androidx.compose.runtime.Composable
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import com.example.caso1.R
import com.example.caso1.data.db.AlertaEntity
import com.example.caso1.data.model.EstadoGalpon
import com.example.caso1.data.model.NivelAlerta
import com.example.caso1.data.model.Rol
import com.example.caso1.data.model.Umbrales
import com.example.caso1.data.model.textoTipoAlerta
import com.example.caso1.ui.theme.ColoresEstado
import com.example.caso1.ui.theme.LocalPaletaEstados
import java.text.SimpleDateFormat
import java.util.*

/** Utilidades de presentación compartidas entre pantallas. */

private val CHILE: Locale = Locale.forLanguageTag("es-CL")

@Composable
@ReadOnlyComposable
fun EstadoGalpon.colores(): ColoresEstado = LocalPaletaEstados.current.de(this)

@Composable
@ReadOnlyComposable
fun NivelAlerta.colores(): ColoresEstado = estado.colores()

/** Nombres de la escala oficial de alertas. */
fun EstadoGalpon.etiqueta(): String = when (this) {
    EstadoGalpon.NORMAL -> "Normal"
    EstadoGalpon.ADVERTENCIA -> "Alerta amarilla"
    EstadoGalpon.CRITICO -> "Alerta roja"
}

fun NivelAlerta.etiqueta(): String = estado.etiqueta()

/** Donde el color ya dice "alerta" basta con el nivel (resumen, tablas). */
fun EstadoGalpon.etiquetaCorta(): String = when (this) {
    EstadoGalpon.NORMAL -> "Normal"
    EstadoGalpon.ADVERTENCIA -> "Amarilla"
    EstadoGalpon.CRITICO -> "Roja"
}

fun Rol.etiqueta(): String = name.lowercase().replaceFirstChar { it.uppercase() }

fun Rol.descripcion(): String = when (this) {
    Rol.OPERARIO -> "Revisa los galpones, confirma alertas y registra acciones"
    Rol.SUPERVISOR -> "Sigue varios galpones, sus alertas y el historial"
    Rol.JEFATURA -> "Resumen del estado de los galpones y alertas del día"
}

fun Rol.icono(): Int = when (this) {
    Rol.OPERARIO -> R.drawable.ic_engineering
    Rol.SUPERVISOR -> R.drawable.ic_supervisor_account
    Rol.JEFATURA -> R.drawable.ic_monitoring
}

/** Cada cifra se colorea según su propio umbral: así se ve si el problema es el calor o la humedad. */
// Se evalúa el valor ya redondeado como se muestra: "70 %" nunca sale en dos colores distintos
fun estadoTemperatura(t: Double): EstadoGalpon = Umbrales.evaluar(Math.round(t * 10) / 10.0, HUMEDAD_NEUTRA)
fun estadoHumedad(h: Double): EstadoGalpon = Umbrales.evaluar(TEMPERATURA_NEUTRA, Math.round(h).toDouble())
private const val HUMEDAD_NEUTRA = 60.0
private const val TEMPERATURA_NEUTRA = 23.0

@Composable
@ReadOnlyComposable
fun colorCifra(estado: EstadoGalpon, normal: Color = MaterialTheme.colorScheme.onSurface): Color =
    if (estado == EstadoGalpon.NORMAL) normal else estado.colores().texto

fun tipoAlertaLegible(tipo: String): String = textoTipoAlerta(tipo)

/** 34.24 → "34,2" (coma decimal, como se escribe en Chile). */
fun cifra(valor: Double, decimales: Int = 1): String = String.format(CHILE, "%.${decimales}f", valor)

fun formatoFechaHora(millis: Long): String = SimpleDateFormat("dd/MM HH:mm", CHILE).format(Date(millis))

fun formatoHora(millis: Long): String = SimpleDateFormat("HH:mm", CHILE).format(Date(millis))

/** "Pendiente de confirmar" o "Confirmada por Supervisor · 08/10 23:40". */
fun estadoConfirmacion(alerta: AlertaEntity): String {
    val por = alerta.confirmadaPor ?: return "Pendiente de confirmar"
    val quien = runCatching { Rol.valueOf(por).etiqueta() }.getOrDefault(por)
    return "Confirmada por $quien" + (alerta.confirmadaEn?.let { " · ${formatoFechaHora(it)}" } ?: "")
}
