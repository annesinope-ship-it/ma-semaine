package com.annesophie.semaine

import android.content.Context

/** Routine d'Anne-Sophie, chargée automatiquement au premier lancement. */
object RoutineDefaults {

    val TEXT = """
ROUTINE
Lundi
- ☀ Beauté : nettoyant + sérum + toner + crème + SPF
- ☀ Lit fait, vaisselle, surfaces cuisine
- ☀ Arroser les plantes
- ☾ Beauté : double nettoyage + toner + sérum hyaluronique + crème
- ☾ Exfoliant AHA/BHA
- ☾ Ménage : off — danse 💃
Mardi
- ☀ Beauté : nettoyant + sérum + toner + crème + SPF
- ☀ Lit fait, vaisselle, surfaces cuisine
- ☀ Changer serviettes de bain + torchons
- ☀ Lancer une machine
- ☾ Beauté : double nettoyage + toner + sérum hyaluronique + crème
- ☾ Vaisselle, surfaces cuisine
- ☾ Étendre le linge
Mercredi
- ☀ Beauté : nettoyant + sérum + toner + crème + SPF
- ☀ Lit fait, vaisselle, surfaces cuisine
- ☀ Nettoyer la baignoire
- ☀ Minerva : remplir la gamelle de croquettes
- ☾ Beauté : double nettoyage + toner + sérum hyaluronique + crème
- ☾ Exfoliant AHA/BHA
- ☾ Vaisselle, surfaces cuisine
- ☾ Plier et ranger le linge
Jeudi
- ☀ Beauté : nettoyant + sérum + toner + crème + SPF
- ☀ Lit fait, vaisselle, surfaces cuisine
- ☀ Nettoyer les toilettes
- ☾ Beauté : double nettoyage + toner + sérum hyaluronique + crème
- ☾ Vaisselle, surfaces cuisine
Vendredi
- ☀ Beauté : nettoyant + sérum + toner + crème + SPF
- ☀ Lit fait, vaisselle, surfaces cuisine
- ☾ Beauté : double nettoyage + toner + sérum hyaluronique + crème
- ☾ Exfoliant AHA/BHA
- ☾ Vaisselle, surfaces cuisine
- ☾ Changer les draps
Samedi
- ☀ Beauté : nettoyant + sérum + toner + crème + SPF
- ☀ Lit fait, vaisselle, surfaces cuisine
- ☀ Minerva : changer l'eau
- ☾ Beauté : double nettoyage + toner + sérum hyaluronique + crème
- ☾ Vaisselle, surfaces cuisine
- ☾ Vider les poubelles
Dimanche
- ☀ Beauté : nettoyant + sérum + toner + crème + SPF
- ☀ Lit fait, vaisselle, surfaces cuisine
- ☀ Minerva : remplir la gamelle de croquettes
- ☾ Beauté : double nettoyage + toner + sérum hyaluronique + crème
- ☾ Cheveux : shampoing + masque + démêlant
- ☾ Vaisselle, surfaces cuisine
- ☾ Nettoyer le frigo
- 1er du mois : Shampoing clarifiant
- 2e du mois : Minerva : nettoyer la litière
""".trimIndent()

    /** Charge la routine si elle n'a jamais été chargée et qu'aucune routine n'existe. */
    fun seedIfNeeded(c: Context) {
        val prefs = c.getSharedPreferences("ma_semaine", Context.MODE_PRIVATE)
        if (prefs.getBoolean("routine_seeded", false)) return
        val hasRoutine = Day.values().any { d -> TaskStore.getTasks(c, d).any { it.routine } }
        if (!hasRoutine) load(c)
        prefs.edit().putBoolean("routine_seeded", true).commit()
    }

    /** (Re)charge la routine d'origine : remplace la routine des jours concernés. */
    fun load(c: Context) {
        val items = WeekParser.parse(TEXT, Day.MON)
        TaskStore.clearRoutine(c, items.map { it.day }.toSet())
        items.forEach { TaskStore.add(c, it.day, it.text, true, it.nth) }
    }
}
