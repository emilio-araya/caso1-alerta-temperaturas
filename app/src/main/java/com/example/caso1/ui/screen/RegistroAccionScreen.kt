package com.example.caso1.ui.screen

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.caso1.R
import com.example.caso1.data.local.borrarEvidencia
import com.example.caso1.data.local.crearArchivoEvidencia
import com.example.caso1.data.local.uriEvidencia
import com.example.caso1.viewmodel.MIN_DESCRIPCION
import com.example.caso1.viewmodel.RegistroAccionViewModel
import com.example.caso1.viewmodel.TipoAccion

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RegistroAccionScreen(
    viewModel: RegistroAccionViewModel = viewModel(factory = RegistroAccionViewModel.Factory),
    onBack: () -> Unit,
    onRegistrado: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val galpones by viewModel.galpones.collectAsStateWithLifecycle()
    val errores = state.errores
    val context = LocalContext.current

    // Ruta del archivo que la cámara del sistema va a escribir (TakePicture solo devuelve true/false)
    // rememberSaveable: si el sistema recrea la actividad mientras la cámara está abierta,
    // el resultado tiene que seguir sabiendo en qué archivo se escribió la foto.
    var fotoPendiente by rememberSaveable { mutableStateOf<String?>(null) }

    val capturarFoto = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { guardada ->
        // Cancelar descarta la foto y también el archivo vacío que se preparó para la cámara
        if (!guardada) borrarEvidencia(fotoPendiente)
        viewModel.onFotoChange(if (guardada) fotoPendiente else null)
        fotoPendiente = null
    }

    // Hay que crear el archivo antes de lanzar la cámara: la app de cámara escribe en esa URI
    val tomarFoto = {
        val archivo = crearArchivoEvidencia(context)
        fotoPendiente = archivo.absolutePath
        capturarFoto.launch(uriEvidencia(context, archivo))
    }

    val pedirPermisoCamara = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { concedido ->
        if (concedido) tomarFoto() else viewModel.permisoCamaraDenegado()
    }

    LaunchedEffect(state.registrado) {
        if (state.registrado) {
            viewModel.navegacionRealizada()
            onRegistrado()
        }
    }

    Scaffold(topBar = { BarraSecundaria(stringResource(R.string.registro_titulo), onBack) }) { padding ->
        // Desplazable y respetando el teclado: en pantallas pequeñas el botón quedaba tapado
        Column(
            Modifier.padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .widthIn(max = 640.dp)
                .padding(16.dp)
        ) {
            Seccion(stringResource(R.string.registro_galpon), errores.galpon)
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
                border = BorderStroke(
                    1.dp,
                    if (errores.galpon != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outlineVariant
                )
            ) {
                Column(Modifier.selectableGroup()) {
                    galpones.forEachIndexed { i, g ->
                        if (i > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        Row(
                            Modifier.fillMaxWidth()
                                .selectable(
                                    selected = state.galponId == g.id,
                                    onClick = { viewModel.onGalponChange(g.id) },
                                    role = Role.RadioButton
                                )
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = state.galponId == g.id, onClick = null)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(g.nombre, style = MaterialTheme.typography.titleSmall)
                                Text(g.granja, style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            MarcaEstado(g.estado)
                            Spacer(Modifier.width(6.dp))
                            Text(g.estado.etiqueta(), style = MaterialTheme.typography.labelMedium,
                                color = colorCifra(g.estado, MaterialTheme.colorScheme.onSurfaceVariant))
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
            Seccion(stringResource(R.string.registro_tipo), errores.tipo)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                TipoAccion.entries.forEach { tipo ->
                    val elegido = state.tipoAccion == tipo
                    FilterChip(
                        selected = elegido,
                        onClick = { viewModel.onTipoChange(tipo) },
                        label = { Text(stringResource(tipo.etiqueta)) },
                        leadingIcon = if (elegido) {
                            { Icono(R.drawable.ic_check_circle, null, Modifier.size(18.dp)) }
                        } else null
                    )
                }
            }

            Spacer(Modifier.height(24.dp))
            Seccion(stringResource(R.string.registro_que_hiciste), null)
            OutlinedTextField(
                value = state.descripcion,
                onValueChange = viewModel::onDescripcionChange,
                placeholder = { Text(stringResource(R.string.registro_placeholder)) },
                isError = errores.descripcion != null,
                supportingText = {
                    Text(
                        errores.descripcion?.let { stringResource(it, MIN_DESCRIPCION) }
                            ?: stringResource(R.string.registro_contador, state.descripcion.trim().length, MIN_DESCRIPCION)
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )

            Spacer(Modifier.height(24.dp))
            Seccion(stringResource(R.string.registro_foto_titulo), null)
            Text(
                stringResource(R.string.registro_foto_ayuda),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(onClick = {
                    // Permiso en tiempo de ejecución (rúbrica EP3): ya concedido o se pide ahora
                    val concedido = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                        PackageManager.PERMISSION_GRANTED
                    if (concedido) tomarFoto() else pedirPermisoCamara.launch(Manifest.permission.CAMERA)
                }) {
                    Text(stringResource(if (state.fotoRuta == null) R.string.registro_foto_tomar else R.string.registro_foto_repetir))
                }
                if (state.fotoRuta != null) {
                    TextButton(onClick = { viewModel.onFotoChange(null) }) {
                        Text(stringResource(R.string.registro_foto_quitar))
                    }
                }
            }
            state.fotoRuta?.let { ruta ->
                Spacer(Modifier.height(8.dp))
                MiniaturaFoto(ruta, lado = 160.dp)
            }
            state.mensajeFoto?.let { mensaje ->
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icono(R.drawable.ic_warning, null, Modifier.size(16.dp), MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.width(4.dp))
                    Text(
                        stringResource(mensaje),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(24.dp))
            Button(
                onClick = viewModel::registrar,
                enabled = !state.guardando,
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                if (state.guardando) {
                    CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = LocalContentColor.current)
                } else {
                    Text(stringResource(R.string.registro_boton))
                }
            }
        }
    }
}

@Composable
private fun Seccion(titulo: String, @StringRes error: Int?) {
    Text(titulo, style = MaterialTheme.typography.titleMedium)
    if (error != null) {
        Row(Modifier.padding(top = 2.dp), verticalAlignment = Alignment.CenterVertically) {
            Icono(R.drawable.ic_warning, null, Modifier.size(16.dp), MaterialTheme.colorScheme.error)
            Spacer(Modifier.width(4.dp))
            Text(stringResource(error), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
        }
    }
    Spacer(Modifier.height(8.dp))
}
