package com.example.caso1.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.caso1.R
import com.example.caso1.viewmodel.CampoUmbral
import com.example.caso1.viewmodel.UmbralesUiState
import com.example.caso1.viewmodel.UmbralesViewModel

/**
 * Formulario de umbrales (supervisor y jefatura). La validación vive en el ViewModel
 * ([com.example.caso1.viewmodel.validarUmbrales]); aquí solo se muestra cada error junto a su campo.
 */
@Composable
fun UmbralesScreen(
    onBack: () -> Unit,
    viewModel: UmbralesViewModel = viewModel(factory = UmbralesViewModel.Factory)
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val recursos = LocalResources.current

    LaunchedEffect(state.mensaje) {
        state.mensaje?.let {
            snackbar.showSnackbar(recursos.getString(it))
            viewModel.mensajeMostrado()
        }
    }

    Scaffold(
        topBar = { BarraSecundaria(stringResource(R.string.umbrales_titulo), onBack) },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        if (state.cargando) {
            Box(Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            return@Scaffold
        }
        // Desplazable y respetando el teclado, igual que el registro de acciones
        Column(
            Modifier.padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .widthIn(max = 640.dp)
                .padding(16.dp)
        ) {
            Text(
                stringResource(R.string.umbrales_explicacion),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(24.dp))
            Text(stringResource(R.string.umbrales_seccion_temperatura), style = MaterialTheme.typography.titleMedium)
            CampoUmbral.entries.filter { it.esTemperatura }.forEach { CampoNumero(it, state, viewModel::onCambio) }

            Spacer(Modifier.height(16.dp))
            Text(stringResource(R.string.umbrales_seccion_humedad), style = MaterialTheme.typography.titleMedium)
            CampoUmbral.entries.filter { !it.esTemperatura }.forEach { CampoNumero(it, state, viewModel::onCambio) }

            Spacer(Modifier.height(24.dp))
            Button(
                onClick = viewModel::guardar,
                enabled = !state.guardando,
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                if (state.guardando) {
                    CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = LocalContentColor.current)
                } else {
                    Text(stringResource(R.string.umbrales_boton_guardar))
                }
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = viewModel::restaurarPredeterminados,
                enabled = !state.guardando,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.umbrales_boton_restaurar))
            }
        }
    }
}

/** Campo numérico con unidad; en rojo, con ícono y mensaje cuando no es válido. */
@Composable
private fun CampoNumero(campo: CampoUmbral, state: UmbralesUiState, onCambio: (CampoUmbral, String) -> Unit) {
    val error = state.errores[campo]
    OutlinedTextField(
        value = state.textos[campo].orEmpty(),
        onValueChange = { onCambio(campo, it) },
        label = { Text(stringResource(campo.etiqueta)) },
        suffix = {
            Text(stringResource(if (campo.esTemperatura) R.string.umbrales_unidad_temperatura else R.string.umbrales_unidad_humedad))
        },
        isError = error != null,
        supportingText = error?.let { { Text(stringResource(it)) } },
        trailingIcon = error?.let {
            { Icono(R.drawable.ic_warning, stringResource(R.string.umbrales_error_icono), tint = MaterialTheme.colorScheme.error) }
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
    )
}
