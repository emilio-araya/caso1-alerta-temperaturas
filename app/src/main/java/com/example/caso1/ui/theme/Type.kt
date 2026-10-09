package com.example.caso1.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.example.caso1.R

/** Barlow: tipografía con ADN de señalética pública, legible a distancia y a pleno sol. */
val Barlow = FontFamily(
    Font(R.font.barlow_regular, FontWeight.Normal),
    Font(R.font.barlow_medium, FontWeight.Medium),
    Font(R.font.barlow_semibold, FontWeight.SemiBold),
    Font(R.font.barlow_bold, FontWeight.Bold)
)

/** Barlow Condensed: cifras grandes y nombres de estado. */
val BarlowCondensed = FontFamily(
    Font(R.font.barlow_condensed_semibold, FontWeight.SemiBold),
    Font(R.font.barlow_condensed_bold, FontWeight.Bold)
)

private const val CIFRAS_TABULARES = "tnum"

private fun condensada(tamano: Int, linea: Int, peso: FontWeight = FontWeight.Bold) = TextStyle(
    fontFamily = BarlowCondensed,
    fontWeight = peso,
    fontSize = tamano.sp,
    lineHeight = linea.sp,
    letterSpacing = (-0.01).em,
    fontFeatureSettings = CIFRAS_TABULARES
)

private fun texto(tamano: Int, linea: Int, peso: FontWeight, espaciado: Double = 0.0) = TextStyle(
    fontFamily = Barlow,
    fontWeight = peso,
    fontSize = tamano.sp,
    lineHeight = linea.sp,
    letterSpacing = espaciado.em
)

val Typography = Typography(
    displayLarge = condensada(64, 68),
    displayMedium = condensada(52, 56),
    displaySmall = condensada(40, 44),
    headlineLarge = condensada(32, 38, FontWeight.SemiBold),
    headlineMedium = condensada(28, 34, FontWeight.SemiBold),
    headlineSmall = condensada(24, 30, FontWeight.SemiBold),
    titleLarge = texto(22, 28, FontWeight.SemiBold),
    titleMedium = texto(17, 24, FontWeight.SemiBold, 0.005),
    titleSmall = texto(15, 20, FontWeight.SemiBold, 0.005),
    bodyLarge = texto(17, 24, FontWeight.Normal),
    bodyMedium = texto(15, 21, FontWeight.Normal),
    bodySmall = texto(13, 18, FontWeight.Normal, 0.01),
    labelLarge = texto(15, 20, FontWeight.SemiBold, 0.01),
    labelMedium = texto(13, 16, FontWeight.SemiBold, 0.02),
    labelSmall = texto(12, 16, FontWeight.Medium, 0.03)
)

/** Nombre de estado en mayúsculas, como en los avisos oficiales ("ALERTA ROJA"). */
val EstiloEstado = TextStyle(
    fontFamily = BarlowCondensed,
    fontWeight = FontWeight.Bold,
    fontSize = 15.sp,
    lineHeight = 18.sp,
    letterSpacing = 0.06.em
)

/** Cifras en tablas y listas: mismo ancho para que las columnas alineen. */
val EstiloCifra = TextStyle(
    fontFamily = Barlow,
    fontWeight = FontWeight.Medium,
    fontSize = 15.sp,
    lineHeight = 20.sp,
    fontFeatureSettings = CIFRAS_TABULARES
)
