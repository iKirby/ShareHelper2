package me.ikirby.shareagent.util

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
