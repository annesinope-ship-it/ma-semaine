package com.annesophie.semaine

import android.app.Activity
import android.app.AlertDialog
import android.app.TimePickerDialog
import android.content.pm.PackageManager
import android.os.Build
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
    private lateinit var colorButton: TextView
    private lateinit var reminderButton: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        intent.getStringExtra(DayWidgetProvider.EXTRA_DAY)?.let { current = Day.from(it) }
        RoutineDefaults.seedIfNeeded(this)

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

        colorButton = TextView(this).apply {
            textSize = 13f
            setTextColor(ink)
            setPadding(dp(4), dp(10), dp(4), 0)
            setOnClickListener { showColorDialog() }
        }
        root.addView(colorButton)

        reminderButton = TextView(this).apply {
            textSize = 13f
            setTextColor(ink)
            setPadding(dp(4), dp(8), dp(4), 0)
            setOnClickListener { showReminderDialog() }
        }
        root.addView(reminderButton)

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

        // Notifications (Android 13+) pour le rappel du soir
        if (Build.VERSION.SDK_INT >= 33 && TaskStore.reminderEnabled(this) &&
            checkSelfPermission("android.permission.POST_NOTIFICATIONS") != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf("android.permission.POST_NOTIFICATIONS"), 1)
        }
        ReminderReceiver.schedule(this)
        DayWidgetProvider.refreshAll(this)
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
            .setNeutralButton("Ma routine d'origine") { _, _ ->
                RoutineDefaults.load(this)
                changed()
                Toast.makeText(this, "Routine d'origine rechargée 🔁", Toast.LENGTH_SHORT).show()
            }
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

    /** Apparence : couleur + motif de la case du jour affiché (ou de toutes les cases). */
    private fun showColorDialog(applyAll: Boolean = false) {
        var dialog: AlertDialog? = null
        val allBox = CheckBox(this).apply {
            text = "Appliquer à toutes les cases"
            textSize = 14f
            isChecked = applyAll
        }
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(8), dp(20), dp(4))
        }
        fun targets(): List<Day> = if (allBox.isChecked) Day.values().toList() else listOf(current)
        fun reopen() {
            val all = allBox.isChecked
            dialog?.dismiss()
            showColorDialog(all)
        }
        fun section(title: String) = TextView(this).apply {
            text = title
            textSize = 13f
            setTypeface(null, Typeface.BOLD)
            setTextColor(ink)
            setPadding(0, dp(12), 0, dp(4))
        }

        // Thème en un geste
        val theme = TextView(this).apply {
            text = "🌿  Thème douceur champêtre\nune couleur pastel par jour"
            textSize = 14f
            setTextColor(ink)
            setPadding(dp(14), dp(10), dp(14), dp(10))
            background = pill(0xFFEFE7DA.toInt(), 14)
            setOnClickListener {
                Palette.champetre.forEach { (d, c) -> TaskStore.setColor(this@MainActivity, d, c) }
                changed()
                Toast.makeText(this@MainActivity, "Thème douceur champêtre appliqué 🌿", Toast.LENGTH_SHORT).show()
                reopen()
            }
        }
        content.addView(theme)
        content.addView(allBox)

        // Couleurs
        content.addView(section("Couleur"))
        val selected = TaskStore.getColor(this, current)
        val perRow = 4
        Palette.colors.chunked(perRow).forEach { rowColors ->
            val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            rowColors.forEach { (name, color) ->
                val isSel = color == selected
                val cell = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    gravity = Gravity.CENTER_HORIZONTAL
                    setPadding(dp(2), dp(6), dp(2), dp(6))
                    layoutParams = LinearLayout.LayoutParams(0, wrap, 1f)
                }
                val swatch = TextView(this).apply {
                    val d = GradientDrawable()
                    d.shape = GradientDrawable.OVAL
                    d.setColor(color)
                    d.setStroke(dp(if (isSel) 3 else 1), if (isSel) ink else 0xFFCCCCCC.toInt())
                    background = d
                    gravity = Gravity.CENTER
                    text = if (isSel) "✓" else ""
                    setTextColor(Palette.textColor(color))
                    layoutParams = LinearLayout.LayoutParams(dp(40), dp(40))
                }
                val label = TextView(this).apply {
                    text = name
                    textSize = 10f
                    gravity = Gravity.CENTER
                    setTextColor(grey)
                    setPadding(0, dp(4), 0, 0)
                }
                cell.addView(swatch)
                cell.addView(label)
                cell.setOnClickListener {
                    targets().forEach { TaskStore.setColor(this@MainActivity, it, color) }
                    changed()
                    reopen()
                }
                row.addView(cell)
            }
            repeat(perRow - rowColors.size) {
                row.addView(android.view.View(this).apply { layoutParams = LinearLayout.LayoutParams(0, 1, 1f) })
            }
            content.addView(row)
        }

        // Motifs
        content.addView(section("Motif"))
        val bg = TaskStore.getColor(this, current)
        val tint = Palette.titleColor(bg, current)
        val selDecor = TaskStore.getDecor(this, current)
        Palette.decors.chunked(3).forEach { rowDecors ->
            val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            rowDecors.forEach { (key, name, res) ->
                val isSel = key == selDecor
                val cell = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    gravity = Gravity.CENTER_HORIZONTAL
                    setPadding(dp(4), dp(8), dp(4), dp(8))
                    layoutParams = LinearLayout.LayoutParams(0, wrap, 1f)
                    background = pill(if (isSel) 0xFFEFE7DA.toInt() else Color.TRANSPARENT, 12)
                }
                val frame = LinearLayout(this).apply {
                    gravity = Gravity.CENTER
                    val d = GradientDrawable()
                    d.cornerRadius = dp(10).toFloat()
                    d.setColor(Palette.opaque(bg))
                    d.setStroke(dp(1), 0xFFDDDDDD.toInt())
                    background = d
                    layoutParams = LinearLayout.LayoutParams(dp(52), dp(52))
                }
                if (res != 0) {
                    val img = ImageView(this).apply {
                        setImageResource(res)
                        setColorFilter(tint)
                        layoutParams = LinearLayout.LayoutParams(dp(34), dp(34))
                    }
                    frame.addView(img)
                } else {
                    frame.addView(TextView(this).apply { text = "—"; setTextColor(grey) })
                }
                val label = TextView(this).apply {
                    text = if (isSel) "✓ $name" else name
                    textSize = 11f
                    gravity = Gravity.CENTER
                    setTextColor(if (isSel) ink else grey)
                    setPadding(0, dp(4), 0, 0)
                }
                cell.addView(frame)
                cell.addView(label)
                cell.setOnClickListener {
                    targets().forEach { TaskStore.setDecor(this@MainActivity, it, key) }
                    changed()
                    reopen()
                }
                row.addView(cell)
            }
            content.addView(row)
        }

        val scroll = ScrollView(this)
        scroll.addView(content)
        dialog = AlertDialog.Builder(this)
            .setTitle("Apparence — ${current.label}")
            .setView(scroll)
            .setPositiveButton("Fermer", null)
            .show()
    }

    /** Rappel du soir : heure et activation. */
    private fun showReminderDialog() {
        val on = TaskStore.reminderEnabled(this)
        val minutes = TaskStore.reminderMinutes(this)
        val options = arrayOf(
            "Changer l'heure (${hhmm(minutes)})",
            if (on) "Désactiver le rappel" else "Activer le rappel"
        )
        AlertDialog.Builder(this)
            .setTitle("🔔 Rappel du soir")
            .setItems(options) { _, which ->
                if (which == 0) {
                    TimePickerDialog(this, { _, h, m ->
                        TaskStore.setReminder(this, true, h * 60 + m)
                        ReminderReceiver.schedule(this)
                        render()
                    }, minutes / 60, minutes % 60, true).show()
                } else {
                    TaskStore.setReminder(this, !on, minutes)
                    ReminderReceiver.schedule(this)
                    if (!on && Build.VERSION.SDK_INT >= 33 &&
                        checkSelfPermission("android.permission.POST_NOTIFICATIONS") != PackageManager.PERMISSION_GRANTED
                    ) {
                        requestPermissions(arrayOf("android.permission.POST_NOTIFICATIONS"), 1)
                    }
                    render()
                }
            }
            .show()
    }

    private fun hhmm(minutes: Int): String = String.format("%02d:%02d", minutes / 60, minutes % 60)

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

        // Boutons Apparence et Rappel
        val currentColor = TaskStore.getColor(this, current)
        val colorName = Palette.colors.firstOrNull { it.second == currentColor }?.first ?: "Personnalisée"
        val decorName = Palette.decorName(TaskStore.getDecor(this, current))
        colorButton.text = "🎨  Apparence ${current.label} : $colorName · $decorName  ›"
        reminderButton.text = if (TaskStore.reminderEnabled(this))
            "🔔  Rappel du soir : ${hhmm(TaskStore.reminderMinutes(this))}  ›"
        else "🔕  Rappel du soir : désactivé  ›"

        // Liste du jour sélectionné
        list.removeAllViews()
        val rows = TaskStore.rows(this, current.sources, false)
        if (rows.isEmpty()) {
            val empty = TextView(this)
            empty.text = "Aucune tâche pour ${current.label.lowercase()}"
            empty.setTextColor(grey)
            empty.setPadding(dp(4), dp(16), 0, 0)
            list.addView(empty)
        }
        rows.forEach { r ->
            if (r is Row.Header) {
                val header = TextView(this)
                header.text = r.text
                header.textSize = if (r.big) 13f else 12f
                header.setTypeface(null, Typeface.BOLD)
                header.setTextColor(if (r.big) current.color else grey)
                header.setPadding(dp(4), dp(if (r.big) 14 else 8), 0, dp(6))
                list.addView(header)
                return@forEach
            }
            val entry = (r as Row.Item).entry
            val task = entry.task
            val done = entry.done

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
