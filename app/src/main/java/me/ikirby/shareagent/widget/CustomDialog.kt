package me.ikirby.shareagent.widget

import android.app.Activity
import android.app.Dialog
import android.widget.EditText
import androidx.annotation.StringRes
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import me.ikirby.shareagent.R

fun showSingleInputDialog(
    activity: Activity,
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

fun showMultilineInputDialog(
    activity: Activity,
    @StringRes titleResId: Int,
    callback: (String) -> Unit,
    defaultValue: String = ""
) {
    val dialog = MaterialAlertDialogBuilder(activity)
        .setTitle(titleResId)
        .setView(R.layout.dialog_multi_line_input)
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
    activity: Activity,
    @StringRes titleResId: Int,
    items: Array<String>,
    callback: (Int) -> Unit,
    @StringRes neutralBtnResId: Int? = null,
    neutralCallback: (() -> Unit)? = null
) {
    val builder = MaterialAlertDialogBuilder(activity)
        .setTitle(titleResId)
        .setNegativeButton(R.string.cancel, null)
        .setItems(items) { _, index ->
            callback(index)
        }

    if (neutralBtnResId != null && neutralCallback != null) {
        builder.setNeutralButton(neutralBtnResId) { _, _ ->
            neutralCallback()
        }
    }
    builder.show()
}

fun showPromptDialog(
    activity: Activity,
    @StringRes titleResId: Int,
    @StringRes contentResId: Int,
    callback: () -> Unit
) {
    MaterialAlertDialogBuilder(activity)
        .setTitle(titleResId)
        .setMessage(contentResId)
        .setPositiveButton(R.string.ok) { _, _ ->
            callback()
        }
        .setNegativeButton(R.string.cancel, null)
        .show()
}
