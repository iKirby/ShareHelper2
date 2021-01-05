package me.ikirby.shareagent.contextual

import android.content.Context
import android.net.Uri
import androidx.core.content.edit
import androidx.preference.PreferenceManager

class Prefs(context: Context) {

    companion object {
        const val PREF_ABOUT = "about"
        const val PREF_PRIVACY_POLICY = "privacy_policy"
        const val PREF_SAVE_DIRECTORY = "save_directory"
        const val PREF_TEXT_DIRECTORY = "text_directory"
        const val PREF_REMOVE_URL_PARAMS_ENABLED = "remove_url_params_enabled"
        const val PREF_REMOVE_PARAMS = "remove_params"
        const val PREF_ALLOW_INTERNET = "allow_internet"
        const val PREF_ASK_FOR_TEXT_FILE_NAME = "ask_text_file_name"
        const val PREF_APPEND_SEPARATOR = "append_separator"

        const val PREF_ERROR_LOG = "error_log"
        const val PREF_LAST_ERROR = "last_error"
    }

    private val preferences = PreferenceManager.getDefaultSharedPreferences(context)

    var saveDirectory: Uri?
        get() {
            val value = preferences.getString(PREF_SAVE_DIRECTORY, null)
            return if (value != null) {
                Uri.parse(value)
            } else {
                null
            }
        }
        set(value) {
            if (value != null) {
                preferences.edit { putString(PREF_SAVE_DIRECTORY, value.toString()) }
            } else {
                preferences.edit { remove(PREF_SAVE_DIRECTORY) }
            }
        }

    var textDirectory: Uri?
        get() {
            val value = preferences.getString(PREF_TEXT_DIRECTORY, null)
            return if (value != null) {
                Uri.parse(value)
            } else {
                null
            }
        }
        set(value) {
            if (value != null) {
                preferences.edit { putString(PREF_TEXT_DIRECTORY, value.toString()) }
            } else {
                preferences.edit { remove(PREF_TEXT_DIRECTORY) }
            }
        }

    val removeURLParamsEnabled: Boolean
        get() = preferences.getBoolean(PREF_REMOVE_URL_PARAMS_ENABLED, false)

    var removeParams: List<String>
        get() {
            val value = preferences.getString(PREF_REMOVE_PARAMS, "")!!
            return if (value.isNotBlank()) {
                value.split(",")
            } else {
                emptyList()
            }
        }
        set(value) {
            preferences.edit { putString(PREF_REMOVE_PARAMS, value.joinToString(",")) }
        }

    val allowInternet: Boolean
        get() = preferences.getBoolean(PREF_ALLOW_INTERNET, false)

    val askForTextFileName: Boolean
        get() = preferences.getBoolean(PREF_ASK_FOR_TEXT_FILE_NAME, false)

    var lastError: String?
        get() = preferences.getString(PREF_LAST_ERROR, null)
        set(value) {
            preferences.edit { putString(PREF_LAST_ERROR, value) }
        }

    val appendSeparator: String
        get() {
            val value = preferences.getString(PREF_APPEND_SEPARATOR, null)
            return if (value.isNullOrEmpty()) {
                "\n\n"
            } else {
                value
            }
        }

}