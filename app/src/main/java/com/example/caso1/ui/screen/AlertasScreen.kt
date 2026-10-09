package com.example.caso1.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.caso1.data.db.AlertaEntity
import com.example.caso1.data.db.DatabaseProvider
import com.example.caso1.data.model.Rol
import com.example.caso1.data.repository.GalponRepository
import kotlinx.coroutines.launch

/**
 * Alertas activas. Operario y supervisor pueden confirmarlas (acusar recibo);
 * jefatura solo consulta. La alerta sigue activa hasta que el galpón vuelva a la normalidad.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlertasScreen(rol: Rol?, onBack: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val repo = remember { GalponRepository(DatabaseProvider.get(context)) }
    val alertas by repo.alertasActivas.collectAsState(initial = emptyList())
    val galpones by repo.galpones.collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()

    val nombres = remember(galpones) { galpones.associate { it.id to "${it.granja} · ${it.nombre}" } }
    // Primero las pendientes, y dentro de ellas las críticas
    val ordenadas = alertas.sortedWith(compareBy<AlertaEntity>({ it.confirmadaPor != null }, { it.nivel != "CRITICO" }))
    val puedeConfirmar = rol == Rol.OPERARIO || rol == Rol.SUPERVISOR
    var porConfirmar by remember { mutableStateOf<AlertaEntity?>(null) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Alertas activas") }, navigationIcon = {
            TextButton(onClick = onBack) { Text("← Volver") }
        }) }
    ) { padding ->
        LazyColumn(
            Modifier.padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (alertas.isEmpty()) {
                item { Text("Sin alertas activas 🎉") }
            }
            items(ordenadas, key = { it.id }) { a ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text(
                            "${if (a.nivel == "CRITICO") "🔴" else "🟡"} ${tipoAlertaLegible(a.tipo)}",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text("${nombres[a.galponId] ?: "Galpón ${a.galponId}"} · ${formatoFechaHora(a.fechaHora)}")
                        Spacer(Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                estadoConfirmacion(a),
                                style = MaterialTheme.typography.bodySmall,
                                color = if (a.confirmadaPor == null) colorNivel(a.nivel)
                                    else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f)
                            )
                            if (puedeConfirmar && a.confirmadaPor == null) {
                                FilledTonalButton(onClick = { porConfirmar = a }) { Text("Confirmar") }
                            }
                        }
                    }
                }
            }
        }
    }

    porConfirmar?.let { a ->
        AlertDialog(
            onDismissRequest = { porConfirmar = null },
            title = { Text("Confirmar alerta") },
            text = {
                Text(
                    "${tipoAlertaLegible(a.tipo)} en ${nombres[a.galponId] ?: "galpón ${a.galponId}"}.\n\n" +
                        "Quedará registrado en el historial que la recibiste. La alerta seguirá " +
                        "activa hasta que las condiciones vuelvan a la normalidad."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val r = rol ?: return@TextButton
                    porConfirmar = null
                    scope.launch {
                        val galpon = nombres[a.galponId] ?: "Galpón ${a.galponId}"
                        repo.confirmarAlerta(a, r, "${tipoAlertaLegible(a.tipo)} en $galpon (${r.etiqueta()})")
                    }
                }) { Text("Confirmar") }
            },
            dismissButton = { TextButton(onClick = { porConfirmar = null }) { Text("Cancelar") } }
        )
    }
}
