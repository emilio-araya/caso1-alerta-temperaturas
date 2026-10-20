package com.example.caso1.data.local

import com.example.caso1.data.model.EstadoGalpon
import com.example.caso1.data.model.NivelAlerta
import kotlinx.serialization.Serializable

/*
 * Registros guardados localmente en DataStore (archivo JSON, ver [DatosMonitoreo]).
 * Se mantienen separados de los DTO de la API: la app los muestra aunque no haya conexión.
 */

/** Galpón guardado localmente en DataStore. */
@Serializable
data class GalponEntity(
    val id: Int,
    val granja: String,
    val nombre: String,
    val estado: EstadoGalpon
)

/** Medición guardada localmente en DataStore; una sola por galpón e instante (galponId, fechaHora). */
@Serializable
data class MedicionEntity(
    val id: Int = 0,
    val galponId: Int,
    val temperatura: Double,
    val humedad: Double,
    val fechaHora: Long
)

/** Alerta guardada localmente en DataStore; como máximo una activa por galpón. */
@Serializable
data class AlertaEntity(
    val id: Int = 0,
    val galponId: Int,
    val tipo: String,
    val nivel: NivelAlerta,   // se guarda como texto ("CRITICO")
    val activa: Boolean,
    val fechaHora: Long,
    // Confirmación ("acuso recibo"): la alerta sigue activa mientras dure la condición
    val confirmadaPor: String? = null,
    val confirmadaEn: Long? = null,
    // Medición que produjo la alerta (respuesta 6). Null en alertas guardadas antes de este campo.
    val temperatura: Double? = null,
    val humedad: Double? = null
)

/** Evento (acción registrada) guardado localmente en DataStore. */
@Serializable
data class EventoEntity(
    val id: Int = 0,
    val galponId: Int,
    val descripcion: String,
    val accionRegistrada: String?,
    val fechaHora: Long,
    // Rol de quien registró la acción ("OPERARIO"). Null en eventos guardados antes de este campo.
    val usuario: String? = null
)
