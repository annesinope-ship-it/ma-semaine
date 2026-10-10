package com.annesophie.semaine

import java.text.Normalizer

/**
 * Transforme un texte collé en tâches par jour.
 *
 * Formats acceptés :
 *   lun : courses
 *   ven : dentiste
 *   we : run club, film
 *
 * ou bien un jour seul suivi de tâches sur les lignes d'après :
 *   Lundi
 *   - courses
 *   - pressing
 *
 * Une ligne sans jour va dans le dernier jour cité (ou le jour affiché dans l'appli).
 * Plusieurs tâches sur une ligne : séparées par des virgules ou des points-virgules.
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
        "sam" to Day.WEEKEND, "samedi" to Day.WEEKEND, "sat" to Day.WEEKEND, "saturday" to Day.WEEKEND,
        "dim" to Day.WEEKEND, "dimanche" to Day.WEEKEND, "sun" to Day.WEEKEND, "sunday" to Day.WEEKEND
    )

    private fun normalize(s: String): String =
        Normalizer.normalize(s.lowercase().trim(), Normalizer.Form.NFD)
            .replace(Regex("\\p{M}+"), "")
            .replace(Regex("[.\\s]+$"), "")
            .trim()

    private fun dayOf(word: String): Day? = aliases[normalize(word)]

    private fun cleanTask(s: String): String =
        s.trim().trimStart('-', '•', '*', '–', '—', '·', '☐', '□', ' ').trim()

    private fun splitTasks(s: String): List<String> =
        s.split(',', ';').map { cleanTask(it) }.filter { it.isNotEmpty() }

    fun parse(text: String, defaultDay: Day): List<Pair<Day, String>> {
        val result = mutableListOf<Pair<Day, String>>()
        var currentDay = defaultDay

        text.lines().forEach { rawLine ->
            val line = cleanTask(rawLine)
            if (line.isEmpty()) return@forEach

            // "jour : tâches" (deux-points ou tiret comme séparateur)
            val sep = Regex("^([^:–—]{1,12}?)\\s*[:–—]\\s*(.*)$").find(line)
            if (sep != null) {
                val day = dayOf(sep.groupValues[1])
                if (day != null) {
                    currentDay = day
                    splitTasks(sep.groupValues[2]).forEach { result.add(day to it) }
                    return@forEach
                }
            }

            // Ligne qui ne contient qu'un jour : "Lundi"
            val alone = dayOf(line)
            if (alone != null) {
                currentDay = alone
                return@forEach
            }

            // Sinon : tâche(s) pour le jour en cours
            splitTasks(line).forEach { result.add(currentDay to it) }
        }
        return result
    }
}
