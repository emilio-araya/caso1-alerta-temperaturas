package com.example.caso1.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.caso1.data.db.DatabaseProvider
import com.example.caso1.data.db.GalponEntity
import com.example.caso1.data.db.MedicionEntity
import com.example.caso1.data.model.EstadoGalpon
import com.example.caso1.data.model.Rol
import com.example.caso1.data.repository.GalponRepository
import com.example.caso1.notifications.NotificationHelper

/**
 * Pantalla principal. Lo que se muestra depende del perfil (punto 3.1 del caso):
 * - Operario: estado de galpones + registrar/confirmar acciones.
 * - Supervisor: galpones ordenados por gravedad + acceso a alertas activas.
 * - Jefatura: resumen por estado y alertas del día.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    rol: Rol?,
    onCerrarSesion: () -> Unit,
    onVerDetalle: (Int) -> Unit = {},
    onVerAlertas: () -> Unit = {},
    onRegistrar: () -> Unit = {},
    onVerHistorial: () -> Unit = {}
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val repo = remember { GalponRepository(DatabaseProvider.get(context)) }

    LaunchedEffect(Unit) {
        repo.refrescar().forEach { a ->
            NotificationHelper.notificarCritico(context, a.galponId, "${a.tipo} en galpón ${a.galponId}")
        }
    }

    val galpones by repo.galpones.collectAsState(initial = emptyList())
    val ultimas by repo.ultimasMediciones.collectAsState(initial = emptyList())
    val alertas by repo.alertasActivas.collectAsState(initial = emptyList())
    val historialAlertas by repo.alertas.collectAsState(initial = emptyList())
    val ultimaPorGalpon = remember(ultimas) { ultimas.associateBy { it.galponId } }

    // Supervisor y jefatura ven primero lo más grave; el operario, en orden de galpón
    val listado = if (rol == Rol.OPERARIO) galpones else galpones.sortedByDescending { it.estado.ordinal }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Galpones")
                        rol?.let { Text("Perfil: ${it.etiqueta()}", style = MaterialTheme.typography.labelMedium) }
                    }
                },
                actions = {
                    TextButton(onClick = onVerAlertas) { Text("Alertas (${alertas.size})") }
                    TextButton(onClick = onCerrarSesion) { Text("Salir") }
                }
            )
        },
        bottomBar = {
            val modBoton = Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp)
            when (rol) {
                Rol.OPERARIO -> Button(onClick = onRegistrar, modifier = modBoton) { Text("Registrar acción") }
                Rol.SUPERVISOR, Rol.JEFATURA ->
                    OutlinedButton(onClick = onVerHistorial, modifier = modBoton) { Text("Ver historial") }
                null -> {}
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (rol == Rol.JEFATURA && galpones.isNotEmpty()) {
                item {
                    val hoy = inicioDeHoy()
                    ResumenJefatura(
                        galpones,
                        alertasActivas = alertas.size,
                        alertasHoy = historialAlertas.count { it.fechaHora >= hoy }
                    )
                }
            }
            if (galpones.isEmpty()) {
                item { Text("Cargando galpones…") }
            }
            items(listado, key = { it.id }) { g ->
                TarjetaGalpon(g, ultimaPorGalpon[g.id]) { onVerDetalle(g.id) }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TarjetaGalpon(galpon: GalponEntity, ultima: MedicionEntity?, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(14.dp).background(galpon.estado.color(), CircleShape))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("${galpon.granja} · ${galpon.nombre}", style = MaterialTheme.typography.titleMedium)
                ultima?.let {
                    Text("${"%.1f".format(it.temperatura)} °C · HR ${"%.0f".format(it.humedad)} %")
                }
            }
            Text(
                galpon.estado.etiqueta(),
                color = galpon.estado.color(),
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun ResumenJefatura(galpones: List<GalponEntity>, alertasActivas: Int, alertasHoy: Int) {
    val conteo = galpones.groupingBy { it.estado }.eachCount()
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text("Resumen", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                EstadoGalpon.entries.forEach { e ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("${conteo[e] ?: 0}", style = MaterialTheme.typography.headlineMedium, color = e.color())
                        Text(e.etiqueta(), style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Text("$alertasActivas alertas activas de ${galpones.size} galpones")
            Text("$alertasHoy alertas registradas hoy", style = MaterialTheme.typography.bodySmall)
        }
    }
}
