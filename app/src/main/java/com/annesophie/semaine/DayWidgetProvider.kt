package com.annesophie.semaine

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.RemoteViews

class DayWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        ids.forEach { updateWidget(context, manager, it) }
        manager.notifyAppWidgetViewDataChanged(ids, R.id.task_list)
    }

    override fun onDeleted(context: Context, ids: IntArray) {
        ids.forEach { TaskStore.removeWidget(context, it) }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_TOGGLE) {
            val day = Day.from(intent.getStringExtra(EXTRA_DAY))
            val taskId = intent.getStringExtra(EXTRA_TASK_ID) ?: return
            TaskStore.toggle(context, day, taskId)
            refreshAll(context)
        }
    }

    companion object {
        const val ACTION_TOGGLE = "com.annesophie.semaine.TOGGLE"
        const val EXTRA_DAY = "day"
        const val EXTRA_TASK_ID = "task_id"

        /** Met à jour tous les widgets posés sur l'écran d'accueil. */
        fun refreshAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, DayWidgetProvider::class.java))
            ids.forEach { updateWidget(context, manager, it) }
            manager.notifyAppWidgetViewDataChanged(ids, R.id.task_list)
        }

        fun updateWidget(context: Context, manager: AppWidgetManager, widgetId: Int) {
            val day = TaskStore.getWidgetDay(context, widgetId)
            val views = RemoteViews(context.packageName, R.layout.widget_day)

            // En-tête : jour (avec un point si c'est aujourd'hui) + nombre de tâches restantes
            val isToday = day == Day.today()
            views.setTextViewText(R.id.day_title, if (isToday) "● ${day.short}" else day.short)
            views.setTextColor(R.id.day_title, day.color)
            views.setTextViewText(R.id.day_count, TaskStore.remaining(context, day).toString())

            // Liste des tâches
            val serviceIntent = Intent(context, DayWidgetService::class.java).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
                putExtra(EXTRA_DAY, day.name)
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
                data = Uri.parse("semaine://add/$widgetId")
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
                data = Uri.parse("semaine://open/$widgetId")
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
