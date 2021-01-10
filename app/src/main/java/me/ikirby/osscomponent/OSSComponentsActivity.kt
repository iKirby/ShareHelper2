package me.ikirby.osscomponent

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.MenuItem
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class OSSComponentsActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        title = "Open source components"
        recyclerView = RecyclerView(this).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            layoutManager = LinearLayoutManager(this@OSSComponentsActivity)
        }
        setContentView(recyclerView)

        val itemClickListener = object : OSSItemClickListener {
            override fun onClick(url: String) {
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    data = Uri.parse(url)
                }
                startActivity(intent)
            }
        }
        val adapter = OSSComponentAdapter(itemClickListener)
        recyclerView.adapter = adapter

        val list = listOf(
            OSSComponent("AndroidX", "https://www.apache.org/licenses/LICENSE-2.0"),
            OSSComponent("kotlinx.coroutines", "https://www.apache.org/licenses/LICENSE-2.0"),
            OSSComponent(
                "Material Components for Android",
                "https://www.apache.org/licenses/LICENSE-2.0"
            ),
            OSSComponent("jsoup", "https://jsoup.org/license")
        )
        adapter.setData(list)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}
