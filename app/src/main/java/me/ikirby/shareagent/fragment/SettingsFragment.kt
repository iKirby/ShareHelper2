package me.ikirby.shareagent.fragment

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import me.ikirby.shareagent.*
import me.ikirby.shareagent.contextual.Prefs
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

class SettingsFragment : PreferenceFragmentCompat() {

    companion object {
        const val REQUEST_CHOOSE_SAVE_DIRECTORY = 1
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        if (savedInstanceState == null) {
            addPreferencesFromResource(R.xml.preferences)

            val aboutPreference = findPreference<Preference>(Prefs.PREF_ABOUT)
            aboutPreference?.summary = BuildConfig.VERSION_NAME

            val saveDirectoryPreference = findPreference<Preference>(Prefs.PREF_SAVE_DIRECTORY)
            val saveDirectoryValue = App.prefs.saveDirectory
            saveDirectoryPreference?.summary = if (saveDirectoryValue != null) {
                URLDecoder.decode(saveDirectoryValue.toString(), StandardCharsets.UTF_8.name())
            } else {
                getString(R.string.directory_not_set)
            }
            saveDirectoryPreference?.setOnPreferenceClickListener {
                chooseSaveDirectory()
                true
            }

            val paramsRemoveListPreference = findPreference<Preference>(Prefs.PREF_REMOVE_PARAMS)
            paramsRemoveListPreference?.setOnPreferenceClickListener {
                val intent = Intent(requireContext(), ParamsConfigActivity::class.java)
                startActivity(intent)
                true
            }

            val privacyPolicyPreference = findPreference<Preference>(Prefs.PREF_PRIVACY_POLICY)
            privacyPolicyPreference?.setOnPreferenceClickListener {
                showPrivacyPolicy()
                true
            }

            val errorLogPreference = findPreference<Preference>(Prefs.PREF_ERROR_LOG)
            errorLogPreference?.setOnPreferenceClickListener {
                val intent = Intent(requireContext(), ErrorLogActivity::class.java)
                startActivity(intent)
                true
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (data == null) {
            return
        }

        if (requestCode == REQUEST_CHOOSE_SAVE_DIRECTORY) {
            onChooseSaveDirectoryResult(resultCode, data)
        }
    }

    private fun chooseSaveDirectory() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
            addFlags(Intent.FLAG_GRANT_PREFIX_URI_PERMISSION)
        }
        startActivityForResult(intent, REQUEST_CHOOSE_SAVE_DIRECTORY)
    }

    private fun onChooseSaveDirectoryResult(resultCode: Int, data: Intent) {
        val uri = data.data
        if (resultCode != Activity.RESULT_OK || uri == null) {
            return
        }

        val flags = data.flags and
                (Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        requireContext().contentResolver.takePersistableUriPermission(uri, flags)
        App.prefs.saveDirectory = uri
        findPreference<Preference>(Prefs.PREF_SAVE_DIRECTORY)?.summary =
            URLDecoder.decode(uri.toString(), StandardCharsets.UTF_8.name())
    }

    private fun showPrivacyPolicy() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.privacy_policy)
            .setMessage(R.string.privacy_policy_content)
            .show()
    }
}