package com.annesophie.semaine

import android.content.Context
import java.util.Calendar

enum class Day(val label: String, val short: String, val color: Int) {
    MON("Lundi", "LUN", 0xFFF5A623.toInt()),
    TUE("Mardi", "MAR", 0xFF9B6BD6.toInt()),
    WED("Mercredi", "MER", 0xFF4A90E2.toInt()),
    THU("Jeudi", "JEU", 0xFF1FA59A.toInt()),
    FRI("Vendredi", "VEN", 0xFF8E6CC9.toInt()),
    WEEKEND("Week-end", "WEEK-END", 0xFFF07C2A.toInt());

    companion object {
        fun from(name: String?): Day = values().firstOrNull { it.name == name } ?: MON

        fun today(): Day = when (Calendar.getInstance().get(Calendar.DAY_OF_WEEK)) {
            Calendar.MONDAY -> MON
            Calendar.TUESDAY -> TUE
            Calendar.WEDNESDAY -> WED
            Calendar.THURSDAY -> THU
            Calendar.FRIDAY -> FRI
            else -> WEEKEND
        }
    }
}

fun Context.dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
