package com.example.caso1.viewmodel

import android.content.res.Resources
import androidx.annotation.StringRes

/**
 * Texto que el ViewModel quiere mostrar, sin depender de Context: guarda el id de
 * strings.xml y sus argumentos. La UI lo convierte en String con [resolver].
 */
data class TextoUi(@param:StringRes val id: Int, val args: List<Any> = emptyList()) {
    fun resolver(recursos: Resources): String = recursos.getString(id, *args.toTypedArray())
}
