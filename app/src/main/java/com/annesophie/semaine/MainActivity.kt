package com.annesophie.semaine

import android.app.Activity
import android.app.AlertDialog
import android.content.ClipboardManager
import android.content.Intent
import android.widget.Toast
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

/** L'appli : gérer les tâches de toute la semaine. */
class MainActivity : Activity() {

    private val wrap = ViewGroup.LayoutParams.WRAP_CONTENT
    private val match = ViewGroup.LayoutParams.MATCH_PARENT
    private val grey = 0xFF9A9A9A.toInt()
    private val ink = 0xFF222222.toInt()

    private var current = Day.today()
    private lateinit var dayBar: LinearLayout
    private lateinit var list: LinearLayout
    private lateinit var input: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        intent.getStringExtra(DayWidgetProvider.EXTRA_DAY)?.let { current = Day.from(it) }

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(24), dp(16), dp(16))
            setBackgroundColor(0xFFF4F1EC.toInt())
        }

        val title = TextView(this).apply {
            text = "Ma semaine"
            textSize = 28f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(ink)
            layoutParams = LinearLayout.LayoutParams(0, wrap, 1f)
        }
        val pasteButton = TextView(this).apply {
            text = "📋  Coller une liste"
            textSize = 14f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(ink)
            setPadding(dp(14), dp(8), dp(14), dp(8))
            background = pill(Color.WHITE, 18)
            setOnClickListener { showPasteDialog() }
        }
        val titleRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(8), 0, dp(12))
        }
        titleRow.addView(title)
        titleRow.addView(pasteButton)
        root.addView(titleRow)

        dayBar = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val dayScroll = HorizontalScrollView(this).apply { isHorizontalScrollBarEnabled = false }
        dayScroll.addView(dayBar)
        root.addView(dayScroll)

        input = EditText(this).apply {
            hint = "Nouvelle tâche"
            isSingleLine = true
            imeOptions = EditorInfo.IME_ACTION_DONE
            layoutParams = LinearLayout.LayoutParams(0, wrap, 1f)
        }
        val addButton = Button(this).apply { text = "Ajouter" }
        val inputRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(12), 0, dp(8))
        }
        inputRow.addView(input)
        inputRow.addView(addButton)
        root.addView(inputRow)

        list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val listScroll = ScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(match, 0, 1f)
        }
        listScroll.addView(list)
        root.addView(listScroll)

        val clearButton = Button(this).apply { text = "Effacer les tâches faites" }
        root.addView(clearButton)

        setContentView(root)

        addButton.setOnClickListener { addTask() }
        input.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                addTask()
                true
            } else {
                false
            }
        }
        clearButton.setOnClickListener {
            TaskStore.clearDone(this, current)
            changed()
        }

        render()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        intent.getStringExtra(DayWidgetProvider.EXTRA_DAY)?.let {
            current = Day.from(it)
            render()
        }
    }

    override fun onResume() {
        super.onResume()
        if (::list.isInitialized) render()
    }

    private fun addTask() {
        TaskStore.add(this, current, input.text.toString())
        input.setText("")
        changed()
    }

    private fun changed() {
        DayWidgetProvider.refreshAll(this)
        render()
    }

    /** Coller une liste pour toute la semaine. */
    private fun showPasteDialog() {
        val box = EditText(this).apply {
            hint = "lun : courses\nven : dentiste\nwe : run club, film"
            minLines = 6
            gravity = Gravity.TOP or Gravity.START
            isSingleLine = false
        }

        // Pré-remplit avec le presse-papiers s'il contient du texte
        try {
            val clip = (getSystemService(CLIPBOARD_SERVICE) as ClipboardManager).primaryClip
            val clipText = clip?.getItemAt(0)?.coerceToText(this)?.toString()
            if (!clipText.isNullOrBlank()) box.setText(clipText)
        } catch (e: Exception) {
            // presse-papiers inaccessible : on laisse vide
        }

        val help = TextView(this).apply {
            text = "Une ligne par jour (lun, mar, mer, jeu, ven, sam, dim, we).\n" +
                "Plusieurs tâches : séparées par des virgules.\n" +
                "Sans jour : la tâche va dans ${current.label.lowercase()}."
            setTextColor(grey)
            textSize = 12f
            setPadding(0, 0, 0, dp(8))
        }
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(8), dp(20), 0)
        }
        val routineBox = CheckBox(this).apply {
            text = "🔁 Routine : revient chaque semaine et se décoche le lundi (remplace la routine déjà enregistrée pour ces jours)"
            textSize = 13f
        }
        content.addView(help)
        content.addView(box)
        content.addView(routineBox)
        // Un texte qui commence par "ROUTINE" coche la case automatiquement
        if (box.text.toString().trimStart().uppercase().startsWith("ROUTINE")) routineBox.isChecked = true

        AlertDialog.Builder(this)
            .setTitle("Coller une liste")
            .setView(content)
            .setNegativeButton("Annuler", null)
            .setPositiveButton("Ajouter") { _, _ ->
                val items = WeekParser.parse(box.text.toString(), current)
                val routine = routineBox.isChecked
                if (routine) TaskStore.clearRoutine(this, items.map { it.day }.toSet())
                items.forEach { TaskStore.add(this, it.day, it.text, routine, it.nth) }
                changed()
                val days = items.map { it.day }.distinct().size
                Toast.makeText(
                    this,
                    if (items.isEmpty()) "Aucune tâche trouvée"
                    else "${items.size} tâche(s) ajoutée(s) sur $days jour(s)",
                    Toast.LENGTH_LONG
                ).show()
            }
            .show()
    }

    private fun pill(color: Int, radius: Int): GradientDrawable {
        val d = GradientDrawable()
        d.cornerRadius = dp(radius).toFloat()
        d.setColor(color)
        return d
    }

    private fun render() {
        // Barre des jours
        dayBar.removeAllViews()
        Day.values().forEach { day ->
            val count = TaskStore.remaining(this, day)
            val selected = day == current
            val chip = TextView(this)
            chip.text = if (count > 0) "${day.short}  $count" else day.short
            chip.textSize = 14f
            chip.setTypeface(null, Typeface.BOLD)
            chip.setPadding(dp(14), dp(8), dp(14), dp(8))
            chip.setTextColor(if (selected) Color.WHITE else day.color)
            chip.background = pill(if (selected) day.color else Color.WHITE, 18)
            val lp = LinearLayout.LayoutParams(wrap, wrap)
            lp.marginEnd = dp(8)
            chip.layoutParams = lp
            chip.setOnClickListener {
                current = day
                render()
            }
            dayBar.addView(chip)
        }

        // Liste du jour sélectionné
        list.removeAllViews()
        val entries = TaskStore.entries(this, current, false)
        if (entries.isEmpty()) {
            val empty = TextView(this)
            empty.text = "Aucune tâche pour ${current.label.lowercase()}"
            empty.setTextColor(grey)
            empty.setPadding(dp(4), dp(16), 0, 0)
            list.addView(empty)
        }
        var lastSource: Day? = null
        val multi = current.sources.size > 1
        entries.forEach { entry ->
            val task = entry.task
            val done = entry.done

            // Week-end : petits intertitres Samedi / Dimanche / Week-end
            if (multi && entry.day != lastSource) {
                val header = TextView(this)
                header.text = entry.day.label.uppercase()
                header.textSize = 12f
                header.setTypeface(null, Typeface.BOLD)
                header.setTextColor(entry.day.color)
                header.setPadding(dp(4), dp(10), 0, dp(6))
                list.addView(header)
                lastSource = entry.day
            }

            val row = LinearLayout(this)
            row.orientation = LinearLayout.HORIZONTAL
            row.gravity = Gravity.CENTER_VERTICAL
            row.setPadding(dp(12), dp(10), dp(8), dp(10))
            row.background = pill(Color.WHITE, 14)
            val rowLp = LinearLayout.LayoutParams(match, wrap)
            rowLp.bottomMargin = dp(8)
            row.layoutParams = rowLp

            val check = ImageView(this)
            check.setImageResource(if (done) R.drawable.ic_check_on else R.drawable.ic_check_off)
            check.layoutParams = LinearLayout.LayoutParams(dp(24), dp(24))

            val label = TextView(this)
            label.text = if (task.routine) "${task.displayText()}  🔁" else task.displayText()
            label.textSize = 16f
            label.setTextColor(if (done) grey else ink)
            if (done) label.paintFlags = label.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            label.setPadding(dp(12), 0, dp(8), 0)
            label.layoutParams = LinearLayout.LayoutParams(0, wrap, 1f)

            val delete = TextView(this)
            delete.text = "✕"
            delete.textSize = 16f
            delete.setTextColor(grey)
            delete.setPadding(dp(8), dp(4), dp(8), dp(4))
            delete.setOnClickListener {
                TaskStore.delete(this, entry.day, task.id)
                changed()
            }

            row.setOnClickListener {
                TaskStore.toggle(this, entry.day, task.id)
                changed()
            }

            row.addView(check)
            row.addView(label)
            row.addView(delete)
            list.addView(row)
        }
    }
}
