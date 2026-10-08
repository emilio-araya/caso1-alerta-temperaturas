package com.example.caso1.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.caso1.data.db.DatabaseProvider
import com.example.caso1.data.repository.GalponRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(onCerrarSesion: () -> Unit, onVerDetalle: (Int) -> Unit = {}, onVerAlertas: () -> Unit = {}) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val repo = remember { GalponRepository(DatabaseProvider.get(context)) }

    LaunchedEffect(Unit) { repo.refrescar() }

    val galpones by repo.galpones.collectAsState(initial = emptyList())

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Galpones") },
                actions = {
                    TextButton(onClick = onVerAlertas) { Text("Alertas") }
                    TextButton(onClick = onCerrarSesion) { Text("Salir") }
                }
            )
        }
    ) { padding ->
        Column(Modifier.padding(padding).padding(16.dp)) {
            if (galpones.isEmpty()) {
                Text("Cargando galpones…")
            } else {
                galpones.forEach { g ->
                    Card(
                        onClick = { onVerDetalle(g.id) },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Text("${g.granja} · ${g.nombre}", style = MaterialTheme.typography.titleMedium)
                            Text("Estado: ${g.estado}")
                        }
                    }
                }
            }
        }
    }
}
