package com.annesophie.semaine

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.view.View
import android.widget.RemoteViews
import java.time.LocalDate
import java.time.ZoneId

/** Widget « un jour de la semaine » (le jour est choisi à la pose du widget). */
class DayWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        refreshAll(context)
    }

    override fun onDeleted(context: Context, ids: IntArray) {
        ids.forEach { TaskStore.removeWidget(context, it) }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        when (intent.action) {
            ACTION_TOGGLE -> {
                val day = Day.from(intent.getStringExtra(EXTRA_DAY))
                val taskId = intent.getStringExtra(EXTRA_TASK_ID) ?: return
                TaskStore.toggle(context, day, taskId)
                refreshAll(context)
            }
            ACTION_REFRESH -> refreshAll(context)
        }
    }

    companion object {
        const val ACTION_TOGGLE = "com.annesophie.semaine.TOGGLE"
        const val ACTION_REFRESH = "com.annesophie.semaine.REFRESH"
        const val EXTRA_DAY = "day"
        const val EXTRA_TASK_ID = "task_id"
        const val EXTRA_TODAY = "today_mode"

        /** Met à jour tous les widgets (jours + Aujourd'hui) et programme le passage de minuit. */
        fun refreshAll(context: Context) {
            RoutineDefaults.seedIfNeeded(context)
            val manager = AppWidgetManager.getInstance(context)
            val dayIds = manager.getAppWidgetIds(ComponentName(context, DayWidgetProvider::class.java))
            val todayIds = manager.getAppWidgetIds(ComponentName(context, TodayWidgetProvider::class.java))
            dayIds.forEach { render(context, manager, it, false) }
            todayIds.forEach { render(context, manager, it, true) }
            manager.notifyAppWidgetViewDataChanged(dayIds + todayIds, R.id.task_list)
            scheduleMidnight(context)
        }

        /** Réveil juste après minuit : nouveau jour (widget Aujourd'hui, ●, remise à zéro du lundi). */
        fun scheduleMidnight(context: Context) {
            val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val next = LocalDate.now().plusDays(1).atStartOfDay(ZoneId.systemDefault())
                .plusMinutes(1).toInstant().toEpochMilli()
            val pi = PendingIntent.getBroadcast(
                context, 9001,
                Intent(context, DayWidgetProvider::class.java).setAction(ACTION_REFRESH),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            am.setAndAllowWhileIdle(AlarmManager.RTC, next, pi)
        }

        fun render(context: Context, manager: AppWidgetManager, widgetId: Int, todayMode: Boolean) {
            val day = if (todayMode) Day.today() else TaskStore.getWidgetDay(context, widgetId)
            val sources = if (todayMode) Day.todaySources() else day.sources
            val views = RemoteViews(context.packageName, R.layout.widget_day)

            // Couleurs de la case
            val bg = TaskStore.getColor(context, day)
            val accent = Palette.titleColor(bg, day)
            views.setInt(R.id.widget_bg, "setColorFilter", Palette.opaque(bg))
            views.setInt(R.id.widget_bg, "setImageAlpha", Color.alpha(bg))
            views.setTextColor(R.id.day_title, accent)
            views.setTextColor(R.id.day_count, Palette.textColor(bg))
            views.setTextColor(R.id.empty_view, Palette.mutedColor(bg))
            views.setTextColor(R.id.add_button, Palette.mutedColor(bg))

            // Motif décoratif
            val decor = Palette.decorRes(TaskStore.getDecor(context, day))
            if (decor != 0) {
                views.setViewVisibility(R.id.deco, View.VISIBLE)
                views.setImageViewResource(R.id.deco, decor)
                views.setInt(R.id.deco, "setColorFilter", accent)
                views.setInt(R.id.deco, "setImageAlpha", 70)
            } else {
                views.setViewVisibility(R.id.deco, View.GONE)
            }

            // En-tête : nom du jour + nombre de tâches restantes
            val title = when {
                todayMode -> "AUJOURD'HUI · ${day.label.uppercase()}"
                day.isToday() -> "● ${day.short}"
                else -> day.short
            }
            views.setTextViewText(R.id.day_title, title)
            views.setTextViewText(R.id.day_count, TaskStore.remaining(context, sources).toString())

            // Liste des tâches
            val serviceIntent = Intent(context, DayWidgetService::class.java).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
                putExtra(EXTRA_DAY, day.name)
                putExtra(EXTRA_TODAY, todayMode)
                data = Uri.parse(toUri(Intent.URI_INTENT_SCHEME))
            }
            views.setRemoteAdapter(R.id.task_list, serviceIntent)
            views.setEmptyView(R.id.task_list, R.id.empty_view)

            // Toucher une tâche = la cocher / décocher
            val toggleIntent = Intent(context, DayWidgetProvider::class.java).setAction(ACTION_TOGGLE)
            views.setPendingIntentTemplate(
                R.id.task_list,
                PendingIntent.getBroadcast(
                    context, widgetId, toggleIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
                )
            )

            // "+ Ajouter" (et zone vide) = petite fenêtre de saisie
            val addIntent = Intent(context, AddTaskActivity::class.java).apply {
                putExtra(EXTRA_DAY, day.name)
                data = Uri.parse("semaine://add/$widgetId/${day.name}")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            val addPending = PendingIntent.getActivity(
                context, widgetId, addIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.add_button, addPending)
            views.setOnClickPendingIntent(R.id.empty_view, addPending)

            // Toucher l'en-tête = ouvrir l'appli sur ce jour
            val openIntent = Intent(context, MainActivity::class.java).apply {
                putExtra(EXTRA_DAY, day.name)
                data = Uri.parse("semaine://open/$widgetId/${day.name}")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            views.setOnClickPendingIntent(
                R.id.header,
                PendingIntent.getActivity(
                    context, widgetId + 100000, openIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            )

            manager.updateAppWidget(widgetId, views)
        }
    }
}

/** Widget « Aujourd'hui » : affiche toujours le jour en cours. */
class TodayWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        DayWidgetProvider.refreshAll(context)
    }
}
