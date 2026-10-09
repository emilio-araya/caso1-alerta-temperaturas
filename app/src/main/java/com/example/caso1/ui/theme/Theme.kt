package com.example.caso1.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.caso1.data.model.EstadoGalpon

private val EsquemaClaro = lightColorScheme(
    primary = Marino,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD6E3F3),
    onPrimaryContainer = MarinoProfundo,
    secondary = Color(0xFF4A5D73),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE0E7F0),
    onSecondaryContainer = Color(0xFF16263A),
    tertiary = Color(0xFF4A5D73),
    background = FondoClaro,
    onBackground = TintaClara,
    surface = FondoClaro,
    onSurface = TintaClara,
    surfaceVariant = Color(0xFFE2E7EE),
    onSurfaceVariant = Color(0xFF4F5E70),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFF8FAFC),
    surfaceContainer = Color(0xFFEDF1F5),
    surfaceContainerHigh = Color(0xFFE7ECF2),
    surfaceContainerHighest = Color(0xFFE1E7EE),
    outline = Color(0xFF8593A4),
    outlineVariant = Color(0xFFD5DCE4),
    error = AlertaRoja,
    onError = Color.White,
    errorContainer = Color(0xFFFCDDE1),
    onErrorContainer = Color(0xFF5C0012)
)

private val EsquemaOscuro = darkColorScheme(
    primary = Color(0xFF9CC2EE),
    onPrimary = Color(0xFF0B2440),
    primaryContainer = Color(0xFF1F3E62),
    onPrimaryContainer = Color(0xFFD6E3F3),
    secondary = Color(0xFFB4C3D6),
    onSecondary = Color(0xFF1E2D3F),
    secondaryContainer = Color(0xFF2A3A4E),
    onSecondaryContainer = Color(0xFFD6E1EE),
    tertiary = Color(0xFFB4C3D6),
    background = FondoOscuro,
    onBackground = TintaOscura,
    surface = FondoOscuro,
    onSurface = TintaOscura,
    surfaceVariant = Color(0xFF243245),
    onSurfaceVariant = Color(0xFFA9B6C6),
    surfaceContainerLowest = Color(0xFF09101A),
    surfaceContainerLow = Color(0xFF131D2A),
    surfaceContainer = Color(0xFF172231),
    surfaceContainerHigh = Color(0xFF1C2939),
    surfaceContainerHighest = Color(0xFF223043),
    outline = Color(0xFF6B7B8F),
    outlineVariant = Color(0xFF2A394C),
    error = TextoRojoOscuro,
    onError = Color(0xFF4A0010),
    errorContainer = Color(0xFF6B0F20),
    onErrorContainer = Color(0xFFFFDADF)
)

private val Formas = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(24.dp)
)

/** Colores de un estado: la franja (fondo pleno), su texto, y el color para texto sobre la superficie. */
@Immutable
data class ColoresEstado(val franja: Color, val sobreFranja: Color, val texto: Color)

@Immutable
data class PaletaEstados(val normal: ColoresEstado, val advertencia: ColoresEstado, val critico: ColoresEstado) {
    fun de(estado: EstadoGalpon): ColoresEstado = when (estado) {
        EstadoGalpon.NORMAL -> normal
        EstadoGalpon.ADVERTENCIA -> advertencia
        EstadoGalpon.CRITICO -> critico
    }
}

private val EstadosClaro = PaletaEstados(
    normal = ColoresEstado(AlertaVerde, Color.White, TextoVerdeClaro),
    advertencia = ColoresEstado(AlertaAmarilla, TintaSobreAmarillo, TextoAmarilloClaro),
    critico = ColoresEstado(AlertaRoja, Color.White, TextoRojoClaro)
)

private val EstadosOscuro = PaletaEstados(
    normal = ColoresEstado(AlertaVerde, Color.White, TextoVerdeOscuro),
    advertencia = ColoresEstado(AlertaAmarilla, TintaSobreAmarillo, TextoAmarilloOscuro),
    critico = ColoresEstado(AlertaRoja, Color.White, TextoRojoOscuro)
)

val LocalPaletaEstados = staticCompositionLocalOf { EstadosClaro }

/**
 * Tema de marca, sin color dinámico: el verde, amarillo y rojo de alerta tienen
 * significado y no deben cambiar con el fondo de pantalla del teléfono.
 */
@Composable
fun AlertaTemperaturasTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(LocalPaletaEstados provides if (darkTheme) EstadosOscuro else EstadosClaro) {
        MaterialTheme(
            colorScheme = if (darkTheme) EsquemaOscuro else EsquemaClaro,
            typography = Typography,
            shapes = Formas,
            content = content
        )
    }
}
