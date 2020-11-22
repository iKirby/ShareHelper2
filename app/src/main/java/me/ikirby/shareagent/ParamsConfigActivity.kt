package me.ikirby.shareagent

import android.app.Dialog
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import me.ikirby.shareagent.adapter.OnItemClickListener
import me.ikirby.shareagent.adapter.ParamsListAdapter
import me.ikirby.shareagent.databinding.ActivityParamsConfigBinding

class ParamsConfigActivity: AppCompatActivity() {

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
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.params_config, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                true
            }
            R.id.menu_add -> {
                showAddDialog()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onBackPressed() {
        App.prefs.removeParams = adapter.getList()
        super.onBackPressed()
    }

    private fun showAddDialog() {
        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle(R.string.add)
            .setView(R.layout.dialog_single_input)
            .setPositiveButton(R.string.ok, null)
            .setNegativeButton(R.string.cancel, null)
            .create()

        dialog.setOnShowListener {
            val btnOK = dialog.getButton(Dialog.BUTTON_POSITIVE)
            val editText = dialog.findViewById<EditText>(R.id.editText)!!

            btnOK.setOnClickListener {
                val str = editText.text.toString()
                if (str.isNotBlank()) {
                    adapter.addItem(str.trim())
                }
                dialog.dismiss()
            }
        }

        dialog.show()
    }
}