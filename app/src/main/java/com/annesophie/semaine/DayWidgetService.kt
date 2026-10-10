package com.annesophie.semaine

import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import android.widget.RemoteViewsService

class DayWidgetService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory =
        TaskFactory(applicationContext, Day.from(intent.getStringExtra(DayWidgetProvider.EXTRA_DAY)))
}

class TaskFactory(
    private val context: Context,
    private val day: Day
) : RemoteViewsService.RemoteViewsFactory {

    private var entries: List<Entry> = emptyList()

    // Tâches à faire d'abord, tâches faites en bas (l'ordre d'origine est gardé sinon)
    private fun load(): List<Entry> = TaskStore.entries(context, day, true).sortedBy { it.done }

    override fun onCreate() {
        entries = load()
    }

    override fun onDataSetChanged() {
        entries = load()
    }

    override fun onDestroy() {}

    override fun getCount(): Int = entries.size

    override fun getViewAt(position: Int): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_item)
        val entry = entries.getOrNull(position) ?: return views
        val task = entry.task

        // Dans le widget Week-end, on précise Sam / Dim
        val prefix = if (entry.day != day && entry.day != Day.WEEKEND) {
            entry.day.short.lowercase().replaceFirstChar { it.uppercase() } + " · "
        } else {
            ""
        }
        views.setTextViewText(R.id.item_text, prefix + task.displayText())
        views.setTextColor(
            R.id.item_text,
            if (entry.done) 0xFFAAAAAA.toInt() else 0xFF222222.toInt()
        )
        views.setImageViewResource(
            R.id.item_check,
            if (entry.done) R.drawable.ic_check_on else R.drawable.ic_check_off
        )

        val fill = Intent().apply {
            putExtra(DayWidgetProvider.EXTRA_DAY, entry.day.name)
            putExtra(DayWidgetProvider.EXTRA_TASK_ID, task.id)
        }
        views.setOnClickFillInIntent(R.id.item_root, fill)
        return views
    }

    override fun getLoadingView(): RemoteViews? = null

    override fun getViewTypeCount(): Int = 1

    override fun getItemId(position: Int): Long = position.toLong()

    override fun hasStableIds(): Boolean = false
}
