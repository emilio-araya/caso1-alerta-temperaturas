package com.example.caso1.ui.navigation

object AppRoutes {
    const val LOGIN = "login"
    const val HOME = "home"
    const val DETALLE = "detalle/{galponId}"
    const val ALERTAS = "alertas"
    const val REGISTRAR = "registrar"
    const val CONFIRMAR = "confirmar"
    const val HISTORIAL = "historial"

    fun detalle(galponId: Int) = "detalle/$galponId"
}
