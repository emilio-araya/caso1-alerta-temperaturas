package com.example.caso1.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.caso1.MainActivity
import com.example.caso1.R
import com.example.caso1.data.db.AlertaEntity
import com.example.caso1.ui.screen.tipoAlertaRes
import androidx.compose.ui.graphics.toArgb
import com.example.caso1.ui.theme.AlertaRoja

object NotificationHelper {
    private const val CHANNEL_ID = "alertas_criticas"

    /** Extra del Intent: al tocar la notificación la app abre directo en Alertas. */
    const val EXTRA_ABRIR_ALERTAS = "abrir_alertas"

    private fun crearCanal(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val canal = NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.notif_canal_nombre),
                NotificationManager.IMPORTANCE_HIGH
            ).apply { description = context.getString(R.string.notif_canal_desc) }
            context.getSystemService(NotificationManager::class.java).createNotificationChannel(canal)
        }
    }

    fun notificarCritico(context: Context, alerta: AlertaEntity) {
        // Android 13+: sin el permiso POST_NOTIFICATIONS concedido no se puede notificar
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return
        crearCanal(context)
        val intent = Intent(context, MainActivity::class.java).apply {
            putExtra(EXTRA_ABRIR_ALERTAS, true)
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pending = PendingIntent.getActivity(
            context, alerta.galponId, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val noti = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_warning)
            .setColor(AlertaRoja.toArgb())
            .setContentTitle(context.getString(R.string.notif_titulo, alerta.galponId))
            .setContentText(context.getString(R.string.notif_texto, context.getString(tipoAlertaRes(alerta.tipo))))
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pending)
            .setAutoCancel(true)
            .build()
        context.getSystemService(NotificationManager::class.java).notify(alerta.galponId, noti)
    }
}
