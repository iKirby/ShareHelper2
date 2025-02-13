package me.ikirby.shareagent

import android.content.*
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.ikirby.shareagent.contextual.createFile
import me.ikirby.shareagent.contextual.getMimeType
import me.ikirby.shareagent.contextual.listTextFiles
import me.ikirby.shareagent.contextual.openTextFileForAppend
import me.ikirby.shareagent.databinding.ActivityShareBinding
import me.ikirby.shareagent.databinding.ShareActivityViewModel
import me.ikirby.shareagent.entity.AppItem
import me.ikirby.shareagent.util.Logger
import me.ikirby.shareagent.util.getTitleFromHtml
import me.ikirby.shareagent.util.removeParamsFromURL
import me.ikirby.shareagent.util.resolveBrowsers
import me.ikirby.shareagent.widget.*
import java.io.File
import java.nio.charset.StandardCharsets
import java.util.*

class ShareActivity : AppCompatActivity() {

    private val viewModel by lazy { ViewModelProvider(this)[ShareActivityViewModel::class.java] }
    private lateinit var binding: ActivityShareBinding

    private var uris = mutableListOf<Uri>()
    private val textFileList = mutableListOf<String>()

    private val openChooseSaveDirectory =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            it.data?.let { intent ->
                onChooseDirectoryResult(it.resultCode, intent)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_share)
        binding.viewmodel = viewModel
        binding.lifecycleOwner = this

        when (intent.action) {
            Intent.ACTION_SEND, Intent.ACTION_SEND_MULTIPLE -> {
                if (intent.type == "text/plain") {
                    viewModel.isText.value = true
                    handleText(intent)
                } else {
                    viewModel.isFile.value = true
                    handleFile(intent)
                }
            }

            Intent.ACTION_PROCESS_TEXT -> {
                viewModel.isText.value = true
                handleText(intent)
            }

            else -> {
                unsupported()
                return
            }
        }

