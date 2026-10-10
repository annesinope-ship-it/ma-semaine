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

    private var tasks: List<Task> = emptyList()

    // Tâches à faire d'abord, tâches faites en bas
    private fun load(): List<Task> = TaskStore.getTasks(context, day).sortedBy { it.done }

    override fun onCreate() {
        tasks = load()
    }

    override fun onDataSetChanged() {
        tasks = load()
    }

    override fun onDestroy() {}

    override fun getCount(): Int = tasks.size

    override fun getViewAt(position: Int): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_item)
        val task = tasks.getOrNull(position) ?: return views

        views.setTextViewText(R.id.item_text, task.text)
        views.setTextColor(
            R.id.item_text,
            if (task.done) 0xFFAAAAAA.toInt() else 0xFF222222.toInt()
        )
        views.setImageViewResource(
            R.id.item_check,
            if (task.done) R.drawable.ic_check_on else R.drawable.ic_check_off
        )

        val fill = Intent().apply {
            putExtra(DayWidgetProvider.EXTRA_DAY, day.name)
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
