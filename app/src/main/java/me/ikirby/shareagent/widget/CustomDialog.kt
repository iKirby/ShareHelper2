package me.ikirby.shareagent.widget

import android.app.Dialog
import android.widget.EditText
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import me.ikirby.shareagent.R

fun showSingleInputDialog(
    activity: AppCompatActivity,
    @StringRes titleResId: Int,
    callback: (String) -> Unit,
    defaultValue: String = ""
) {
    val dialog = MaterialAlertDialogBuilder(activity)
        .setTitle(titleResId)
        .setView(R.layout.dialog_single_input)
        .setPositiveButton(R.string.ok, null)
        .setNegativeButton(R.string.cancel, null)
        .create()

    dialog.setOnShowListener {
        val btnOK = dialog.getButton(Dialog.BUTTON_POSITIVE)
        val editText = dialog.findViewById<EditText>(R.id.editText)!!
        editText.setText(defaultValue)

        btnOK.setOnClickListener {
            val str = editText.text.toString()
            callback(str.trim())
            dialog.dismiss()
        }
    }

    dialog.show()
}

fun showSingleSelectDialog(
    activity: AppCompatActivity,
    @StringRes titleResId: Int,
    items: Array<String>,
    callback: (Int) -> Unit
) {
    MaterialAlertDialogBuilder(activity)
        .setTitle(titleResId)
        .setNegativeButton(R.string.cancel, null)
        .setItems(items) { _, index ->
            callback(index)
        }
        .show()
}
