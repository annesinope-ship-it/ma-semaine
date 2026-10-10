package com.annesophie.semaine

import android.content.Context
import java.time.DayOfWeek
import java.time.LocalDate

/**
 * Les jours affichables par un widget.
 * WEEKEND regroupe ses propres tâches + celles de SAT et SUN.
 * offset = position dans la semaine (lundi = 0), utilisé pour les tâches "1er du mois".
 */
enum class Day(val label: String, val short: String, val color: Int, val offset: Long) {
    MON("Lundi", "LUN", 0xFFF5A623.toInt(), 0),
    TUE("Mardi", "MAR", 0xFF9B6BD6.toInt(), 1),
    WED("Mercredi", "MER", 0xFF4A90E2.toInt(), 2),
    THU("Jeudi", "JEU", 0xFF1FA59A.toInt(), 3),
    FRI("Vendredi", "VEN", 0xFF8E6CC9.toInt(), 4),
    WEEKEND("Week-end", "WEEK-END", 0xFFF07C2A.toInt(), 6),
    SAT("Samedi", "SAM", 0xFFE0648C.toInt(), 5),
    SUN("Dimanche", "DIM", 0xFF7A2E3B.toInt(), 6);

    /** Les listes de tâches affichées par ce jour. */
    val sources: List<Day>
        get() = if (this == WEEKEND) listOf(SAT, SUN, WEEKEND) else listOf(this)

    /** Date de ce jour dans la semaine en cours (lundi → dimanche). */
    fun dateThisWeek(): LocalDate = weekStart().plusDays(offset)

    fun isToday(): Boolean {
        val t = today()
        return this == t || (this == WEEKEND && (t == SAT || t == SUN))
    }

    companion object {
        fun from(name: String?): Day = values().firstOrNull { it.name == name } ?: MON

        fun today(): Day = when (LocalDate.now().dayOfWeek) {
            DayOfWeek.MONDAY -> MON
            DayOfWeek.TUESDAY -> TUE
            DayOfWeek.WEDNESDAY -> WED
            DayOfWeek.THURSDAY -> THU
            DayOfWeek.FRIDAY -> FRI
            DayOfWeek.SATURDAY -> SAT
            else -> SUN
        }

        /** Lundi de la semaine en cours. */
        fun weekStart(): LocalDate {
            val now = LocalDate.now()
            return now.minusDays((now.dayOfWeek.value - 1).toLong())
        }
    }
}

fun Context.dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
