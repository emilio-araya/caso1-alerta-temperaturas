package com.example.caso1.ui.screen

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.caso1.R
import com.example.caso1.data.model.EstadoGalpon
import com.example.caso1.data.model.Rol
import com.example.caso1.data.model.Umbrales
import com.example.caso1.ui.navigation.AppRoutes
import com.example.caso1.ui.theme.EstiloEstado

@Composable
fun Icono(@DrawableRes id: Int, descripcion: String?, modifier: Modifier = Modifier, tint: Color = LocalContentColor.current) {
    Icon(painterResource(id), descripcion, modifier, tint)
}

/**
 * Franja de estado a todo el ancho, como el encabezado de un aviso oficial:
 * color pleno del estado, nombre en mayúsculas y un dato a la derecha.
 */
@Composable
fun FranjaEstado(estado: EstadoGalpon, texto: String, modifier: Modifier = Modifier, extra: String? = null) {
    val c = estado.colores()
    Row(
        modifier.fillMaxWidth().background(c.franja).padding(horizontal = 16.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(texto.uppercase(), style = EstiloEstado, color = c.sobreFranja, modifier = Modifier.weight(1f))
        extra?.let { Text(it, style = MaterialTheme.typography.labelMedium, color = c.sobreFranja) }
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
fun EscalaTermica(temperatura: Double, modifier: Modifier = Modifier) {
    val paleta = com.example.caso1.ui.theme.LocalPaletaEstados.current
    val marca = MaterialTheme.colorScheme.onSurface
    val fondo = MaterialTheme.colorScheme.surfaceContainerLowest
    val textoEje = MaterialTheme.colorScheme.onSurfaceVariant
    val estiloEje = MaterialTheme.typography.labelSmall.copy(color = textoEje)
    val medidor = rememberTextMeasurer()
    val zonas = listOf(
        ESCALA_MIN to Umbrales.TEMP_NORMAL_MIN to paleta.advertencia.franja,
        Umbrales.TEMP_NORMAL_MIN to Umbrales.TEMP_NORMAL_MAX to paleta.normal.franja,
        Umbrales.TEMP_NORMAL_MAX to Umbrales.TEMP_ADVERTENCIA_MAX to paleta.advertencia.franja,
        Umbrales.TEMP_ADVERTENCIA_MAX to ESCALA_MAX to paleta.critico.franja
    )
    val descripcion = "Escala térmica: ${cifra(temperatura)} grados"

    Canvas(modifier.fillMaxWidth().height(30.dp).semantics { contentDescription = descripcion }) {
        fun x(t: Double) = (size.width * ((t.coerceIn(ESCALA_MIN, ESCALA_MAX) - ESCALA_MIN) / (ESCALA_MAX - ESCALA_MIN))).toFloat()
        val alto = 6.dp.toPx()
        val top = 5.dp.toPx()
        zonas.forEach { (rango, color) ->
            val (desde, hasta) = rango
            drawRect(color, Offset(x(desde), top), Size(x(hasta) - x(desde), alto))
        }
        // Separaciones finas entre zonas
        listOf(Umbrales.TEMP_NORMAL_MIN, Umbrales.TEMP_NORMAL_MAX, Umbrales.TEMP_ADVERTENCIA_MAX).forEach { t ->
            drawRect(fondo, Offset(x(t) - 1f, top), Size(2f, alto))
            val etiqueta = medidor.measure("${t.toInt()}°", estiloEje)
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

// ---------- Navegación principal (Material 3: barra inferior en teléfono, riel en pantallas grandes) ----------

enum class Destino(val ruta: String, val etiqueta: String, @param:DrawableRes val icono: Int) {
    GALPONES(AppRoutes.HOME, "Galpones", R.drawable.ic_thermostat),
    ALERTAS(AppRoutes.ALERTAS, "Alertas", R.drawable.ic_notifications),
    HISTORIAL(AppRoutes.HISTORIAL, "Historial", R.drawable.ic_history)
}

/** El historial es para supervisión y jefatura (punto 3.1 del caso). */
fun destinosPara(rol: Rol?): List<Destino> =
    if (rol == Rol.OPERARIO) listOf(Destino.GALPONES, Destino.ALERTAS) else Destino.entries

/** Lo que toda pantalla principal necesita saber para dibujar la navegación. */
data class Marco(
    val rol: Rol?,
    val ancho: WindowWidthSizeClass,
    val alertasPendientes: Int,
    val onNavegar: (Destino) -> Unit
)

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
            BadgedBox(badge = { Badge { Text("${marco.alertasPendientes}") } }) { Icono(d.icono, d.etiqueta) }
        } else {
            Icono(d.icono, d.etiqueta)
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
                                label = { Text(d.etiqueta) }
                            )
                        }
                    }
                }
            },
            floatingActionButton = botonFlotante,
            snackbarHost = snackbarHost,
            content = contenido
        )
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
                        label = { Text(d.etiqueta) }
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
            IconButton(onClick = onBack) { Icono(R.drawable.ic_arrow_back, "Volver") }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
    )
}
