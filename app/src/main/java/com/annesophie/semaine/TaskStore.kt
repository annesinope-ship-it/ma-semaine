package com.annesophie.semaine

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class Task(val id: String, val text: String, val done: Boolean)

/** Stockage local des tâches (sur le téléphone uniquement). */
object TaskStore {

    private fun prefs(c: Context) = c.getSharedPreferences("ma_semaine", Context.MODE_PRIVATE)

    fun getTasks(c: Context, day: Day): List<Task> {
        val arr = try {
            JSONArray(prefs(c).getString("tasks_${day.name}", "[]"))
        } catch (e: Exception) {
            JSONArray()
        }
        return (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            Task(o.getString("id"), o.getString("text"), o.optBoolean("done", false))
        }
    }

    private fun save(c: Context, day: Day, tasks: List<Task>) {
        val arr = JSONArray()
        tasks.forEach {
            arr.put(JSONObject().put("id", it.id).put("text", it.text).put("done", it.done))
        }
        prefs(c).edit().putString("tasks_${day.name}", arr.toString()).commit()
    }

    fun add(c: Context, day: Day, text: String) {
        val clean = text.trim()
        if (clean.isEmpty()) return
        save(c, day, getTasks(c, day) + Task(UUID.randomUUID().toString(), clean, false))
    }

    fun toggle(c: Context, day: Day, id: String) {
        save(c, day, getTasks(c, day).map { if (it.id == id) it.copy(done = !it.done) else it })
    }

    fun delete(c: Context, day: Day, id: String) {
        save(c, day, getTasks(c, day).filter { it.id != id })
    }

    fun clearDone(c: Context, day: Day) {
        save(c, day, getTasks(c, day).filter { !it.done })
    }

    fun remaining(c: Context, day: Day): Int = getTasks(c, day).count { !it.done }

    // Quel jour affiche chaque widget
    fun setWidgetDay(c: Context, widgetId: Int, day: Day) {
        prefs(c).edit().putString("widget_$widgetId", day.name).commit()
    }

    fun getWidgetDay(c: Context, widgetId: Int): Day =
        Day.from(prefs(c).getString("widget_$widgetId", null))

    fun removeWidget(c: Context, widgetId: Int) {
        prefs(c).edit().remove("widget_$widgetId").commit()
    }
}
