package com.annesophie.semaine

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

/**
 * Rappel du soir : à l'heure choisie (21 h par défaut), notification s'il reste
 * des tâches ☾ Soir pour aujourd'hui. Reprogrammé après chaque rappel et au redémarrage.
 */
class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_FIRE -> {
                notifyIfNeeded(context)
                schedule(context)
            }
            Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_MY_PACKAGE_REPLACED -> {
                schedule(context)
                DayWidgetProvider.refreshAll(context)
            }
        }
    }

    companion object {
        const val ACTION_FIRE = "com.annesophie.semaine.REMINDER"
        private const val CHANNEL = "rappel_soir"
        private const val NOTIF_ID = 2101

        private fun pending(context: Context): PendingIntent = PendingIntent.getBroadcast(
            context, 9002,
            Intent(context, ReminderReceiver::class.java).setAction(ACTION_FIRE),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        /** Programme le prochain rappel (ou l'annule s'il est désactivé). */
        fun schedule(context: Context) {
            val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val pi = pending(context)
            am.cancel(pi)
            if (!TaskStore.reminderEnabled(context)) return
            val minutes = TaskStore.reminderMinutes(context)
            val time = LocalTime.of(minutes / 60, minutes % 60)
            var next = LocalDateTime.of(LocalDate.now(), time)
            if (!next.isAfter(LocalDateTime.now())) next = next.plusDays(1)
            am.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                next.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
                pi
            )
        }

        fun notifyIfNeeded(context: Context) {
            if (Build.VERSION.SDK_INT >= 33 &&
                context.checkSelfPermission("android.permission.POST_NOTIFICATIONS") != PackageManager.PERMISSION_GRANTED
            ) return

            val left = TaskStore.entries(context, Day.todaySources(), true)
                .filter { it.task.period() == Period.EVENING && !it.done }
            if (left.isEmpty()) return

            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL, "Rappel du soir", NotificationManager.IMPORTANCE_DEFAULT)
            )

            val open = PendingIntent.getActivity(
                context, 9003,
                Intent(context, MainActivity::class.java).apply {
                    putExtra(DayWidgetProvider.EXTRA_DAY, Day.today().name)
                    data = Uri.parse("semaine://reminder")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val n = left.size
            val title = "☾ Routine du soir"
            val text = if (n == 1) "Il reste 1 tâche" else "Il reste $n tâches"
            val details = left.joinToString("\n") { "• ${it.task.displayText()}" }

            val notification = Notification.Builder(context, CHANNEL)
                .setSmallIcon(R.drawable.ic_notif)
                .setContentTitle(title)
                .setContentText(text)
                .setStyle(Notification.BigTextStyle().bigText(details))
                .setContentIntent(open)
                .setAutoCancel(true)
                .build()
            nm.notify(NOTIF_ID, notification)
        }
    }
}
