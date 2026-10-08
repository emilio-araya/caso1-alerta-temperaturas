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
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetalleGalponScreen(galponId: Int, onBack: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val repo = remember { GalponRepository(DatabaseProvider.get(context)) }
    val mediciones by repo.mediciones(galponId).collectAsState(initial = emptyList())
    val formato = remember { SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Galpón $galponId") }, navigationIcon = {
            TextButton(onClick = onBack) { Text("← Volver") }
        }) }
    ) { padding ->
        LazyColumn(Modifier.padding(padding).padding(16.dp)) {
            val ultima = mediciones.firstOrNull()
            item {
                if (ultima != null) {
                    Text("Temperatura: ${"%.1f".format(ultima.temperatura)} °C", style = MaterialTheme.typography.headlineSmall)
                    Text("Humedad: ${"%.0f".format(ultima.humedad)} %", style = MaterialTheme.typography.titleLarge)
                    Text("Actualizado: ${formato.format(Date(ultima.fechaHora))}")
                    Spacer(Modifier.height(16.dp))
                    Text("Historial", style = MaterialTheme.typography.titleMedium)
                } else {
                    Text("Sin mediciones registradas")
                }
            }
            items(mediciones) { m ->
                Text("${formato.format(Date(m.fechaHora))} — ${"%.1f".format(m.temperatura)} °C, ${"%.0f".format(m.humedad)} %")
                HorizontalDivider()
            }
        }
    }
}
