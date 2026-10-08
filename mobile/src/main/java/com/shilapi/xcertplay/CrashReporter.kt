package com.shilapi.xcertplay

import android.app.Application
import android.content.Intent
import android.os.Build
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter

/**
 * Field diagnostics for head units without adb: on any uncaught exception the full stack is
 * best-effort written under the app's external files dir and shown on screen by CrashActivity,
 * which runs in its own ":crash" process so it survives the dying one.
 */
object CrashReporter {
    const val EXTRA_TRACE = "trace"

    fun install(app: Application) {
        // Never install inside the reporter process itself, so a reporter crash cannot loop.
        if (currentProcessName()?.endsWith(":crash") == true) return
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            val trace = describe(app, thread, error)
            saveToFile(app, trace)
            showOnScreen(app, trace)
            previous?.uncaughtException(thread, error)
        }
    }

    private fun describe(app: Application, thread: Thread, error: Throwable): String {
        val stack = StringWriter().also { error.printStackTrace(PrintWriter(it)) }.toString()
        return buildString {
            append("time=").append(System.currentTimeMillis())
            append(" model=").append(Build.MODEL)
            append(" os=Android ").append(Build.VERSION.RELEASE)
            append(" (SDK ").append(Build.VERSION.SDK_INT).append(")\n")
            append("process=").append(currentProcessName() ?: "?")
            append(" thread=").append(thread.name).append('\n')
            append(stack.take(60000))
        }
    }

    private fun saveToFile(app: Application, trace: String) {
        try {
            val dir = app.getExternalFilesDir(null) ?: return
            val file = File(dir, "crash-log.txt")
            // Keep the log bounded; the newest entries matter most in the field.
            if (file.length() > 512 * 1024) file.delete()
            file.appendText(trace + "\n\n")
        } catch (ignored: Throwable) {
        }
    }

    private fun showOnScreen(app: Application, trace: String) {
        try {
            app.startActivity(
                Intent(app, CrashActivity::class.java)
                    .addFlags(
                        Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK or
                            Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
                    )
                    .putExtra(EXTRA_TRACE, trace)
            )
        } catch (ignored: Throwable) {
        }
    }

    private fun currentProcessName(): String? = try {
        if (Build.VERSION.SDK_INT >= 28) Application.getProcessName()
        else Class.forName("android.app.ActivityThread").getMethod("currentProcessName").invoke(null) as? String
    } catch (t: Throwable) {
        null
    }
}
