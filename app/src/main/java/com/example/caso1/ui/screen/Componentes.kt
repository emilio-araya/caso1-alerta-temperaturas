package com.example.caso1.ui.screen

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.caso1.R
import com.example.caso1.data.model.EstadoGalpon
import com.example.caso1.data.model.Rol
import com.example.caso1.data.model.ConfigUmbrales
import com.example.caso1.data.repository.EstadoConexion
import com.example.caso1.ui.navigation.AppRoutes
import com.example.caso1.viewmodel.EstadoRed
import com.example.caso1.ui.theme.EstiloEstado
import java.io.File

@Composable
fun Icono(@DrawableRes id: Int, descripcion: String?, modifier: Modifier = Modifier, tint: Color = LocalContentColor.current) {
    Icon(painterResource(id), descripcion, modifier, tint)
}

/** Lado máximo al que se reduce una foto de evidencia antes de dibujarla. */
private const val OBJETIVO_MINIATURA = 480

/**
 * Miniatura de una foto de evidencia (cámara), para el formulario y el historial.
 * Decodifica el JPEG reducido con `inSampleSize` para no cargar la foto completa en memoria;
 * si el archivo ya no existe, no dibuja nada.
 */
@Composable
internal fun MiniaturaFoto(ruta: String, lado: Dp, modifier: Modifier = Modifier) {
    val imagen = remember(ruta) { decodificarMiniatura(ruta) } ?: return
    Image(
        bitmap = imagen.asImageBitmap(),
        contentDescription = stringResource(R.string.foto_evidencia_descripcion),
        contentScale = ContentScale.Crop,
        modifier = modifier.size(lado).clip(MaterialTheme.shapes.small)
    )
}

/** Lee solo los límites del JPEG y luego lo decodifica con la reducción suficiente. */
private fun decodificarMiniatura(ruta: String): Bitmap? {
    if (!File(ruta).exists()) return null
    val limites = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(ruta, limites)
    if (limites.outWidth <= 0 || limites.outHeight <= 0) return null
    var muestra = 1
    while (limites.outWidth / (muestra * 2) >= OBJETIVO_MINIATURA || limites.outHeight / (muestra * 2) >= OBJETIVO_MINIATURA) {
        muestra *= 2
    }
    return BitmapFactory.decodeFile(ruta, BitmapFactory.Options().apply { inSampleSize = muestra })
}

/**
 * Franja de estado a todo el ancho, como el encabezado de un aviso oficial:
 * color pleno del estado, nombre en mayúsculas y un dato a la derecha.
 */
