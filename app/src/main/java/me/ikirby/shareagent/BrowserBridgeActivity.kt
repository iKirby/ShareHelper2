package me.ikirby.shareagent

import android.app.Activity
import android.content.Intent
import android.os.Bundle

class BrowserBridgeActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val url = intent.dataString
        if (url != null) {
            val intent = Intent(this, ShareActivity::class.java).apply {
                action = Intent.ACTION_SEND
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, url)
            }
            startActivity(intent)
        }
        finish()
    }

}
