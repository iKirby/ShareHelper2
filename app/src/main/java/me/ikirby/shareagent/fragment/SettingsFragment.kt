package me.ikirby.shareagent.fragment

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceFragmentCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.ikirby.shareagent.App
import me.ikirby.shareagent.BuildConfig
import me.ikirby.shareagent.ParamsConfigActivity
import me.ikirby.shareagent.R
import me.ikirby.shareagent.contextual.Prefs
import me.ikirby.shareagent.contextual.createFile
import me.ikirby.shareagent.contextual.listTextFiles
import me.ikirby.shareagent.contextual.openTextFileForAppend
import me.ikirby.shareagent.util.Logger
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

            // debug preferences
            if (BuildConfig.DEBUG) {
                val debugPreferenceCategory =
                    findPreference<PreferenceCategory>(Prefs.PREF_CATEGORY_DEBUG)
                debugPreferenceCategory?.isVisible = true

                val writeTestFilePreference = findPreference<Preference>(Prefs.PREF_WRITE_TEST_FILE)
                writeTestFilePreference?.setOnPreferenceClickListener {
                    writeTestFile()
                    true
                }

                val listAppendableTextPreference =
                    findPreference<Preference>(Prefs.PREF_LIST_APPENDABLE_TEXT)
                listAppendableTextPreference?.setOnPreferenceClickListener {
                    logTextFileList()
                    true
                }

                val testAppendPreference = findPreference<Preference>(Prefs.PREF_APPEND_TEST_FILE)
                testAppendPreference?.setOnPreferenceClickListener {
                    testAppend()
                    true
                }
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

    private fun writeTestFile() {
        val saveDirectoryUri = App.prefs.saveDirectory
        if (saveDirectoryUri == null) {
            Logger.d("Save dir not set")
            return
        }

        val file = createFile(
            requireContext(),
            saveDirectoryUri,
            "text/plain",
            "ShareHelperTestFile.txt"
        )
        if (file == null) {
            Logger.d("Create file failed")
            return
        }

        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                requireContext().contentResolver.openOutputStream(file.uri)?.use {
                    it.write("TEST_FILE".toByteArray(StandardCharsets.UTF_8))
                }
            }
            Logger.d("Test file written")
        }
    }

    private fun logTextFileList() {
        val saveDirectoryUri = App.prefs.saveDirectory
        if (saveDirectoryUri == null) {
            Logger.d("Save dir not set")
            return
        }

        listTextFiles(requireContext(), saveDirectoryUri).forEach {
            Logger.d(it)
        }
    }

    private fun testAppend() {
        val saveDirectoryUri = App.prefs.saveDirectory
        if (saveDirectoryUri == null) {
            Logger.d("Save dir not set")
            return
        }

        val file = openTextFileForAppend(
            requireContext(),
            saveDirectoryUri,
            "ShareHelperTestFile.txt"
        )
        if (file == null) {
            Logger.d("Open text file for append failed")
            return
        }

        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                requireContext().contentResolver.openOutputStream(file.uri, "wa")?.use {
                    it.write("\nTEST_FILE".toByteArray(StandardCharsets.UTF_8))
                }
            }
            Logger.d("Appended to file")
        }
    }
}