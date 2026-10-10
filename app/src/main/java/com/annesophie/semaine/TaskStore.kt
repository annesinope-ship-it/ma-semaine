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

    /** Matin (☀), soir (☾) ou sans moment, selon le symbole en début de tâche. */
    fun period(): Period {
        val t = text.trimStart()
        return when {
            t.startsWith("☀") -> Period.MORNING
            t.startsWith("☾") || t.startsWith("🌙") -> Period.EVENING
            else -> Period.NONE
        }
    }

    /** Texte sans le symbole ☀ / ☾ du début. */
    fun cleanText(): String = text.replace(LEADING_SYMBOLS, "").ifBlank { text }

    /** Texte affiché, avec la mention "1er du mois" si besoin. */
    fun displayText(): String = when (nth) {
        0 -> cleanText()
        1 -> "${cleanText()} (1er du mois)"
        else -> "${cleanText()} (${nth}e du mois)"
    }

    /** Visible cette semaine ? (toujours vrai sauf pour les tâches "n-ième du mois") */
    fun isVisibleOn(date: LocalDate): Boolean {
        if (nth <= 0) return true
        val dom = date.dayOfMonth
        return dom in ((nth - 1) * 7 + 1)..(nth * 7)
    }
}

private val LEADING_SYMBOLS = Regex("^(?:☀|☾|🌙|\\uFE0F|\\s)+")

enum class Period(val label: String) {
    NONE(""),
    MORNING("Matin"),
    EVENING("Soir")
}

/** Une ligne affichée : intertitre (jour ou moment) ou tâche. */
sealed class Row {
    data class Header(val text: String, val big: Boolean) : Row()
    data class Item(val entry: Entry) : Row()
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
        entries(c, day.sources, onlyThisWeek)

    fun entries(c: Context, sources: List<Day>, onlyThisWeek: Boolean): List<Entry> =
        sources.flatMap { src ->
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

    fun remaining(c: Context, sources: List<Day>): Int = entries(c, sources, true).count { !it.done }

    /**
     * Lignes à afficher, regroupées :
     * - par jour si plusieurs listes (week-end : Samedi / Dimanche / Week-end)
     * - puis par moment (À faire / ☀ Matin / ☾ Soir) dès qu'il y a des tâches matin ou soir.
     * Dans chaque groupe : à faire d'abord, faites en bas.
     */
    fun rows(c: Context, sources: List<Day>, onlyThisWeek: Boolean): List<Row> {
        val multi = sources.size > 1
        val out = mutableListOf<Row>()
        sources.forEach { src ->
            val list = entries(c, listOf(src), onlyThisWeek)
            if (list.isEmpty()) return@forEach
            if (multi) out.add(Row.Header(src.label, true))
            val groups = list.groupBy { it.task.period() }
            val tagged = groups.keys.any { it != Period.NONE }
            Period.values().forEach { p ->
                val group = groups[p] ?: return@forEach
                if (tagged && p != Period.NONE) out.add(Row.Header(p.label, false))
                group.sortedBy { it.done }.forEach { out.add(Row.Item(it)) }
            }
        }
        return out
    }

    // Motif décoratif de la case d'un jour
    fun getDecor(c: Context, day: Day): String = prefs(c).getString("decor_${day.name}", "") ?: ""

    fun setDecor(c: Context, day: Day, key: String) {
        prefs(c).edit().putString("decor_${day.name}", key).commit()
    }

    // Rappel du soir
    fun reminderEnabled(c: Context): Boolean = prefs(c).getBoolean("reminder_on", true)

    fun reminderMinutes(c: Context): Int = prefs(c).getInt("reminder_min", 21 * 60)

    fun setReminder(c: Context, enabled: Boolean, minutes: Int) {
        prefs(c).edit().putBoolean("reminder_on", enabled).putInt("reminder_min", minutes).commit()
    }

    // Couleur de fond de la case d'un jour
    fun getColor(c: Context, day: Day): Int = prefs(c).getInt("color_${day.name}", Palette.DEFAULT)

    fun setColor(c: Context, day: Day, color: Int) {
        prefs(c).edit().putInt("color_${day.name}", color).commit()
    }

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
