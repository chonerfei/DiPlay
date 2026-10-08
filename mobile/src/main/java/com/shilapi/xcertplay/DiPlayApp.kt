package com.shilapi.xcertplay

import android.app.Application
import android.content.Context
import androidx.multidex.MultiDex

/**
 * Android 4.4 (Dalvik) port: installs legacy multidex so secondary dex files load on KitKat.
 * ART (Android 5+) loads them natively; MultiDex.install is harmless there.
 */
class DiPlayApp : Application() {
    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base)
        MultiDex.install(this)
    }
}
