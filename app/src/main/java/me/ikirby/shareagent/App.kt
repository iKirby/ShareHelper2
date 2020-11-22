package me.ikirby.shareagent

import android.app.Application
import me.ikirby.shareagent.preference.Prefs

class App: Application() {

    companion object {
        lateinit var prefs: Prefs
    }

    override fun onCreate() {
        super.onCreate()
        prefs = Prefs(this)
    }
}
