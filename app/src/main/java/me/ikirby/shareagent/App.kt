package me.ikirby.shareagent

import android.app.Application
import android.widget.Toast
import androidx.annotation.StringRes
import me.ikirby.shareagent.contextual.Prefs

class App : Application() {

    companion object {
        private lateinit var instance: App
        lateinit var prefs: Prefs

        fun showToast(@StringRes resId: Int) {
            Toast.makeText(instance, resId, Toast.LENGTH_LONG).show()
        }

        fun showToast(text: String) {
            Toast.makeText(instance, text, Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        prefs = Prefs(this)
    }
}
