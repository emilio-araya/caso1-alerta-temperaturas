package com.example.caso1.ui.screen

import androidx.compose.ui.graphics.Color
import com.example.caso1.data.db.AlertaEntity
import com.example.caso1.data.model.EstadoGalpon
import com.example.caso1.data.model.Rol
import com.example.caso1.ui.theme.EstadoAdvertencia
import com.example.caso1.ui.theme.EstadoCritico
import com.example.caso1.ui.theme.EstadoNormal
import java.text.SimpleDateFormat
import java.util.*

/** Utilidades de presentación compartidas entre pantallas. */

fun EstadoGalpon.color(): Color = when (this) {
    EstadoGalpon.NORMAL -> EstadoNormal
    EstadoGalpon.ADVERTENCIA -> EstadoAdvertencia
    EstadoGalpon.CRITICO -> EstadoCritico
}

fun EstadoGalpon.etiqueta(): String = when (this) {
    EstadoGalpon.NORMAL -> "Normal"
    EstadoGalpon.ADVERTENCIA -> "Advertencia"
    EstadoGalpon.CRITICO -> "Crítico"
}

fun Rol.etiqueta(): String = name.lowercase().replaceFirstChar { it.uppercase() }

/** Color según el nivel guardado en Room ("CRITICO" / "ADVERTENCIA"). */
fun colorNivel(nivel: String): Color = if (nivel == "CRITICO") EstadoCritico else EstadoAdvertencia

/** "TEMPERATURA_ALTA" → "Temperatura alta" */
fun tipoAlertaLegible(tipo: String): String =
    tipo.lowercase().replace('_', ' ').replaceFirstChar { it.uppercase() }

fun formatoFechaHora(millis: Long): String =
    SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()).format(Date(millis))

/** "Pendiente de confirmar" o "Confirmada por Supervisor · 08/10 23:40". */
fun estadoConfirmacion(alerta: AlertaEntity): String {
    val por = alerta.confirmadaPor ?: return "Pendiente de confirmar"
    val quien = runCatching { Rol.valueOf(por).etiqueta() }.getOrDefault(por)
    return "Confirmada por $quien" + (alerta.confirmadaEn?.let { " · ${formatoFechaHora(it)}" } ?: "")
}
