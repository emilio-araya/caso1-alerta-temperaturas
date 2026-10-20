package com.example.caso1.viewmodel

import androidx.annotation.StringRes
import com.example.caso1.R
import com.example.caso1.data.model.ConfigUmbrales

/**
 * Rangos físicos aceptados, también en la simulación (si se cambian, actualizar los
 * mensajes umbrales_error_rango_*).
 */
val RANGO_TEMPERATURA = -10.0..60.0
val RANGO_HUMEDAD = 0.0..100.0

/** Los seis campos del formulario de umbrales. El texto visible de cada uno está en strings.xml. */
enum class CampoUmbral(@param:StringRes val etiqueta: Int, val esTemperatura: Boolean) {
    TEMP_NORMAL_MIN(R.string.umbrales_normal_min, true),
    TEMP_NORMAL_MAX(R.string.umbrales_normal_max, true),
    TEMP_ADVERTENCIA_MAX(R.string.umbrales_advertencia_max, true),
    HUMEDAD_NORMAL_MIN(R.string.umbrales_normal_min, false),
    HUMEDAD_NORMAL_MAX(R.string.umbrales_normal_max, false),
    HUMEDAD_ADVERTENCIA_MAX(R.string.umbrales_advertencia_max, false)
}

/** Cada par (menor, mayor) debe cumplir menor < mayor; el error se marca en el mayor, con su mensaje. */
private val ORDEN = listOf(
    Triple(CampoUmbral.TEMP_NORMAL_MIN, CampoUmbral.TEMP_NORMAL_MAX, R.string.umbrales_error_mayor_min),
    Triple(CampoUmbral.TEMP_NORMAL_MAX, CampoUmbral.TEMP_ADVERTENCIA_MAX, R.string.umbrales_error_mayor_max),
    Triple(CampoUmbral.HUMEDAD_NORMAL_MIN, CampoUmbral.HUMEDAD_NORMAL_MAX, R.string.umbrales_error_mayor_min),
    Triple(CampoUmbral.HUMEDAD_NORMAL_MAX, CampoUmbral.HUMEDAD_ADVERTENCIA_MAX, R.string.umbrales_error_mayor_max)
)

/** "28,5" o "28.5" → 28.5; null si no es un número. */
fun leerNumero(texto: String): Double? = texto.trim().replace(',', '.').toDoubleOrNull()

/**
 * Reglas del formulario, sin dependencias de Android para poder probarlas con JUnit.
 * Devuelve el id del mensaje de error de cada campo inválido; vacío = todo correcto.
 */
fun validarUmbrales(textos: Map<CampoUmbral, String>): Map<CampoUmbral, Int> {
    val errores = mutableMapOf<CampoUmbral, Int>()
    val valores = mutableMapOf<CampoUmbral, Double>()
    CampoUmbral.entries.forEach { campo ->
        val texto = textos[campo].orEmpty()
        val valor = leerNumero(texto)
        val rango = if (campo.esTemperatura) RANGO_TEMPERATURA else RANGO_HUMEDAD
        when {
            texto.isBlank() -> errores[campo] = R.string.umbrales_error_vacio
            valor == null -> errores[campo] = R.string.umbrales_error_numero
            valor !in rango -> errores[campo] = if (campo.esTemperatura) R.string.umbrales_error_rango_temperatura
                else R.string.umbrales_error_rango_humedad
            else -> valores[campo] = valor
        }
    }
    // El orden solo se revisa entre campos que ya son números válidos
    ORDEN.forEach { (menor, mayor, mensaje) ->
        val a = valores[menor] ?: return@forEach
        val b = valores[mayor] ?: return@forEach
        if (b <= a) errores[mayor] = mensaje
    }
    return errores
}

/** Textos ya validados → umbrales. */
fun configDe(textos: Map<CampoUmbral, String>): ConfigUmbrales {
    fun v(c: CampoUmbral) = leerNumero(textos.getValue(c)) ?: error("validate before converting: $c")
    return ConfigUmbrales(
        v(CampoUmbral.TEMP_NORMAL_MIN), v(CampoUmbral.TEMP_NORMAL_MAX), v(CampoUmbral.TEMP_ADVERTENCIA_MAX),
        v(CampoUmbral.HUMEDAD_NORMAL_MIN), v(CampoUmbral.HUMEDAD_NORMAL_MAX), v(CampoUmbral.HUMEDAD_ADVERTENCIA_MAX)
    )
}

/** Umbrales → textos del formulario: 28.0 → "28", 28.5 → "28,5". */
fun textosDe(c: ConfigUmbrales): Map<CampoUmbral, String> = mapOf(
    CampoUmbral.TEMP_NORMAL_MIN to c.tempNormalMin,
    CampoUmbral.TEMP_NORMAL_MAX to c.tempNormalMax,
    CampoUmbral.TEMP_ADVERTENCIA_MAX to c.tempAdvertenciaMax,
    CampoUmbral.HUMEDAD_NORMAL_MIN to c.humedadNormalMin,
    CampoUmbral.HUMEDAD_NORMAL_MAX to c.humedadNormalMax,
    CampoUmbral.HUMEDAD_ADVERTENCIA_MAX to c.humedadAdvertenciaMax
).mapValues { (_, v) -> if (v % 1.0 == 0.0) v.toLong().toString() else v.toString().replace('.', ',') }
