package com.annesophie.semaine

import android.content.Context

/** Routine d'Anne-Sophie, chargée automatiquement au premier lancement. */
object RoutineDefaults {

    val TEXT = """
ROUTINE
Lundi
- ☀ Skincare | nettoyant + sérum + toner + crème + SPF
- ☀ Lit, vaisselle, cuisine | lit fait, vaisselle, surfaces cuisine
- ☀ Arroser les plantes
- ☾ Skincare | double nettoyage + toner + sérum hyaluronique + crème
- ☾ Exfoliant AHA/BHA
- ☾ Ménage off · danse 💃
Mardi
- ☀ Skincare | nettoyant + sérum + toner + crème + SPF
- ☀ Lit, vaisselle, cuisine | lit fait, vaisselle, surfaces cuisine
- ☀ Serviettes et torchons | changer serviettes de bain + torchons
- ☀ Lancer une machine
- ☾ Skincare | double nettoyage + toner + sérum hyaluronique + crème
- ☾ Vaisselle, cuisine | vaisselle, surfaces cuisine
- ☾ Étendre le linge
Mercredi
- ☀ Skincare | nettoyant + sérum + toner + crème + SPF
- ☀ Lit, vaisselle, cuisine | lit fait, vaisselle, surfaces cuisine
- ☀ Baignoire | nettoyer la baignoire
- ☀ Minerva · croquettes | remplir la gamelle de croquettes
- ☾ Skincare | double nettoyage + toner + sérum hyaluronique + crème
- ☾ Exfoliant AHA/BHA
- ☾ Vaisselle, cuisine | vaisselle, surfaces cuisine
- ☾ Ranger le linge | plier et ranger le linge
Jeudi
- ☀ Skincare | nettoyant + sérum + toner + crème + SPF
- ☀ Lit, vaisselle, cuisine | lit fait, vaisselle, surfaces cuisine
- ☀ Toilettes | nettoyer les toilettes
- ☾ Skincare | double nettoyage + toner + sérum hyaluronique + crème
- ☾ Vaisselle, cuisine | vaisselle, surfaces cuisine
Vendredi
- ☀ Skincare | nettoyant + sérum + toner + crème + SPF
- ☀ Lit, vaisselle, cuisine | lit fait, vaisselle, surfaces cuisine
- ☾ Skincare | double nettoyage + toner + sérum hyaluronique + crème
- ☾ Exfoliant AHA/BHA
- ☾ Vaisselle, cuisine | vaisselle, surfaces cuisine
- ☾ Changer les draps
Samedi
- ☀ Skincare | nettoyant + sérum + toner + crème + SPF
- ☀ Lit, vaisselle, cuisine | lit fait, vaisselle, surfaces cuisine
- ☀ Minerva · eau | changer l'eau
- ☾ Skincare | double nettoyage + toner + sérum hyaluronique + crème
- ☾ Vaisselle, cuisine | vaisselle, surfaces cuisine
- ☾ Poubelles | vider les poubelles
Dimanche
- ☀ Skincare | nettoyant + sérum + toner + crème + SPF
- ☀ Lit, vaisselle, cuisine | lit fait, vaisselle, surfaces cuisine
- ☀ Minerva · croquettes | remplir la gamelle de croquettes
- ☾ Skincare | double nettoyage + toner + sérum hyaluronique + crème
- ☾ Cheveux | shampoing + masque + démêlant
- ☾ Vaisselle, cuisine | vaisselle, surfaces cuisine
- ☾ Frigo | nettoyer le frigo
- 1er du mois : Shampoing clarifiant
- 2e du mois : Minerva · litière | nettoyer la litière
""".trimIndent()

    private const val VERSION = 2   // 2 = intitulés courts + détails

    /**
     * Charge la routine au premier lancement, puis une seule fois à chaque nouvelle version
     * de la routine d'origine (ex. passage aux intitulés courts).
     */
    fun seedIfNeeded(c: Context) {
        val prefs = c.getSharedPreferences("ma_semaine", Context.MODE_PRIVATE)
        val current = prefs.getInt("routine_version", if (prefs.getBoolean("routine_seeded", false)) 1 else 0)
        if (current >= VERSION) return
        val hasRoutine = Day.values().any { d -> TaskStore.getTasks(c, d).any { it.routine } }
        // Première installation avec une routine déjà collée à la main : on n'y touche pas
        if (!(current == 0 && hasRoutine)) load(c)
        prefs.edit().putInt("routine_version", VERSION).putBoolean("routine_seeded", true).commit()
    }

    /** (Re)charge la routine d'origine : remplace la routine des jours concernés. */
    fun load(c: Context) {
        val items = WeekParser.parse(TEXT, Day.MON)
        TaskStore.clearRoutine(c, items.map { it.day }.toSet())
        items.forEach { TaskStore.add(c, it.day, it.text, true, it.nth, it.detail) }
    }
}
