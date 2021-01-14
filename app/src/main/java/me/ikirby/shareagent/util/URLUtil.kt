package me.ikirby.shareagent.util

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import me.ikirby.shareagent.R
import me.ikirby.shareagent.entity.AppItem
import me.ikirby.shareagent.showToast

fun removeParamsFromURL(urlWithParams: String, paramsToRemove: List<String>): String {
    if (!urlWithParams.contains("?")) {
        return urlWithParams
    }
    if (paramsToRemove.isEmpty()) {
        return urlWithParams
    }

    var url = urlWithParams.substringBefore("?")
    val currentParams = urlWithParams.substringAfter("?").split("&").toList()

    val paramsMap = mutableMapOf<String, String>()
    currentParams.forEach {
        val arr = it.split("=")
        if (!paramsToRemove.contains(arr[0])) {
            if (arr.size > 1) {
                paramsMap[arr[0]] = arr[1]
            } else {
                paramsMap[arr[0]] = ""
            }
        }
    }
    if (paramsMap.isNotEmpty()) {
        var isFirst = true
        for (entry: Map.Entry<String, String> in paramsMap) {
            if (isFirst) {
                url = "$url?${entry.key}=${entry.value}"
                isFirst = false
            } else {
                url = "$url&${entry.key}=${entry.value}"
            }
        }
    }
    return url
}

fun resolveBrowsers(context: Context): List<AppItem> {
    val pm = context.packageManager
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://"))

    val flag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        PackageManager.MATCH_ALL
    } else {
        0
    }
    val resolution = pm.queryIntentActivities(intent, flag)
    val list = mutableListOf<AppItem>()
    resolution.forEach {
        if (it.activityInfo.packageName != context.packageName) {
            list.add(
                AppItem(
                    it.loadLabel(pm).toString(),
                    it.activityInfo.packageName,
                    it.activityInfo.name
                )
            )
        }
    }
    return list
}
