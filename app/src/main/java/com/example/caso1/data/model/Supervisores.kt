package com.example.caso1.data.model

/** Teléfono de contacto del supervisor responsable (respuesta 8 del docente). */
data class ContactoSupervisor(val nombre: String, val telefono: String)

/**
 * Contacto de los supervisores responsables de cada galpón (respuesta 8 del docente:
 * "incorporar información de contacto ficticia de los supervisores responsables").
 *
 * IMPORTANTE: los nombres y teléfonos son inventados; la app solo los muestra.
 * No hay llamadas ni mensajes (tampoco el permiso CALL_PHONE): es texto informativo.
 *
 * El mismo galpón siempre responde a la misma persona, para que la demo sea reproducible.
 */
object Supervisores {

    private val CONTACTOS = listOf(
        ContactoSupervisor("María Fuentes", "+56 9 1111 2222"),
        ContactoSupervisor("Jorge Salazar", "+56 9 3333 4444"),
        ContactoSupervisor("Paula Riquelme", "+56 9 5555 6666")
    )

    /** Supervisor ficticio asignado a [galponId]; con ids negativos el índice sigue siendo válido. */
    fun de(galponId: Int): ContactoSupervisor = CONTACTOS[Math.floorMod(galponId, CONTACTOS.size)]
}
