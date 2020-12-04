package me.ikirby.shareagent

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.annotation.StringRes
import java.net.MalformedURLException
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*

fun String.isURL(): Boolean {
    return try {
        URL(this)
        true
    } catch (e: MalformedURLException) {
        false
    }
}

fun Context.hasUriWritePermission(uri: Uri): Boolean {
    contentResolver.persistedUriPermissions.forEach {
        if (it.uri == uri && it.isWritePermission) {
            return true
        }
    }
    return false
}

fun Date.format(): String {
    val sdf = SimpleDateFormat("yyyyMMddHHmmss", Locale.US)
    return sdf.format(this)
}

fun String.toFileName(): String {
    return this.replace(Regex("\\W+"), "_")
}

fun Context.showToast(@StringRes resId: Int) {
    Toast.makeText(this, resId, Toast.LENGTH_LONG).show()
}

fun Context.showToast(str: String) {
    Toast.makeText(this, str, Toast.LENGTH_LONG).show()
}
