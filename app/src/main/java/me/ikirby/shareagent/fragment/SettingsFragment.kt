package me.ikirby.shareagent.fragment

import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreference
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import me.ikirby.osscomponent.OSSComponentsActivity
import me.ikirby.shareagent.*
import me.ikirby.shareagent.contextual.Prefs
import me.ikirby.shareagent.util.resolveBrowsers
import me.ikirby.shareagent.widget.showSingleChoiceDialog
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

class SettingsFragment : PreferenceFragmentCompat() {

    companion object {
        private const val REQUEST_CHOOSE_SAVE_DIRECTORY = 1
        private const val REQUEST_CHOOSE_TEXT_DIRECTORY = 2
    }

    private val openChooseSaveDirectory =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            val intent = it.data
            if (intent != null) {
                onChooseDirectoryResult(REQUEST_CHOOSE_SAVE_DIRECTORY, it.resultCode, intent)
            }
        }

    private val openChooseTextDirectory =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            val intent = it.data
            if (intent != null) {
                onChooseDirectoryResult(REQUEST_CHOOSE_TEXT_DIRECTORY, it.resultCode, intent)
            }
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
                chooseDirectory(REQUEST_CHOOSE_SAVE_DIRECTORY)
                true
            }

            val textDirectoryPreference = findPreference<Preference>(Prefs.PREF_TEXT_DIRECTORY)
            val textDirectoryValue = App.prefs.textDirectory
            textDirectoryPreference?.summary = if (textDirectoryValue != null) {
                URLDecoder.decode(textDirectoryValue.toString(), StandardCharsets.UTF_8.name())
            } else {
                getString(R.string.summary_text_save_directory)
            }
            textDirectoryPreference?.setOnPreferenceClickListener {
                chooseDirectory(REQUEST_CHOOSE_TEXT_DIRECTORY)
                true
            }

            val paramsRemoveListPreference = findPreference<Preference>(Prefs.PREF_REMOVE_PARAMS)
            paramsRemoveListPreference?.setOnPreferenceClickListener {
                val intent = Intent(requireContext(), ParamsConfigActivity::class.java)
                startActivity(intent)
                true
            }

            val enableBrowserPreference =
                findPreference<SwitchPreference>(Prefs.PREF_ENABLE_BROWSER_ACTIVITY)
            enableBrowserPreference?.isChecked = isBrowserActivityEnabled()
            enableBrowserPreference?.setOnPreferenceChangeListener { _, newValue ->
                setBrowserActivityEnabled(newValue == true)
                true
            }

            val defaultBrowserPreference = findPreference<Preference>(Prefs.PREF_DEFAULT_BROWSER)
            App.prefs.defaultBrowser?.let {
                defaultBrowserPreference?.summary = it.label
            }
            defaultBrowserPreference?.setOnPreferenceClickListener {
                chooseBrowser()
                true
            }

            val privacyPolicyPreference = findPreference<Preference>(Prefs.PREF_PRIVACY_POLICY)
            privacyPolicyPreference?.setOnPreferenceClickListener {
                showPrivacyPolicy()
                true
            }

            val shareAppPreference = findPreference<Preference>(Prefs.SHARE_APP)
            shareAppPreference?.setOnPreferenceClickListener {
                val shareIntent = Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_SUBJECT, "ShareHelper")
                    putExtra(
                        Intent.EXTRA_TEXT,
                        "https://play.google.com/store/apps/details?id=me.ikirby.shareagent"
                    )
                    type = "text/plain"
                }
                startActivity(Intent.createChooser(shareIntent, getString(R.string.share_with)))
                true
            }

            val playStorePreference = findPreference<Preference>(Prefs.PLAY_STORE)
            playStorePreference?.setOnPreferenceClickListener {
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    data =
                        Uri.parse("https://play.google.com/store/apps/details?id=me.ikirby.shareagent")
                }
                startActivity(intent)
                true
            }

            val sourceCodePreference = findPreference<Preference>(Prefs.SOURCE_CODE)
            sourceCodePreference?.setOnPreferenceClickListener {
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    data = Uri.parse("https://github.com/iKirby/ShareHelper2")
                }
                startActivity(intent)
                true
            }

            val ossComponentsPreference = findPreference<Preference>(Prefs.OPEN_SOURCE_COMPONENTS)
            ossComponentsPreference?.setOnPreferenceClickListener {
                val intent = Intent(requireContext(), OSSComponentsActivity::class.java)
                startActivity(intent)
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

    private fun chooseDirectory(requestCode: Int) {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
            addFlags(Intent.FLAG_GRANT_PREFIX_URI_PERMISSION)
        }
        if (requestCode == REQUEST_CHOOSE_SAVE_DIRECTORY) {
            openChooseSaveDirectory.launch(intent)
        } else {
            openChooseTextDirectory.launch(intent)
        }
    }

    private fun onChooseDirectoryResult(requestCode: Int, resultCode: Int, data: Intent) {
        if (requestCode != REQUEST_CHOOSE_SAVE_DIRECTORY && requestCode != REQUEST_CHOOSE_TEXT_DIRECTORY) {
            return
        }

        val uri = data.data
        if (resultCode != Activity.RESULT_OK || uri == null) {
            return
        }

        val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        requireContext().contentResolver.takePersistableUriPermission(uri, flags)

        if (requestCode == REQUEST_CHOOSE_SAVE_DIRECTORY) {
            App.prefs.saveDirectory = uri
            findPreference<Preference>(Prefs.PREF_SAVE_DIRECTORY)?.summary =
                URLDecoder.decode(uri.toString(), StandardCharsets.UTF_8.name())
        } else {
            App.prefs.textDirectory = uri
            findPreference<Preference>(Prefs.PREF_TEXT_DIRECTORY)?.summary =
                URLDecoder.decode(uri.toString(), StandardCharsets.UTF_8.name())
        }
    }

    private fun showPrivacyPolicy() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.privacy_policy)
            .setMessage(R.string.privacy_policy_content)
            .show()
    }

    private fun isBrowserActivityEnabled(): Boolean {
        val pm = requireContext().packageManager
        val state = pm.getComponentEnabledSetting(
            ComponentName(
                requireContext(),
                "me.ikirby.shareagent.BrowserBridgeActivity"
            )
        )
        return state == PackageManager.COMPONENT_ENABLED_STATE_ENABLED
    }

    private fun setBrowserActivityEnabled(enabled: Boolean) {
        val pm = requireContext().packageManager
        val state = if (enabled) {
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED
        } else {
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED
        }
        pm.setComponentEnabledSetting(
            ComponentName(requireContext(), "me.ikirby.shareagent.BrowserBridgeActivity"),
            state,
            PackageManager.DONT_KILL_APP
        )
    }

    private fun chooseBrowser() {
        val list = resolveBrowsers(requireContext())
        if (list.isEmpty()) {
            requireContext().showToast(R.string.no_available_browsers)
            return
        }
        val current = App.prefs.defaultBrowser
        val checkedItem = if (current != null) {
            list.indexOf(current)
        } else {
            -1
        }
        val labelList = mutableListOf<String>()
        list.forEach {
            labelList.add(it.label)
        }
        showSingleChoiceDialog(
            requireActivity(),
            R.string.default_browser,
            labelList.toTypedArray(),
            checkedItem,
            {
                val appItem = list[it]
                App.prefs.defaultBrowser = appItem
                val defaultBrowserPreference =
                    findPreference<Preference>(Prefs.PREF_DEFAULT_BROWSER)
                defaultBrowserPreference?.summary = appItem.label
            },
            autoDismiss = true,
            negativeBtnResId = R.string.cancel,
            neutralBtnResId = R.string.clear_default,
            neutralCallback = {
                App.prefs.defaultBrowser = null
                val defaultBrowserPreference =
                    findPreference<Preference>(Prefs.PREF_DEFAULT_BROWSER)
                defaultBrowserPreference?.setSummary(R.string.summary_default_browser)
            }
        )
    }
}