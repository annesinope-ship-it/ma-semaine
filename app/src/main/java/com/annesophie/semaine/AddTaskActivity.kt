package com.annesophie.semaine

import android.app.Activity
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView

/**
 * Petite fenêtre ouverte par "+ Ajouter" sur le widget.
 * Ajout en rafale : elle reste ouverte après chaque tâche, "Terminé" la ferme.
 */
class AddTaskActivity : Activity() {

    private lateinit var day: Day
    private lateinit var input: EditText
    private lateinit var added: TextView
    private val addedNow = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        day = Day.from(intent.getStringExtra(DayWidgetProvider.EXTRA_DAY))
        title = "Ajouter — ${day.label}"

        input = EditText(this).apply {
            hint = "Nouvelle tâche puis ↵"
            isSingleLine = true
            imeOptions = EditorInfo.IME_ACTION_SEND
        }

        added = TextView(this).apply {
            setTextColor(0xFF8E8E93.toInt())
            textSize = 13f
            setPadding(dp(4), dp(6), dp(4), dp(6))
        }

        val addButton = Button(this).apply {
            text = "Ajouter"
            setTextColor(day.color)
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }
        val doneButton = Button(this).apply {
            text = "Terminé"
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }
        val buttons = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.END
        }
        buttons.addView(doneButton)
        buttons.addView(addButton)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(8), dp(20), dp(16))
        }
        layout.addView(input)
        layout.addView(added)
        layout.addView(buttons)
        setContentView(layout)
        window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)

        renderAdded()

        addButton.setOnClickListener { submit() }
        doneButton.setOnClickListener {
            // Si un texte est encore tapé, on l'ajoute avant de fermer
            submit()
            finish()
        }
        input.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEND || actionId == EditorInfo.IME_ACTION_DONE) {
                submit()
                true
            } else {
                false
            }
        }
        input.requestFocus()
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
    }

    private fun submit() {
        val text = input.text.toString().trim()
        if (text.isEmpty()) return
        TaskStore.add(this, day, text)
        DayWidgetProvider.refreshAll(this)
        addedNow.add(text)
        input.setText("")
        renderAdded()
    }

    private fun renderAdded() {
        added.text = if (addedNow.isEmpty()) {
            "Tape une tâche puis ↵ : la fenêtre reste ouverte pour la suivante."
        } else {
            "Ajouté :\n" + addedNow.joinToString("\n") { "✓ $it" }
        }
    }
}
