package com.example.caso1.viewmodel

import com.example.caso1.R
import com.example.caso1.data.model.ConfigUmbrales
import com.example.caso1.data.model.EstadoGalpon

/** Los campos del formulario "Simular medición". */
enum class CampoSimulacion { GALPON, TEMPERATURA, HUMEDAD }

/**
 * Reglas del formulario, sin dependencias de Android para poder probarlas con JUnit.
 * Devuelve el id del mensaje de error de cada campo inválido; vacío = todo correcto.
 * Los mensajes de los números son los mismos del formulario de umbrales.
 */
fun validarSimulacion(galponId: Int?, galponesExistentes: Set<Int>, temperatura: String, humedad: String): Map<CampoSimulacion, Int> {
    val errores = mutableMapOf<CampoSimulacion, Int>()
    if (galponId == null || galponId !in galponesExistentes) errores[CampoSimulacion.GALPON] = R.string.simulacion_error_galpon
    errorNumero(temperatura, RANGO_TEMPERATURA, R.string.umbrales_error_rango_temperatura)?.let { errores[CampoSimulacion.TEMPERATURA] = it }
    errorNumero(humedad, RANGO_HUMEDAD, R.string.umbrales_error_rango_humedad)?.let { errores[CampoSimulacion.HUMEDAD] = it }
    return errores
}

/** Vacío, no numérico o fuera de [rango] → id del mensaje; null si el número es válido. */
private fun errorNumero(texto: String, rango: ClosedFloatingPointRange<Double>, mensajeRango: Int): Int? {
    val valor = leerNumero(texto)
    return when {
        texto.isBlank() -> R.string.umbrales_error_vacio
        valor == null -> R.string.umbrales_error_numero
        valor !in rango -> mensajeRango
        else -> null
    }
}

/**
 * Vista previa: el estado que tendría el galpón con estos valores y los umbrales actuales,
 * o null mientras alguno de los dos números no sea válido.
 */
fun estadoSimulado(temperatura: String, humedad: String, umbrales: ConfigUmbrales): EstadoGalpon? {
    val t = leerNumero(temperatura)?.takeIf { it in RANGO_TEMPERATURA } ?: return null
    val h = leerNumero(humedad)?.takeIf { it in RANGO_HUMEDAD } ?: return null
    return umbrales.evaluar(t, h)
}
