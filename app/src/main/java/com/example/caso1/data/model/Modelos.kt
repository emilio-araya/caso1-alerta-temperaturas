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


object Umbrales {
    const val TEMP_NORMAL_MIN = 18.0
    const val TEMP_NORMAL_MAX = 28.0
    const val TEMP_ADVERTENCIA_MAX = 32.0
    const val HUMEDAD_NORMAL_MIN = 50.0
    const val HUMEDAD_NORMAL_MAX = 70.0
    const val HUMEDAD_ADVERTENCIA_MAX = 80.0

    fun evaluar(temperatura: Double, humedad: Double): EstadoGalpon = when {
        temperatura > TEMP_ADVERTENCIA_MAX || humedad > HUMEDAD_ADVERTENCIA_MAX -> EstadoGalpon.CRITICO
        temperatura > TEMP_NORMAL_MAX || humedad > HUMEDAD_NORMAL_MAX ||
            temperatura < TEMP_NORMAL_MIN || humedad < HUMEDAD_NORMAL_MIN -> EstadoGalpon.ADVERTENCIA
        else -> EstadoGalpon.NORMAL
    }

    /** Causa de la alerta: la condición que provoca el peor nivel, o null si todo es normal. */
    fun causa(temperatura: Double, humedad: Double): String? = when {
        temperatura > TEMP_ADVERTENCIA_MAX -> "TEMPERATURA_ALTA"
        humedad > HUMEDAD_ADVERTENCIA_MAX -> "HUMEDAD_ALTA"
        temperatura > TEMP_NORMAL_MAX -> "TEMPERATURA_ALTA"
        humedad > HUMEDAD_NORMAL_MAX -> "HUMEDAD_ALTA"
        temperatura < TEMP_NORMAL_MIN -> "TEMPERATURA_BAJA"
        humedad < HUMEDAD_NORMAL_MIN -> "HUMEDAD_BAJA"
        else -> null
    }
}
