package com.annesophie.semaine

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.util.UUID

/**
 * Une tâche.
 * - routine = true : revient chaque semaine, se décoche automatiquement le lundi
 *   (cochée = cochée depuis le lundi de la semaine en cours).
 * - nth > 0 : n'apparaît que la n-ième semaine du mois (ex. 1er dimanche du mois).
 */
data class Task(
    val id: String,
    val text: String,
    val done: Boolean,
    val routine: Boolean = false,
    val doneOn: String = "",
    val nth: Int = 0
) {
    fun isDone(): Boolean =
        if (routine) doneOn.isNotEmpty() && doneOn >= Day.weekStart().toString() else done

    /** Texte affiché, avec la mention "1er du mois" si besoin. */
    fun displayText(): String = when (nth) {
        0 -> text
        1 -> "$text (1er du mois)"
        else -> "$text (${nth}e du mois)"
    }

    /** Visible cette semaine ? (toujours vrai sauf pour les tâches "n-ième du mois") */
    fun isVisibleOn(date: LocalDate): Boolean {
        if (nth <= 0) return true
        val dom = date.dayOfMonth
        return dom in ((nth - 1) * 7 + 1)..(nth * 7)
    }
}

/** Une tâche avec le jour (la liste) auquel elle appartient. */
data class Entry(val day: Day, val task: Task) {
    val done: Boolean get() = task.isDone()
}

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
            Task(
                id = o.getString("id"),
                text = o.getString("text"),
                done = o.optBoolean("done", false),
                routine = o.optBoolean("routine", false),
                doneOn = o.optString("doneOn", ""),
                nth = o.optInt("nth", 0)
            )
        }
    }

    private fun save(c: Context, day: Day, tasks: List<Task>) {
        val arr = JSONArray()
        tasks.forEach {
            arr.put(
                JSONObject()
                    .put("id", it.id)
                    .put("text", it.text)
                    .put("done", it.done)
                    .put("routine", it.routine)
                    .put("doneOn", it.doneOn)
                    .put("nth", it.nth)
            )
        }
        prefs(c).edit().putString("tasks_${day.name}", arr.toString()).commit()
    }

    /**
     * Tâches affichées pour un jour (le week-end regroupe samedi + dimanche + week-end).
     * onlyThisWeek = masque les tâches "n-ième du mois" qui ne tombent pas cette semaine.
     */
    fun entries(c: Context, day: Day, onlyThisWeek: Boolean): List<Entry> =
        day.sources.flatMap { src ->
            val date = src.dateThisWeek()
            getTasks(c, src)
                .filter { !onlyThisWeek || it.isVisibleOn(date) }
                .map { Entry(src, it) }
        }

    fun add(c: Context, day: Day, text: String, routine: Boolean = false, nth: Int = 0) {
        val clean = text.trim()
        if (clean.isEmpty()) return
        save(c, day, getTasks(c, day) + Task(UUID.randomUUID().toString(), clean, false, routine, "", nth))
    }

    fun toggle(c: Context, day: Day, id: String) {
        val today = LocalDate.now().toString()
        save(c, day, getTasks(c, day).map {
            when {
                it.id != id -> it
                it.routine -> it.copy(doneOn = if (it.isDone()) "" else today)
                else -> it.copy(done = !it.done)
            }
        })
    }

    fun delete(c: Context, day: Day, id: String) {
        save(c, day, getTasks(c, day).filter { it.id != id })
    }

    /** Efface les tâches ponctuelles faites (la routine n'est jamais effacée). */
    fun clearDone(c: Context, day: Day) {
        day.sources.forEach { src ->
            save(c, src, getTasks(c, src).filter { it.routine || !it.done })
        }
    }

    /** Supprime la routine de ces jours (avant d'en coller une nouvelle). */
    fun clearRoutine(c: Context, days: Collection<Day>) {
        days.forEach { d -> save(c, d, getTasks(c, d).filter { !it.routine }) }
    }

    fun remaining(c: Context, day: Day): Int = entries(c, day, true).count { !it.done }

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
