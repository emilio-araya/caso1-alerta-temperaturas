package com.example.caso1.data.api

import kotlinx.serialization.Serializable

/*
 * DTO (Data Transfer Object): la forma exacta del JSON que viaja por la red.
 * Se mantienen separados de las entidades de Room: si el servidor cambia un
 * campo, solo cambia el DTO y su conversión en el repositorio.
 */

@Serializable
data class GalponDto(
    val id: Int,
    val granja: String,
    val nombre: String,
    val estado: String          // "NORMAL" | "ADVERTENCIA" | "CRITICO"
)

@Serializable
data class MedicionDto(
    val galponId: Int,
    val temperatura: Double,
    val humedad: Double,
    val fechaHora: Long         // epoch millis
)

@Serializable
data class AlertaDto(
    val galponId: Int,
    val tipo: String,           // "TEMPERATURA_ALTA", "HUMEDAD_ALTA", ...
    val nivel: String,          // "ADVERTENCIA" | "CRITICO"
    val fechaHora: Long
)

@Serializable
data class SimulacionDto(val galponId: Int)
