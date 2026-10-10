package com.annesophie.semaine

import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import android.widget.RemoteViewsService

class DayWidgetService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory =
        TaskFactory(
            applicationContext,
            Day.from(intent.getStringExtra(DayWidgetProvider.EXTRA_DAY)),
            intent.getBooleanExtra(DayWidgetProvider.EXTRA_TODAY, false)
        )
}

class TaskFactory(
    private val context: Context,
    private val fixedDay: Day,
    private val todayMode: Boolean
) : RemoteViewsService.RemoteViewsFactory {

    private var rows: List<Row> = emptyList()
    private var bg: Int = Palette.DEFAULT
    private var accent: Int = 0xFF8A8A8A.toInt()

    private fun load() {
        // Le widget Aujourd'hui recalcule le jour à chaque mise à jour (passage de minuit)
        val day = if (todayMode) Day.today() else fixedDay
        val sources = if (todayMode) Day.todaySources() else day.sources
        bg = TaskStore.getColor(context, day)
        accent = Palette.titleColor(bg, day)
        rows = TaskStore.rows(context, sources, true)
    }

    override fun onCreate() = load()

    override fun onDataSetChanged() = load()

    override fun onDestroy() {}

    override fun getCount(): Int = rows.size

    override fun getViewAt(position: Int): RemoteViews {
        return when (val row = rows.getOrNull(position)) {
            is Row.Header -> {
                val views = RemoteViews(context.packageName, R.layout.widget_header)
                views.setTextViewText(R.id.header_text, row.text)
                views.setTextColor(R.id.header_text, if (row.big) accent else Palette.mutedColor(bg))
                views.setTextViewTextSize(
                    R.id.header_text, android.util.TypedValue.COMPLEX_UNIT_SP, if (row.big) 12f else 11f
                )
                views
            }
            is Row.Item -> {
                val views = RemoteViews(context.packageName, R.layout.widget_item)
                val entry = row.entry
                views.setTextViewText(R.id.item_text, entry.task.displayText())
                views.setTextColor(R.id.item_text, Palette.textColor(bg, entry.done))
                views.setImageViewResource(
                    R.id.item_check,
                    if (entry.done) R.drawable.ic_check_on else R.drawable.ic_check_off
                )
                val fill = Intent().apply {
                    putExtra(DayWidgetProvider.EXTRA_DAY, entry.day.name)
                    putExtra(DayWidgetProvider.EXTRA_TASK_ID, entry.task.id)
                }
                views.setOnClickFillInIntent(R.id.item_root, fill)
                views
            }
            null -> RemoteViews(context.packageName, R.layout.widget_item)
        }
    }

    override fun getLoadingView(): RemoteViews? = null

    override fun getViewTypeCount(): Int = 2

    override fun getItemId(position: Int): Long = position.toLong()

    override fun hasStableIds(): Boolean = false
}
