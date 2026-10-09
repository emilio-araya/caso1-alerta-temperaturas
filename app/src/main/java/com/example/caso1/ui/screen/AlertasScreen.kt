package com.example.caso1.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.caso1.data.db.DatabaseProvider
import com.example.caso1.data.repository.GalponRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlertasScreen(onBack: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val repo = remember { GalponRepository(DatabaseProvider.get(context)) }
    val alertas by repo.alertasActivas.collectAsState(initial = emptyList())

    Scaffold(
        topBar = { TopAppBar(title = { Text("Alertas activas") }, navigationIcon = {
            TextButton(onClick = onBack) { Text("← Volver") }
        }) }
    ) { padding ->
        LazyColumn(Modifier.padding(padding).padding(16.dp)) {
            if (alertas.isEmpty()) {
                item { Text("Sin alertas activas 🎉") }
            }
            items(alertas) { a ->
                Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Column(Modifier.padding(16.dp)) {
                        Text("${if (a.nivel == "CRITICO") "🔴" else "🟡"} ${a.tipo}", style = MaterialTheme.typography.titleMedium)
                        Text("Galpón ${a.galponId} · Nivel: ${a.nivel}")
                    }
                }
            }
        }
    }
}
