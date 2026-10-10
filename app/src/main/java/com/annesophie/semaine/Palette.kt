package com.annesophie.semaine

import android.graphics.Color

/** Couleurs proposées pour le fond des cases (ARGB). */
object Palette {

    val colors: List<Pair<String, Int>> = listOf(
        "Blanc" to 0xFFFFFFFF.toInt(),
        "Crème" to 0xFFF5EFE6.toInt(),
        "Lin" to 0xFFEADFCF.toInt(),
        "Rose poudré" to 0xFFF6DCDC.toInt(),
        "Vieux rose" to 0xFFE3B5B5.toInt(),
        "Bordeaux" to 0xFF7A2E3B.toInt(),
        "Pêche" to 0xFFFBE1CC.toInt(),
        "Jaune pâle" to 0xFFFFF3C4.toInt(),
        "Sauge" to 0xFFCFDAC5.toInt(),
        "Vert sapin" to 0xFF2F4A3A.toInt(),
        "Bleu ciel" to 0xFFDCE8F5.toInt(),
        "Bleu nuit" to 0xFF1F2A44.toInt(),
        "Lavande" to 0xFFE6DDF3.toInt(),
        "Gris perle" to 0xFFE9E9EB.toInt(),
        "Noir" to 0xFF1C1C1E.toInt(),
        "Verre clair" to 0x99FFFFFF.toInt(),
        "Verre sombre" to 0x99000000.toInt()
    )

    const val DEFAULT = 0xFFFFFFFF.toInt()

    /** Fond sombre → texte clair. */
    fun isDark(color: Int): Boolean {
        val lum = (0.299 * Color.red(color) + 0.587 * Color.green(color) + 0.114 * Color.blue(color)) / 255
        return lum < 0.55
    }

    fun opaque(color: Int): Int = color or 0xFF000000.toInt()

    fun textColor(bg: Int, done: Boolean = false): Int = when {
        isDark(bg) && done -> 0x99FFFFFF.toInt()
        isDark(bg) -> 0xFFFFFFFF.toInt()
        done -> 0xFFAAAAAA.toInt()
        else -> 0xFF222222.toInt()
    }

    fun mutedColor(bg: Int): Int = if (isDark(bg)) 0xB3FFFFFF.toInt() else 0xFF9A9A9A.toInt()

    /** Couleur du nom du jour : couleur du jour sur fond clair, blanc sur fond sombre. */
    fun titleColor(bg: Int, day: Day): Int = if (isDark(bg)) 0xFFFFFFFF.toInt() else day.color
}