        binding.actionOpenInBrowser.setOnClickListener {
            openInBrowserAction()
        }
        binding.actionFetchTitle.setOnClickListener {
            fetchTitle()
        }
        binding.actionCopy.setOnClickListener {
            copyText()
        }
        binding.actionShare.setOnClickListener {
            shareTextMenu()
        }
        binding.actionSave.setOnClickListener {
            if (viewModel.isText.value == true) {
                if ((App.prefs.saveDirectory ?: App.prefs.textDirectory) == null) {
                    promptChooseDirectory()
                    return@setOnClickListener
                }
                if (App.prefs.askForTextFileName) {
                    promptFileName()
                } else {
                    saveFile()
                }
            } else if (App.prefs.saveDirectory == null) {
                promptChooseDirectory()
                return@setOnClickListener
            } else {
                saveFile()
            }
        }
        binding.actionAddTo.setOnClickListener {
            showChooseFileDialog()
        }
        if (viewModel.isText.value == true) {
            binding.contentView.setOnClickListener {
                showEditDialog()
            }
        }
    }

    private fun unsupported() {
        showToast(R.string.unsupported_intent)
        finish()
    }

    private fun handleText(intent: Intent) {
        var text = if (intent.action == Intent.ACTION_SEND_MULTIPLE) {
            intent.getStringArrayListExtra(Intent.EXTRA_TEXT)?.joinToString("\n")
                ?: intent.getStringArrayExtra(Intent.EXTRA_TEXT)?.joinToString("\n")
        } else {
            intent.getStringExtra(Intent.EXTRA_TEXT)
        }
        if (text == null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            intent.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT)?.toString()
        }
        if (text == null) {
            handleFile(intent)
            return
        }

        val isURL = text.isURL()
        if (isURL) {
            viewModel.url.value = text
            if (App.prefs.removeURLParamsEnabled) {
                text = removeParamsFromURL(text, App.prefs.removeParams)
            }
        }

        val subject = intent.getStringExtra(Intent.EXTRA_SUBJECT) ?: ""
        val content = if (subject.isNotBlank() && !text.contains(subject)) {
            "$subject\n$text"
        } else {
            text
        }
        viewModel.subject.value = if (subject == content) "" else subject
        viewModel.content.value = content

        (App.prefs.textDirectory ?: App.prefs.saveDirectory)?.let { uri ->
            lifecycleScope.launch {
                val list = withContext(Dispatchers.IO) {
                    listTextFiles(this@ShareActivity, uri)
                }
                if (list.isNotEmpty()) {
                    textFileList.addAll(list)
                    viewModel.canAdd.value = true
                }
            }
        }

        if (App.prefs.allowInternet && isURL && viewModel.subject.value.isNullOrBlank()) {
            if (App.prefs.fetchTitleAutomatically) {
                fetchTitle()
            } else {
                viewModel.canFetchTitle.value = true
            }
        }
    }

    private fun handleFile(intent: Intent) {
        if (intent.action == Intent.ACTION_SEND_MULTIPLE) {
            val uriList = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM, Uri::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM)
            }
            if (!uriList.isNullOrEmpty()) {
                uris.addAll(uriList.filter { it.path != null })
            }
        } else {
            val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.data ?: intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.data ?: intent.getParcelableExtra(Intent.EXTRA_STREAM)
            }
            if (uri?.path != null) {
                uris.add(uri)
            }
        }
        if (uris.isEmpty()) {
            unsupported()
        }
        if (uris.size > 1) {
            viewModel.content.value = getString(R.string.file_count, uris.size)
        } else {
            val fileName = File(uris.first().path!!).name
            viewModel.content.value = fileName
        }
    }

    private fun getTextFileName(): String {
        return if (viewModel.subject.value.isNullOrBlank()) {
            "Text_${Date().format()}"
        } else {
            viewModel.subject.value!!.toFileName()
        }
    }

    private fun copyText() {
        val text = viewModel.content.value
        if (text != null) {
            val clipboardManager = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("ShareHelperCopy", text)
            clipboardManager.setPrimaryClip(clip)

            val toastText = if (text.length > 20) {
                text.substring(0..20) + "…"
            } else {
                text
            }
            showToast(getString(R.string.copied_text, toastText))
        }
        finish()
    }

    private fun shareTextMenu() {
        when (App.prefs.shareOption) {
            1 -> shareText(false)
            2 -> shareText(true)
            else -> showSingleSelectDialog(
                this, R.string.share_option,
                arrayOf(getString(R.string.merge_subject), getString(R.string.separate_subject))
            ) {
                if (it == 0) {
                    shareText(false)
                } else {
                    shareText(true)
                }
            }
        }
    }

    private fun shareText(separate: Boolean) {
        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            type = "text/plain"
            if (separate) {
                val newlineIndex = viewModel.content.value!!.indexOf("\n")
                if (newlineIndex != -1) {
                    // has subject
                    val subject = viewModel.content.value!!.substring(0, newlineIndex)
                    val content = viewModel.content.value!!.substring(newlineIndex + 1)
                    putExtra(Intent.EXTRA_SUBJECT, subject)
                    putExtra(Intent.EXTRA_TEXT, content)
                } else {
                    // no subject
                    putExtra(Intent.EXTRA_TEXT, viewModel.content.value)
                }
            } else {
                putExtra(Intent.EXTRA_TEXT, viewModel.content.value)
            }
        }
        val intent = Intent.createChooser(shareIntent, getString(R.string.share_with)).apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                putExtra(
                    Intent.EXTRA_EXCLUDE_COMPONENTS,
                    arrayOf(ComponentName(applicationContext, ShareActivity::class.java))
                )
            }
        }
        viewModel.selectedAction.value = true
        startActivity(intent)
    }

    override fun onResume() {
        super.onResume()
        if (viewModel.selectedAction.value == true) {
            finish()
        }
    }

    private fun saveFile() {
        viewModel.processing.value = true
        if (uris.isNotEmpty()) {
            val saveDirectoryUri = App.prefs.saveDirectory
            if (saveDirectoryUri == null) {
                showToast(R.string.save_directory_not_set)
                finish()
                return
            }
            saveOtherFile(uris, saveDirectoryUri)
        } else {
            val saveDirectoryUri = App.prefs.textDirectory ?: App.prefs.saveDirectory
            if (saveDirectoryUri == null) {
                showToast(R.string.save_directory_not_set)
                finish()
                return
            }
            val file = createFile(
                this,
                saveDirectoryUri,
                "text/plain",
                "${getTextFileName()}.txt"
            )
            if (file == null) {
                showToast(R.string.create_file_failed)
                finish()
                return
            }
            saveTextFile(viewModel.content.value!!, file)
        }
    }

    private fun saveOtherFile(fromUris: List<Uri>, saveDirectoryUri: Uri) {
        lifecycleScope.launch {
            runCatching {
                fromUris.forEach {
                    val file = createFile(
                        this@ShareActivity,
                        saveDirectoryUri,
                        getMimeType(contentResolver, it) ?: "application/octet-stream",
                        File(it.path!!).name
                    )
                    if (file == null) {
                        showToast(R.string.create_file_failed)
                        finish()
                        return@launch
                    }
                    withContext(Dispatchers.IO) {
                        val inputStream = contentResolver.openInputStream(it)
                        if (inputStream != null) {
                            inputStream.buffered().use { input ->
                                contentResolver.openOutputStream(file.uri)?.use { output ->
                                    val buffer = ByteArray(1024)
                                    while (true) {
                                        val length = input.read(buffer)
                                        if (length > 0) {
                                            output.write(buffer, 0, length)
                                        } else {
                                            break
                                        }
                                    }
                                }
                            }
                            inputStream.close()
                        }
                    }
                }
            }.onSuccess {
                showToast(R.string.file_saved)
            }.onFailure {
                Logger.e(it)
                showToast(R.string.write_file_failed)
            }
            finish()
        }
    }

    private fun saveTextFile(content: String, targetFile: DocumentFile) {
        lifecycleScope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    contentResolver.openOutputStream(targetFile.uri)?.use { output ->
                        output.write(content.toByteArray(StandardCharsets.UTF_8))
                    }
                }
            }.onSuccess {
                showToast(R.string.text_file_saved)
            }.onFailure {
                Logger.e(it)
                showToast(R.string.write_file_failed)
            }
            finish()
        }
    }

    private fun promptFileName() {
        showSingleInputDialog(this, R.string.enter_file_name, {
            viewModel.subject.value = it
            saveFile()
        }, getTextFileName())
    }

    private fun showChooseFileDialog() {
        showSingleChoiceDialog(
            this,
            R.string.append_choose_dialog_title,
            textFileList.toTypedArray(),
            -1,
            positiveBtnResId = R.string.action_append,
            positiveCallback = {
                appendToFile(textFileList[it])
            },
            negativeBtnResId = R.string.action_insert,
            negativeCallback = {
                insertToFile(textFileList[it])
            }
        )
    }

    private fun appendToFile(fileName: String) {
        val directoryUri = App.prefs.textDirectory ?: App.prefs.saveDirectory
        if (directoryUri == null) {
            showToast(R.string.save_directory_not_set)
            finish()
            return
        }

        val file = openTextFileForAppend(this, directoryUri, fileName)
        if (file == null) {
            showToast(R.string.open_selected_file_failed)
            showChooseFileDialog()
            return
        }

        viewModel.processing.value = true
        val content = "${App.prefs.appendSeparator}${viewModel.content.value}"
        lifecycleScope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    contentResolver.openOutputStream(file.uri, "wa")?.use {
                        it.write(content.toByteArray(StandardCharsets.UTF_8))
                    }
                }
            }.onSuccess {
                showToast(getString(R.string.content_appended, fileName))
            }.onFailure {
                Logger.e(it)
                showToast(R.string.write_file_failed)
            }
            finish()
        }
    }

    private fun insertToFile(fileName: String) {
        val directoryUri = App.prefs.textDirectory ?: App.prefs.saveDirectory
        if (directoryUri == null) {
            showToast(R.string.save_directory_not_set)
            finish()
            return
        }

        val file = openTextFileForAppend(this, directoryUri, fileName)
        if (file == null) {
            showToast(R.string.open_selected_file_failed)
            showChooseFileDialog()
            return
        }

        viewModel.processing.value = true
        val content = "${viewModel.content.value}${App.prefs.appendSeparator}"
        lifecycleScope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    val originalContent = contentResolver.openInputStream(file.uri)?.use {
                        it.readBytes()
                    }
                    contentResolver.openOutputStream(file.uri, "rw")?.use {
                        it.write(content.toByteArray(StandardCharsets.UTF_8))
                        it.write(originalContent)
                    }
                }
            }.onSuccess {
                showToast(getString(R.string.content_appended, fileName))
            }.onFailure {
                Logger.e(it)
                showToast(R.string.write_file_failed)
            }
            finish()
        }
    }


    private fun showEditDialog() {
        showMultilineInputDialog(this, R.string.edit_content, {
            if (it.isNotBlank()) {
                viewModel.content.value = it
            }
        }, viewModel.content.value ?: "")
    }

    private fun openInBrowserAction() {
        val defaultBrowser = App.prefs.defaultBrowser
        val availableBrowsers = resolveBrowsers(this)
        if (defaultBrowser != null) {
            if (availableBrowsers.contains(defaultBrowser)) {
                openInBrowser(defaultBrowser)
            } else if (availableBrowsers.isNotEmpty()) {
                openInBrowser()
            }
        } else if (availableBrowsers.isNotEmpty()) {
            openInBrowser()
        }
        finish()
    }

    private fun openInBrowser(defaultBrowser: AppItem? = null) {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse(viewModel.url.value!!)
            if (defaultBrowser != null) {
                component = ComponentName(defaultBrowser.packageName, defaultBrowser.name)
            }
        }
        try {
            startActivity(intent)
        } catch (_: Exception) {
            showToast(R.string.no_available_browsers)
        }
    }

    private fun fetchTitle() {
        viewModel.processing.value = true
        lifecycleScope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    getTitleFromHtml(viewModel.url.value!!)
                }
            }.onSuccess {
                if (it.isNotBlank()) {
                    viewModel.content.value = "$it\n${viewModel.url.value}"
                }
                viewModel.canFetchTitle.value = false
            }.onFailure {
                Logger.e(it)
                showToast(R.string.error_fetching_title)
            }
            viewModel.processing.value = false
        }
    }

    private fun promptChooseDirectory() {
        showPromptDialog(this, R.string.save_directory, R.string.save_directory_unavailable_tip) {
            chooseSaveDirectory()
        }
    }

    private fun chooseSaveDirectory() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
            addFlags(Intent.FLAG_GRANT_PREFIX_URI_PERMISSION)
        }
        openChooseSaveDirectory.launch(intent)
    }

    private fun onChooseDirectoryResult(resultCode: Int, data: Intent) {
        val uri = data.data
        if (resultCode != RESULT_OK || uri == null) {
            return
        }
        val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        contentResolver.takePersistableUriPermission(uri, flags)
        App.prefs.saveDirectory = uri
    }

}
