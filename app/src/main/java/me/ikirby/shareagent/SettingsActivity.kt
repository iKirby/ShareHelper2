package me.ikirby.shareagent

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import me.ikirby.shareagent.fragment.SettingsFragment

class SettingsActivity: AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        supportFragmentManager.beginTransaction().add(android.R.id.content, SettingsFragment()).commit()
    }

}
