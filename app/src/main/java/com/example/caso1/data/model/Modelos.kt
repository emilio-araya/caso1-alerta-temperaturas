package com.example.caso1.data.model

/**
 * Estado ambiental de un galpón según reglas de negocio (umbrales).
 */
enum class EstadoGalpon {
    NORMAL, ADVERTENCIA, CRITICO
}

data class Galpon(
    val id: Int,
    val granja: String,
    val nombre: String,
    val estado: EstadoGalpon = EstadoGalpon.NORMAL
)

data class Medicion(
    val id: Int = 0,
    val galponId: Int,
    val temperatura: Double,
    val humedad: Double,
    val fechaHora: Long // epoch millis
)

enum class NivelAlerta {
    ADVERTENCIA, CRITICO;

    val estado: EstadoGalpon get() = if (this == CRITICO) EstadoGalpon.CRITICO else EstadoGalpon.ADVERTENCIA
}

enum class Rol { OPERARIO, SUPERVISOR, JEFATURA }

data class Alerta(
    val id: Int = 0,
    val galponId: Int,
    val tipo: String,        // Ej: "TEMPERATURA_ALTA", "HUMEDAD_ALTA"
    val nivel: NivelAlerta,
    val activa: Boolean = true,
    val fechaHora: Long
)


/**
 * Umbrales que definen los estados del galpón. Para el MVP son ficticios y configurables
 * (respuesta 1 del docente): se guardan en DataStore y se editan en "Configurar umbrales".
 * Sobre el máximo normal → advertencia; sobre el máximo de advertencia → crítico;
 * bajo el mínimo normal → advertencia. Los límites son inclusivos (28,0 °C sigue normal).
 */
data class ConfigUmbrales(
    val tempNormalMin: Double = Umbrales.TEMP_NORMAL_MIN,
    val tempNormalMax: Double = Umbrales.TEMP_NORMAL_MAX,
    val tempAdvertenciaMax: Double = Umbrales.TEMP_ADVERTENCIA_MAX,
    val humedadNormalMin: Double = Umbrales.HUMEDAD_NORMAL_MIN,
    val humedadNormalMax: Double = Umbrales.HUMEDAD_NORMAL_MAX,
    val humedadAdvertenciaMax: Double = Umbrales.HUMEDAD_ADVERTENCIA_MAX
) {
    fun evaluar(temperatura: Double, humedad: Double): EstadoGalpon = when {
        temperatura > tempAdvertenciaMax || humedad > humedadAdvertenciaMax -> EstadoGalpon.CRITICO
        temperatura > tempNormalMax || humedad > humedadNormalMax ||
            temperatura < tempNormalMin || humedad < humedadNormalMin -> EstadoGalpon.ADVERTENCIA
        else -> EstadoGalpon.NORMAL
    }

    /** Causa de la alerta: la condición que provoca el peor nivel, o null si todo es normal. */
    fun causa(temperatura: Double, humedad: Double): String? = when {
        temperatura > tempAdvertenciaMax -> "TEMPERATURA_ALTA"
        humedad > humedadAdvertenciaMax -> "HUMEDAD_ALTA"
        temperatura > tempNormalMax -> "TEMPERATURA_ALTA"
        humedad > humedadNormalMax -> "HUMEDAD_ALTA"
        temperatura < tempNormalMin -> "TEMPERATURA_BAJA"
        humedad < humedadNormalMin -> "HUMEDAD_BAJA"
        else -> null
    }
}

/** Valores predeterminados de los umbrales (los que se restauran desde el formulario). */
object Umbrales {
    const val TEMP_NORMAL_MIN = 18.0
    const val TEMP_NORMAL_MAX = 28.0
    const val TEMP_ADVERTENCIA_MAX = 32.0
    const val HUMEDAD_NORMAL_MIN = 50.0
    const val HUMEDAD_NORMAL_MAX = 70.0
    const val HUMEDAD_ADVERTENCIA_MAX = 80.0

    val PREDETERMINADOS = ConfigUmbrales()

    fun evaluar(temperatura: Double, humedad: Double): EstadoGalpon = PREDETERMINADOS.evaluar(temperatura, humedad)

    fun causa(temperatura: Double, humedad: Double): String? = PREDETERMINADOS.causa(temperatura, humedad)
}
