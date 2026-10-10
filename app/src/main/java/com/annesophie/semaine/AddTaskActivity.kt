package com.annesophie.semaine

import android.app.Activity
import android.os.Bundle
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout

/** Petite fenêtre ouverte par "+ Ajouter" sur le widget. */
class AddTaskActivity : Activity() {

    private lateinit var day: Day
    private lateinit var input: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        day = Day.from(intent.getStringExtra(DayWidgetProvider.EXTRA_DAY))
        title = "Ajouter — ${day.label}"

        input = EditText(this).apply {
            hint = "Nouvelle tâche"
            isSingleLine = true
            imeOptions = EditorInfo.IME_ACTION_DONE
        }
        val addButton = Button(this).apply {
            text = "Ajouter"
            setTextColor(day.color)
        }
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(8), dp(20), dp(16))
        }
        layout.addView(input)
        layout.addView(addButton)
        setContentView(layout)
        window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)

        addButton.setOnClickListener { submit() }
        input.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
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
        TaskStore.add(this, day, input.text.toString())
        DayWidgetProvider.refreshAll(this)
        finish()
    }
}
