package me.ikirby.shareagent.widget

import android.app.Activity
import android.app.Dialog
import android.widget.EditText
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
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

fun showSingleChoiceDialog(
    activity: Activity,
    @StringRes titleResId: Int,
    items: Array<String>,
    checkedItem: Int,
    callback: ((Int) -> Unit)? = null,
    autoDismiss: Boolean = false,
    @StringRes positiveBtnResId: Int? = null,
    positiveCallback: ((Int) -> Unit)? = null,
    @StringRes negativeBtnResId: Int? = null,
    negativeCallback: ((Int) -> Unit)? = null,
    @StringRes neutralBtnResId: Int? = null,
    neutralCallback: ((Int) -> Unit)? = null
) {
    val builder = MaterialAlertDialogBuilder(activity)
        .setTitle(titleResId)
        .setSingleChoiceItems(items, checkedItem) { dialogInterface, index ->
            callback?.invoke(index)
            if (autoDismiss) {
                dialogInterface.dismiss()
            }
        }

    if (positiveBtnResId != null) {
        builder.setPositiveButton(positiveBtnResId) { dialogInterface, _ ->
            val position = (dialogInterface as AlertDialog).listView.checkedItemPosition
            positiveCallback?.invoke(position)
        }
    }

    if (negativeBtnResId != null) {
        builder.setNegativeButton(negativeBtnResId) { dialogInterface, _ ->
            val position = (dialogInterface as AlertDialog).listView.checkedItemPosition
            negativeCallback?.invoke(position)
        }
    }

    if (neutralBtnResId != null) {
        builder.setNeutralButton(neutralBtnResId) { dialogInterface, _ ->
            val position = (dialogInterface as AlertDialog).listView.checkedItemPosition
            neutralCallback?.invoke(position)
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

fun showSingleSelectDialog(
    activity: Activity,
    @StringRes titleResId: Int,
    items: Array<String>,
    selectCallback: (which: Int) -> Unit
) {
    MaterialAlertDialogBuilder(activity)
        .setTitle(titleResId)
        .setItems(items) { _, which ->
            selectCallback(which)
        }
        .setNegativeButton(R.string.cancel, null)
        .show()
}
