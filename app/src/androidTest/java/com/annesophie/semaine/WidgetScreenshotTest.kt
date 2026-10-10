package com.annesophie.semaine

import android.app.Activity
import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.util.Log
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.GridLayout
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.FileOutputStream

/**
 * Pose de vrais widgets (via AppWidgetHost, comme un lanceur) et prend des captures.
 * Nécessite : adb shell appwidget grantbind --package com.annesophie.semaine
 */
@RunWith(AndroidJUnit4::class)
class WidgetScreenshotTest {

    private val inst = InstrumentationRegistry.getInstrumentation()
    private val ctx = inst.targetContext
    private val outDir: File get() = ctx.getExternalFilesDir("shots")!!.apply { mkdirs() }

    private fun save(bmp: Bitmap, name: String) {
        FileOutputStream(File(outDir, name)).use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    private fun shoot(name: String) {
        Thread.sleep(2500)
        inst.waitForIdleSync()
        val bmp = inst.uiAutomation.takeScreenshot()
        if (bmp != null) save(bmp, name)
    }

    @Test
    fun widgets() {
        // Données : routine d'origine + quelques tâches ponctuelles
        ctx.getSharedPreferences("ma_semaine", 0).edit().clear().commit()
        RoutineDefaults.seedIfNeeded(ctx)
        TaskStore.add(ctx, Day.MON, "Courses")
        TaskStore.add(ctx, Day.FRI, "Dentiste")
        TaskStore.add(ctx, Day.WEEKEND, "Run club")
        Palette.champetre.forEach { (d, c) -> TaskStore.setColor(ctx, d, c) }
        Day.values().forEach { TaskStore.setDecor(ctx, it, "chat") }
        TaskStore.setDecor(ctx, Day.TUE, "brin")
        TaskStore.setDecor(ctx, Day.WED, "fleur")
        TaskStore.setColor(ctx, Day.FRI, 0xFF7A2E3B.toInt())
        TaskStore.setDecor(ctx, Day.FRI, "lune")
        // Une tâche cochée pour voir le rendu
        val firstMon = TaskStore.getTasks(ctx, Day.MON).first()
        TaskStore.toggle(ctx, Day.MON, firstMon.id)

        val manager = AppWidgetManager.getInstance(ctx)
        val host = AppWidgetHost(ctx, 4242)
        val dayProvider = ComponentName(ctx, DayWidgetProvider::class.java)
        val todayProvider = ComponentName(ctx, TodayWidgetProvider::class.java)

        val days = listOf(Day.MON, Day.TUE, Day.WED, Day.THU, Day.FRI, Day.WEEKEND)
        val ids = days.map { d ->
            val id = host.allocateAppWidgetId()
            val ok = manager.bindAppWidgetIdIfAllowed(id, dayProvider)
            Log.i("WidgetTest", "bind $d -> $ok")
            TaskStore.setWidgetDay(ctx, id, d)
            id
        }
        val todayId = host.allocateAppWidgetId()
        Log.i("WidgetTest", "bind today -> ${manager.bindAppWidgetIdIfAllowed(todayId, todayProvider)}")

        // Ouvre l'appli (capture de l'écran principal)
        val launch = Intent(ctx, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val activity: Activity = inst.startActivitySync(launch)
        shoot("1-appli.png")

        // Affiche les widgets dans l'activité, comme sur un écran d'accueil
        inst.runOnMainSync {
            host.startListening()
            val grid = GridLayout(activity).apply {
                columnCount = 2
                setBackgroundColor(Color.rgb(120, 140, 160))
                setPadding(20, 60, 20, 20)
            }
            val w = activity.resources.displayMetrics.widthPixels / 2 - 30
            ids.forEach { id ->
                val v: AppWidgetHostView = host.createView(activity, id, manager.getAppWidgetInfo(id))
                grid.addView(v, ViewGroup.MarginLayoutParams(w, (w * 1.05).toInt()).apply { setMargins(10, 10, 10, 10) })
            }
            activity.setContentView(grid)
        }
        DayWidgetProvider.refreshAll(ctx)
        shoot("2-semaine.png")

        inst.runOnMainSync {
            val frame = FrameLayout(activity).apply {
                setBackgroundColor(Color.rgb(120, 140, 160))
                setPadding(30, 80, 30, 30)
            }
            val v = host.createView(activity, todayId, manager.getAppWidgetInfo(todayId))
            val w = activity.resources.displayMetrics.widthPixels - 60
            frame.addView(v, FrameLayout.LayoutParams(w, (w * 0.9).toInt()))
            activity.setContentView(frame)
        }
        DayWidgetProvider.refreshAll(ctx)
        shoot("3-aujourdhui.png")

        inst.runOnMainSync { host.stopListening() }
    }

    /** Parcours réel : on pose un widget, la fenêtre « Quel jour ? » s'ouvre, on choisit Lundi. */
    @Test
    fun configFlow() {
        RoutineDefaults.seedIfNeeded(ctx)
        val manager = AppWidgetManager.getInstance(ctx)
        val host = AppWidgetHost(ctx, 4343)
        val id = host.allocateAppWidgetId()
        manager.bindAppWidgetIdIfAllowed(id, ComponentName(ctx, DayWidgetProvider::class.java))

        val cfgIntent = Intent(ctx, ConfigActivity::class.java)
            .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val cfg = inst.startActivitySync(cfgIntent)
        shoot("4-choix-du-jour.png")
        inst.runOnMainSync {
            fun find(v: android.view.View): android.widget.Button? {
                if (v is android.widget.Button && v.text.toString() == "Mardi") return v
                if (v is ViewGroup) for (i in 0 until v.childCount) find(v.getChildAt(i))?.let { return it }
                return null
            }
            find(cfg.window.decorView)!!.performClick()
        }
        Thread.sleep(1000)

        val launch = Intent(ctx, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val activity: Activity = inst.startActivitySync(launch)
        inst.runOnMainSync {
            host.startListening()
            val frame = FrameLayout(activity).apply {
                setBackgroundColor(Color.rgb(120, 140, 160))
                setPadding(30, 80, 30, 30)
            }
            val v = host.createView(activity, id, manager.getAppWidgetInfo(id))
            frame.addView(v, FrameLayout.LayoutParams(500, 520))
            activity.setContentView(frame)
        }
        DayWidgetProvider.refreshAll(ctx)
        shoot("5-apres-choix.png")
        inst.runOnMainSync { host.stopListening() }
    }
}
