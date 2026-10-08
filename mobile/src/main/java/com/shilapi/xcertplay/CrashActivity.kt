package com.shilapi.xcertplay

import android.app.Activity
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

/**
 * Standalone-process crash screen: shows the full stack trace so it can be photographed from a
 * head unit that has no adb. Declared with android:process=":crash" in the manifest.
 */
class CrashActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val trace = intent.getStringExtra(CrashReporter.EXTRA_TRACE) ?: "no trace captured"
        val body = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.rgb(16, 18, 24))
            setPadding(dp(16), dp(24), dp(16), dp(16))
        }
        body.addView(TextView(this).apply {
            text = "DiPlay crashed"
            setTextColor(Color.WHITE); textSize = 18f; typeface = Typeface.DEFAULT_BOLD
        })
        body.addView(TextView(this).apply {
            text = "Please take a photo of this screen and send it back."
            setTextColor(Color.rgb(160, 168, 180)); textSize = 12f
            setPadding(0, dp(6), 0, dp(10))
        })
        body.addView(
            ScrollView(this).apply {
                addView(
                    TextView(this@CrashActivity).apply {
                        text = trace
                        setTextColor(Color.rgb(220, 224, 232)); textSize = 9f
                        typeface = Typeface.MONOSPACE
                        setTextIsSelectable(true)
                    }
                )
            },
            LinearLayout.LayoutParams(-1, 0, 1f)
        )
        body.addView(
            Button(this).apply {
                text = "Close"
                setOnClickListener { finishAffinity() }
            }
        )
        setContentView(body, ViewGroup.LayoutParams(-1, -1))
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
}
