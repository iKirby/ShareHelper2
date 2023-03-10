package me.ikirby.shareagent

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import me.ikirby.shareagent.adapter.OnItemClickListener
import me.ikirby.shareagent.adapter.ParamsListAdapter
import me.ikirby.shareagent.databinding.ActivityParamsConfigBinding
import me.ikirby.shareagent.widget.showSingleInputDialog

class ParamsConfigActivity : AppCompatActivity() {

    private lateinit var binding: ActivityParamsConfigBinding
    private lateinit var adapter: ParamsListAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityParamsConfigBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setTitle(R.string.params_remove_list)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        adapter = ParamsListAdapter()
        adapter.onItemClickListener = object : OnItemClickListener {
            override fun onClick(view: View, position: Int) {
                adapter.removeItem(position)
            }
        }
        binding.recyclerView.adapter = adapter

        App.prefs.removeParams.forEach(adapter::addItem)


        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                App.prefs.removeParams = adapter.getList()
                finish()
            }
        })
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.params_config, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressedDispatcher.onBackPressed()
                true
            }
            R.id.menu_add -> {
                showAddDialog()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showAddDialog() {
        showSingleInputDialog(this, R.string.add, {
            if (it.isNotEmpty()) {
                adapter.addItem(it)
            }
        })
    }
}