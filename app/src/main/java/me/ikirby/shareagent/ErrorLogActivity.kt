package me.ikirby.shareagent

import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import me.ikirby.shareagent.databinding.ActivityErrorLogBinding

class ErrorLogActivity: AppCompatActivity() {

    private lateinit var binding: ActivityErrorLogBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityErrorLogBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setTitle(R.string.error_log)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        binding.textView.text = App.prefs.lastError ?: getString(R.string.no_error)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                finish()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }
}
