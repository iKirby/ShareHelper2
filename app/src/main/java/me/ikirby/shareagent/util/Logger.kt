package me.ikirby.shareagent.util

import android.util.Log
import me.ikirby.shareagent.App
import me.ikirby.shareagent.BuildConfig

object Logger {
    private const val TAG = "ShareHelper"
    private const val TAG_DEBUG = "ShareHelperDEBUG"

    fun d(msg: String) {
        Log.d(if (BuildConfig.DEBUG) TAG_DEBUG else TAG, msg)
    }

    fun e(msg: String) {
        Log.e(TAG, msg)
    }

    fun e(t: Throwable) {
        val stackTraceString = Log.getStackTraceString(t)
        App.prefs.lastError = stackTraceString
        e(stackTraceString)
    }
}
