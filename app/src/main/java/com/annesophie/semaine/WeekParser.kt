package com.annesophie.semaine

import java.text.Normalizer

data class ParsedTask(val day: Day, val text: String, val nth: Int = 0)

/**
 * Transforme un texte collé en tâches par jour.
 *
 * Format court (plusieurs tâches séparées par des virgules) :
 *   lun : courses
 *   we : run club, film
 *
 * Format liste (une tâche par ligne, les virgules sont gardées) :
 *   Lundi
 *   - ☀ Lit fait, vaisselle, surfaces cuisine
 *   - ☾ Exfoliant AHA/BHA
 *
 * Tâche mensuelle : "- 1er du mois : Shampoing clarifiant" (ou 2e, 3e, 4e).
 * Une ligne sans jour va dans le dernier jour cité (ou le jour affiché dans l'appli).
 */
object WeekParser {

    private val aliases: Map<String, Day> = mapOf(
        "lun" to Day.MON, "lundi" to Day.MON, "mon" to Day.MON, "monday" to Day.MON,
        "mar" to Day.TUE, "mardi" to Day.TUE, "tue" to Day.TUE, "tuesday" to Day.TUE,
        "mer" to Day.WED, "mercredi" to Day.WED, "wed" to Day.WED, "wednesday" to Day.WED,
        "jeu" to Day.THU, "jeudi" to Day.THU, "thu" to Day.THU, "thursday" to Day.THU,
        "ven" to Day.FRI, "vendredi" to Day.FRI, "fri" to Day.FRI, "friday" to Day.FRI,
        "we" to Day.WEEKEND, "w-e" to Day.WEEKEND, "weekend" to Day.WEEKEND,
        "week-end" to Day.WEEKEND, "week end" to Day.WEEKEND,
        "sam" to Day.SAT, "samedi" to Day.SAT, "sat" to Day.SAT, "saturday" to Day.SAT,
        "dim" to Day.SUN, "dimanche" to Day.SUN, "sun" to Day.SUN, "sunday" to Day.SUN
    )

    private val monthly = Regex(
        "^(\\d)\\s*(?:er|re|e|eme|ème)?\\s+(?:dimanche\\s+|dim\\.?\\s+)?du\\s+mois\\s*[:–—-]\\s*(.+)$",
        RegexOption.IGNORE_CASE
    )

    private fun normalize(s: String): String =
        Normalizer.normalize(s.lowercase().trim(), Normalizer.Form.NFD)
            .replace(Regex("\\p{M}+"), "")
            .replace(Regex("[.\\s]+$"), "")
            .trim()

    private fun dayOf(word: String): Day? = aliases[normalize(word)]

    private fun cleanTask(s: String): String =
        s.trim().trimStart('-', '•', '*', '–', '—', '·', '☐', '□', ' ').trim()

    private fun toTask(day: Day, raw: String): ParsedTask? {
        val t = cleanTask(raw)
        if (t.isEmpty()) return null
        val m = monthly.find(t)
        if (m != null) {
            val n = m.groupValues[1].toIntOrNull()?.coerceIn(1, 5) ?: 0
            return ParsedTask(day, m.groupValues[2].trim(), n)
        }
        return ParsedTask(day, t)
    }

    fun parse(text: String, defaultDay: Day): List<ParsedTask> {
        val result = mutableListOf<ParsedTask>()
        var currentDay = defaultDay

        text.lines().forEach { rawLine ->
            val line = cleanTask(rawLine)
            if (line.isEmpty()) return@forEach
            // Ligne de titre "ROUTINE" : ignorée
            if (normalize(line).startsWith("routine")) return@forEach

            // Format court "jour : tâche, tâche"
            val sep = Regex("^([^:–—]{1,12}?)\\s*[:–—]\\s*(.*)$").find(line)
            if (sep != null) {
                val day = dayOf(sep.groupValues[1])
                if (day != null) {
                    currentDay = day
                    sep.groupValues[2].split(',', ';').forEach { part ->
                        toTask(day, part)?.let { result.add(it) }
                    }
                    return@forEach
                }
            }

            // Ligne qui ne contient qu'un jour : "Lundi"
            val alone = dayOf(line)
            if (alone != null) {
                currentDay = alone
                return@forEach
            }

            // Sinon : une tâche entière pour le jour en cours
            toTask(currentDay, line)?.let { result.add(it) }
        }
        return result
    }
}
