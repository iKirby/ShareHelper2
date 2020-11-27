package me.ikirby.shareagent

import android.content.Context
import android.net.Uri
import java.net.MalformedURLException
import java.net.URL
import java.util.Date
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
