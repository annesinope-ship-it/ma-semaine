package com.annesophie.semaine

import android.graphics.Color

/** Couleurs et motifs proposés pour les cases. */
object Palette {

    private const val ROSE = 0xFFF6DCDC.toInt()
    private const val LAVANDE = 0xFFE6DDF3.toInt()
    private const val PECHE = 0xFFFBE1CC.toInt()
    private const val MENTHE = 0xFFD6EEE3.toInt()
    private const val SAUGE = 0xFFCFDAC5.toInt()
    private const val TERRACOTTA = 0xFFEFC7B5.toInt()
    private const val MIEL = 0xFFF5E6C4.toInt()

    val colors: List<Pair<String, Int>> = listOf(
        "Blanc" to 0xFFFFFFFF.toInt(),
        "Crème" to 0xFFF5EFE6.toInt(),
        "Lin" to 0xFFEADFCF.toInt(),
        "Gris perle" to 0xFFE9E9EB.toInt(),
        "Rose poudré" to ROSE,
        "Vieux rose" to 0xFFE3B5B5.toInt(),
        "Lavande" to LAVANDE,
        "Pêche" to PECHE,
        "Terracotta" to TERRACOTTA,
        "Miel" to MIEL,
        "Jaune pâle" to 0xFFFFF3C4.toInt(),
        "Menthe" to MENTHE,
        "Sauge" to SAUGE,
        "Bleu ciel" to 0xFFDCE8F5.toInt(),
        "Bordeaux" to 0xFF7A2E3B.toInt(),
        "Vert sapin" to 0xFF2F4A3A.toInt(),
        "Bleu nuit" to 0xFF1F2A44.toInt(),
        "Noir" to 0xFF1C1C1E.toInt(),
        "Verre clair" to 0x99FFFFFF.toInt(),
        "Verre sombre" to 0x99000000.toInt()
    )

    /** Thème « douceur champêtre » : une couleur pastel par jour. */
    val champetre: Map<Day, Int> = mapOf(
        Day.MON to ROSE,
        Day.TUE to LAVANDE,
        Day.WED to PECHE,
        Day.THU to MENTHE,
        Day.FRI to SAUGE,
        Day.SAT to TERRACOTTA,
        Day.SUN to MIEL,
        Day.WEEKEND to TERRACOTTA
    )

    /** Motifs décoratifs : clé enregistrée → (nom, dessin). */
    val decors: List<Triple<String, String, Int>> = listOf(
        Triple("", "Aucun", 0),
        Triple("brin", "Brin", R.drawable.deco_brin),
        Triple("fleur", "Fleur", R.drawable.deco_fleur),
        Triple("chat", "Chat", R.drawable.deco_chat),
        Triple("patte", "Patte", R.drawable.deco_patte),
        Triple("lune", "Lune", R.drawable.deco_lune)
    )

    fun decorRes(key: String): Int = decors.firstOrNull { it.first == key }?.third ?: 0

    fun decorName(key: String): String = decors.firstOrNull { it.first == key }?.second ?: "Aucun"

    const val DEFAULT = 0xFFFFFFFF.toInt()

    // Fonds neutres : le nom du jour garde sa couleur vive
    private val neutrals = setOf(
        0xFFFFFFFF.toInt(), 0xFFF5EFE6.toInt(), 0xFFEADFCF.toInt(),
        0xFFE9E9EB.toInt(), 0x99FFFFFF.toInt()
    )

    /** Fond sombre → texte clair. */
    fun isDark(color: Int): Boolean {
        val lum = (0.299 * Color.red(color) + 0.587 * Color.green(color) + 0.114 * Color.blue(color)) / 255
        return lum < 0.55
    }

    fun opaque(color: Int): Int = color or 0xFF000000.toInt()

    fun textColor(bg: Int, done: Boolean = false): Int = when {
        isDark(bg) && done -> 0x99FFFFFF.toInt()
        isDark(bg) -> 0xFFFFFFFF.toInt()
        done -> 0xFF9A9A9A.toInt()
        else -> 0xFF222222.toInt()
    }

    fun mutedColor(bg: Int): Int = if (isDark(bg)) 0xB3FFFFFF.toInt() else 0xFF8A8A8A.toInt()

    /** Version foncée et plus saturée d'une couleur pastel (titres, intertitres, motif). */
    fun deepen(color: Int): Int {
        val hsv = FloatArray(3)
        Color.colorToHSV(opaque(color), hsv)
        hsv[1] = (hsv[1] * 1.8f + 0.25f).coerceAtMost(1f)
        hsv[2] = hsv[2] * 0.5f
        return Color.HSVToColor(hsv)
    }

    /**
     * Couleur du nom du jour, des intertitres et du motif :
     * blanc sur fond sombre, couleur du jour sur fond neutre, teinte foncée assortie sur fond pastel.
     */
    fun titleColor(bg: Int, day: Day): Int = when {
        isDark(bg) -> 0xFFFFFFFF.toInt()
        bg in neutrals -> day.color
        else -> deepen(bg)
    }
}
