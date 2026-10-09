package com.example.caso1.ui.screen

import androidx.annotation.StringRes
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import com.example.caso1.R
import com.example.caso1.data.db.GalponEntity
import com.example.caso1.data.model.EstadoGalpon
import com.example.caso1.data.model.NivelAlerta
import com.example.caso1.data.model.Rol
import com.example.caso1.data.model.Umbrales
import com.example.caso1.data.repository.ACCION_CONFIRMAR
import com.example.caso1.ui.theme.ColoresEstado
import com.example.caso1.ui.theme.LocalPaletaEstados
import com.example.caso1.viewmodel.TipoAccion
import java.text.SimpleDateFormat
import java.util.*

/** Utilidades de presentación compartidas entre pantallas. Los textos viven en strings.xml. */

private val CHILE: Locale = Locale.forLanguageTag("es-CL")

@Composable
@ReadOnlyComposable
fun EstadoGalpon.colores(): ColoresEstado = LocalPaletaEstados.current.de(this)

@Composable
@ReadOnlyComposable
fun NivelAlerta.colores(): ColoresEstado = estado.colores()

// ---------- Estados ----------

@StringRes
fun EstadoGalpon.etiquetaRes(): Int = when (this) {
    EstadoGalpon.NORMAL -> R.string.estado_normal
    EstadoGalpon.ADVERTENCIA -> R.string.estado_amarilla
    EstadoGalpon.CRITICO -> R.string.estado_roja
}

/** Donde el color ya dice "alerta" basta con el nivel (resumen, tablas). */
@StringRes
fun EstadoGalpon.etiquetaCortaRes(): Int = when (this) {
    EstadoGalpon.NORMAL -> R.string.estado_corto_normal
    EstadoGalpon.ADVERTENCIA -> R.string.estado_corto_amarilla
    EstadoGalpon.CRITICO -> R.string.estado_corto_roja
}

@Composable
fun EstadoGalpon.etiqueta(): String = stringResource(etiquetaRes())

@Composable
fun EstadoGalpon.etiquetaCorta(): String = stringResource(etiquetaCortaRes())

@Composable
fun NivelAlerta.etiqueta(): String = estado.etiqueta()

// ---------- Perfiles ----------

@StringRes
fun Rol.etiquetaRes(): Int = when (this) {
    Rol.OPERARIO -> R.string.rol_operario
    Rol.SUPERVISOR -> R.string.rol_supervisor
    Rol.JEFATURA -> R.string.rol_jefatura
}

@StringRes
fun Rol.descripcionRes(): Int = when (this) {
    Rol.OPERARIO -> R.string.rol_operario_desc
    Rol.SUPERVISOR -> R.string.rol_supervisor_desc
    Rol.JEFATURA -> R.string.rol_jefatura_desc
}

@Composable
fun Rol.etiqueta(): String = stringResource(etiquetaRes())

fun Rol.icono(): Int = when (this) {
    Rol.OPERARIO -> R.drawable.ic_engineering
    Rol.SUPERVISOR -> R.drawable.ic_supervisor_account
    Rol.JEFATURA -> R.drawable.ic_monitoring
}

// ---------- Alertas y acciones ----------

/** Código de causa guardado en Room ("TEMPERATURA_ALTA") → texto en strings.xml. */
@StringRes
fun tipoAlertaRes(tipo: String): Int = when (tipo) {
    "TEMPERATURA_ALTA" -> R.string.alerta_tipo_temperatura_alta
    "TEMPERATURA_BAJA" -> R.string.alerta_tipo_temperatura_baja
    "HUMEDAD_ALTA" -> R.string.alerta_tipo_humedad_alta
    else -> R.string.alerta_tipo_humedad_baja
}

@Composable
fun tipoAlertaLegible(tipo: String): String = stringResource(tipoAlertaRes(tipo))

/**
 * Nombre visible de una acción guardada. Las acciones nuevas guardan un código
 * (VENTILACION, CONFIRMACION_ALERTA); las antiguas guardaban el texto, que se muestra tal cual.
 */
@Composable
fun accionLegible(codigo: String?): String = when (codigo) {
    null -> stringResource(R.string.accion_evento)
    ACCION_CONFIRMAR -> stringResource(R.string.accion_confirmacion)
    else -> TipoAccion.entries.firstOrNull { it.name == codigo }?.let { stringResource(it.etiqueta) } ?: codigo
}

/** "Confirmada por Supervisor · 08/10 23:40" o "Pendiente de confirmar". */
@Composable
fun textoConfirmacion(por: Rol?, en: Long?): String =
    if (por == null) stringResource(R.string.pendiente_confirmar)
    else stringResource(R.string.confirmada_por, por.etiqueta(), en?.let(::formatoFechaHora) ?: "")

@Composable
fun textoConfirmacion(porCodigo: String?, en: Long?): String =
    textoConfirmacion(porCodigo?.let { runCatching { Rol.valueOf(it) }.getOrNull() }, en)

// ---------- Galpones ----------

@Composable
fun nombreGalpon(granja: String?, galpon: String?, id: Int): String =
    if (granja == null || galpon == null) stringResource(R.string.galpon_generico, id)
    else stringResource(R.string.galpon_nombre_completo, granja, galpon)

@Composable
fun nombreGalpon(g: GalponEntity?, id: Int): String = nombreGalpon(g?.granja, g?.nombre, id)

// ---------- Cifras y fechas ----------

/** 34.24 → "34,2" (coma decimal, como se escribe en Chile). */
fun cifra(valor: Double, decimales: Int = 1): String = String.format(CHILE, "%.${decimales}f", valor)

fun formatoFechaHora(millis: Long): String = SimpleDateFormat("dd/MM HH:mm", CHILE).format(Date(millis))

fun formatoHora(millis: Long): String = SimpleDateFormat("HH:mm", CHILE).format(Date(millis))

// Se evalúa el valor ya redondeado como se muestra: "70 %" nunca sale en dos colores distintos
fun estadoTemperatura(t: Double): EstadoGalpon = Umbrales.evaluar(Math.round(t * 10) / 10.0, HUMEDAD_NEUTRA)
fun estadoHumedad(h: Double): EstadoGalpon = Umbrales.evaluar(TEMPERATURA_NEUTRA, Math.round(h).toDouble())
private const val HUMEDAD_NEUTRA = 60.0
private const val TEMPERATURA_NEUTRA = 23.0

/** Cada cifra se colorea según su propio umbral: así se ve si el problema es el calor o la humedad. */
@Composable
@ReadOnlyComposable
fun colorCifra(estado: EstadoGalpon, normal: Color = MaterialTheme.colorScheme.onSurface): Color =
    if (estado == EstadoGalpon.NORMAL) normal else estado.colores().texto
