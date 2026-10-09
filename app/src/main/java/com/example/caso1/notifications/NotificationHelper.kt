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

object NotificationHelper {
    private const val CHANNEL_ID = "alertas_criticas"

    private fun crearCanal(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val canal = NotificationChannel(
                CHANNEL_ID,
                "Alertas críticas",
                NotificationManager.IMPORTANCE_HIGH
            ).apply { description = "Eventos críticos de temperatura/humedad en galpones" }
            context.getSystemService(NotificationManager::class.java).createNotificationChannel(canal)
        }
    }

    fun notificarCritico(context: Context, galponId: Int, mensaje: String) {
        // Android 13+: sin el permiso POST_NOTIFICATIONS concedido no se puede notificar
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return
        crearCanal(context)
        val intent = Intent(context, MainActivity::class.java)
        val pending = PendingIntent.getActivity(
            context, galponId, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val noti = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_notify_error)
            .setContentTitle("⚠️ Alerta crítica — Galpón $galponId")
            .setContentText(mensaje)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pending)
            .setAutoCancel(true)
            .build()
        context.getSystemService(NotificationManager::class.java).notify(galponId, noti)
    }
}