@Composable
fun FranjaEstado(estado: EstadoGalpon, texto: String, modifier: Modifier = Modifier, extra: String? = null) {
    val c = estado.colores()
    // A shed changing state fades to its new color instead of jumping (Guía 12)
    val fondo by animateColorAsState(c.franja, tween(450), label = "franjaEstado")
    val contenido by animateColorAsState(c.sobreFranja, tween(450), label = "franjaEstadoTexto")
    Row(
        modifier.fillMaxWidth().background(fondo).padding(horizontal = 16.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(texto.uppercase(), style = EstiloEstado, color = contenido, modifier = Modifier.weight(1f))
        extra?.let { Text(it, style = MaterialTheme.typography.labelMedium, color = contenido) }
    }
}

/** Cuadro pequeño del color del estado, para listas y tablas. */
@Composable
fun MarcaEstado(estado: EstadoGalpon, modifier: Modifier = Modifier) {
    Box(modifier.size(12.dp).background(estado.colores().franja, MaterialTheme.shapes.extraSmall))
}

private const val ESCALA_MIN = 14.0
private const val ESCALA_MAX = 38.0

/**
 * Escala térmica 14–38 °C con las zonas de los umbrales y una marca en la lectura actual.
 * Todas las tarjetas usan la misma escala, así se comparan galpones de un vistazo.
 */
@Composable
fun EscalaTermica(temperatura: Double, umbrales: ConfigUmbrales, modifier: Modifier = Modifier) {
    val paleta = com.example.caso1.ui.theme.LocalPaletaEstados.current
    val marca = MaterialTheme.colorScheme.onSurface
    val fondo = MaterialTheme.colorScheme.surfaceContainerLowest
    val textoEje = MaterialTheme.colorScheme.onSurfaceVariant
    val estiloEje = MaterialTheme.typography.labelSmall.copy(color = textoEje)
    val medidor = rememberTextMeasurer()
    val zonas = listOf(
        ESCALA_MIN to umbrales.tempNormalMin to paleta.advertencia.franja,
        umbrales.tempNormalMin to umbrales.tempNormalMax to paleta.normal.franja,
        umbrales.tempNormalMax to umbrales.tempAdvertenciaMax to paleta.advertencia.franja,
        umbrales.tempAdvertenciaMax to ESCALA_MAX to paleta.critico.franja
    )
    val descripcion = stringResource(R.string.escala_descripcion, cifra(temperatura))

    Canvas(modifier.fillMaxWidth().height(30.dp).semantics { contentDescription = descripcion }) {
        fun x(t: Double) = (size.width * ((t.coerceIn(ESCALA_MIN, ESCALA_MAX) - ESCALA_MIN) / (ESCALA_MAX - ESCALA_MIN))).toFloat()
        val alto = 6.dp.toPx()
        val top = 5.dp.toPx()
        zonas.forEach { (rango, color) ->
            val (desde, hasta) = rango
            drawRect(color, Offset(x(desde), top), Size(x(hasta) - x(desde), alto))
        }
        // Separaciones finas entre zonas
        listOf(umbrales.tempNormalMin, umbrales.tempNormalMax, umbrales.tempAdvertenciaMax).forEach { t ->
            drawRect(fondo, Offset(x(t) - 1f, top), Size(2f, alto))
            val etiqueta = medidor.measure("${cifraUmbral(t)}°", estiloEje)
            drawText(etiqueta, topLeft = Offset(x(t) - etiqueta.size.width / 2f, top + alto + 3.dp.toPx()))
        }
        // Marca de la lectura actual: triángulo sobre la barra y línea que la cruza
        val mx = x(temperatura)
        val lado = 5.dp.toPx()
        drawPath(Path().apply {
            moveTo(mx - lado, 0f); lineTo(mx + lado, 0f); lineTo(mx, lado); close()
        }, marca)
        drawRect(marca, Offset(mx - 1.5f, top - 1f), Size(3f, alto + 2f))
    }
}

/** Estado vacío con ícono, título y una línea que explica qué pasa ahora. */
@Composable
fun EstadoVacio(@DrawableRes icono: Int, titulo: String, texto: String, modifier: Modifier = Modifier, tint: Color = MaterialTheme.colorScheme.onSurfaceVariant) {
    Column(
        modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icono(icono, null, Modifier.size(56.dp), tint)
        Spacer(Modifier.height(16.dp))
        Text(titulo, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
        Spacer(Modifier.height(6.dp))
        Text(texto, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
    }
}

/**
 * "Qué hacer" según el estado (respuesta 2 del docente). En crítico, si [onRegistrarCorrectiva]
 * no es null (perfil operario), muestra el botón para registrar la acción correctiva.
 */
@Composable
fun AccionRecomendada(estado: EstadoGalpon, modifier: Modifier = Modifier, onRegistrarCorrectiva: (() -> Unit)? = null) {
    val texto = estado.accionRecomendadaRes() ?: return
    Column(modifier) {
        Row(verticalAlignment = Alignment.Top) {
            Icono(R.drawable.ic_warning, null, Modifier.size(18.dp), estado.colores().texto)
            Spacer(Modifier.width(8.dp))
            Column {
                Text(stringResource(R.string.accion_recomendada_titulo), style = MaterialTheme.typography.labelLarge)
                Text(stringResource(texto), style = MaterialTheme.typography.bodyMedium)
            }
        }
        if (estado == EstadoGalpon.CRITICO && onRegistrarCorrectiva != null) {
            Spacer(Modifier.height(8.dp))
            Button(onClick = onRegistrarCorrectiva, modifier = Modifier.fillMaxWidth()) {
                Icono(R.drawable.ic_edit_note, null, Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.accion_recomendada_boton))
            }
        }
    }
}

// ---------- Navegación principal (Material 3: barra inferior en teléfono, riel en pantallas grandes) ----------

enum class Destino(val ruta: String, @param:StringRes val etiqueta: Int, @param:DrawableRes val icono: Int) {
    GALPONES(AppRoutes.HOME, R.string.nav_galpones, R.drawable.ic_thermostat),
    ALERTAS(AppRoutes.ALERTAS, R.string.nav_alertas, R.drawable.ic_notifications),
    HISTORIAL(AppRoutes.HISTORIAL, R.string.nav_historial, R.drawable.ic_history)
}

/** El historial es para supervisión y jefatura (punto 3.1 del caso). */
fun destinosPara(rol: Rol?): List<Destino> =
    if (rol == Rol.OPERARIO) listOf(Destino.GALPONES, Destino.ALERTAS) else Destino.entries

/** Lo que toda pantalla principal necesita saber para dibujar la navegación. */
data class Marco(
    val rol: Rol?,
    val ancho: WindowWidthSizeClass,
    val alertasPendientes: Int,
    val onNavegar: (Destino) -> Unit,
    val red: EstadoRed = EstadoRed(),
    val onReintentar: () -> Unit = {}
)

/**
 * Aviso de conectividad (punto 3.3): sin conexión, la app sigue funcionando con
 * los datos guardados y dice de qué hora son. Usa colores neutros a propósito:
 * verde, amarillo y rojo están reservados para el estado de los galpones.
 */
@Composable
fun AvisoSinConexion(red: EstadoRed, onReintentar: () -> Unit, modifier: Modifier = Modifier) {
    AnimatedVisibility(
        visible = red.conexion == EstadoConexion.SIN_CONEXION,
        enter = expandVertically(),
        exit = shrinkVertically(),
        modifier = modifier
    ) {
        Surface(color = MaterialTheme.colorScheme.inverseSurface, contentColor = MaterialTheme.colorScheme.inverseOnSurface) {
            Row(
                Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icono(R.drawable.ic_cloud_off, null, Modifier.size(20.dp))
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.red_sin_conexion), style = MaterialTheme.typography.titleSmall)
                    Text(
                        red.ultimaActualizacion?.let { stringResource(R.string.red_datos_de, formatoFechaHora(it)) }
                            ?: stringResource(R.string.red_sin_datos),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                if (red.sincronizando) {
                    CircularProgressIndicator(Modifier.padding(horizontal = 16.dp).size(20.dp), strokeWidth = 2.dp,
                        color = LocalContentColor.current)
                } else {
                    TextButton(
                        onClick = onReintentar,
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.inversePrimary)
                    ) { Text(stringResource(R.string.red_reintentar)) }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaPrincipal(
    marco: Marco,
    actual: Destino,
    titulo: String,
    acciones: @Composable RowScope.() -> Unit = {},
    botonFlotante: @Composable () -> Unit = {},
    snackbarHost: @Composable () -> Unit = {},
    contenido: @Composable (PaddingValues) -> Unit
) {
    val destinos = destinosPara(marco.rol)
    val expandida = marco.ancho == WindowWidthSizeClass.Expanded

    @Composable
    fun IconoDestino(d: Destino) {
        if (d == Destino.ALERTAS && marco.alertasPendientes > 0) {
            BadgedBox(badge = { Badge { Text("${marco.alertasPendientes}") } }) { Icono(d.icono, stringResource(d.etiqueta)) }
        } else {
            Icono(d.icono, stringResource(d.etiqueta))
        }
    }

    val scaffold = @Composable {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(titulo, style = MaterialTheme.typography.titleLarge)
                            marco.rol?.let {
                                Text(it.etiqueta(), style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    },
                    actions = acciones,
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
                )
            },
            bottomBar = {
                if (!expandida) {
                    NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest) {
                        destinos.forEach { d ->
                            NavigationBarItem(
                                selected = d == actual,
                                onClick = { marco.onNavegar(d) },
                                icon = { IconoDestino(d) },
                                label = { Text(stringResource(d.etiqueta)) }
                            )
                        }
                    }
                }
            },
            floatingActionButton = botonFlotante,
            snackbarHost = snackbarHost
        ) { padding ->
            // El aviso va bajo la barra superior y el contenido de cada pantalla debajo de él
            Column(Modifier.padding(padding).consumeWindowInsets(padding)) {
                AvisoSinConexion(marco.red, marco.onReintentar)
                Box(Modifier.weight(1f)) { contenido(PaddingValues()) }
            }
        }
    }

    if (expandida) {
        Row(Modifier.fillMaxSize()) {
            NavigationRail(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest) {
                Spacer(Modifier.height(12.dp))
                destinos.forEach { d ->
                    NavigationRailItem(
                        selected = d == actual,
                        onClick = { marco.onNavegar(d) },
                        icon = { IconoDestino(d) },
                        label = { Text(stringResource(d.etiqueta)) }
                    )
                }
            }
            Box(Modifier.weight(1f)) { scaffold() }
        }
    } else {
        scaffold()
    }
}

/** Barra superior de las pantallas secundarias (detalle, registro, confirmación). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BarraSecundaria(titulo: String, onBack: () -> Unit) {
    TopAppBar(
        title = { Text(titulo, style = MaterialTheme.typography.titleLarge) },
        navigationIcon = {
            IconButton(onClick = onBack) { Icono(R.drawable.ic_arrow_back, stringResource(R.string.accion_volver)) }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
    )
}
