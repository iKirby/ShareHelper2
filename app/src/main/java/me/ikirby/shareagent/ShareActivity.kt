package me.ikirby.shareagent

import android.content.*
import android.net.Uri
import android.os.Bundle
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
import me.ikirby.shareagent.widget.showMultilineInputDialog
import me.ikirby.shareagent.widget.showSingleInputDialog
import me.ikirby.shareagent.widget.showSingleSelectDialog
import java.io.File
import java.nio.charset.StandardCharsets
import java.util.*

class ShareActivity : AppCompatActivity() {

    private val viewModel by lazy { ViewModelProvider(this).get(ShareActivityViewModel::class.java) }
    private lateinit var binding: ActivityShareBinding

    private var uri: Uri? = null
    private val textFileList = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_share)
        binding.viewmodel = viewModel
        binding.lifecycleOwner = this

        when (intent.action) {
            Intent.ACTION_SEND -> {
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
        binding.actionCopy.setOnClickListener {
            copyText()
        }
        binding.actionShare.setOnClickListener {
            shareText()
        }
        binding.actionSave.setOnClickListener {
            if (App.prefs.askForTextFileName && viewModel.isText.value == true) {
                promptFileName()
            } else {
                saveFile()
            }
        }
        binding.actionAppend.setOnClickListener {
            showAppendSelectDialog()
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
        var text = intent.getStringExtra(Intent.EXTRA_TEXT)
            ?: intent.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT)?.toString()
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
        viewModel.subject.value = subject
        viewModel.content.value = content

        (App.prefs.textDirectory ?: App.prefs.saveDirectory)?.let { uri ->
            lifecycleScope.launch {
                val list = withContext(Dispatchers.IO) {
                    listTextFiles(this@ShareActivity, uri)
                }
                if (list.isNotEmpty()) {
                    textFileList.addAll(list)
                    viewModel.canAppend.value = true
                }
            }
        }

        if (App.prefs.allowInternet && isURL && subject.isBlank()) {
            viewModel.processing.value = true
            lifecycleScope.launch {
                runCatching {
                    withContext(Dispatchers.IO) {
                        getTitleFromHtml(text)
                    }
                }.onSuccess {
                    if (it.isNotBlank()) {
                        viewModel.content.value = "$it\n$text"
                    }
                }.onFailure {
                    Logger.e(it)
                    showToast(R.string.error_fetching_title)
                }
                viewModel.processing.value = false
            }
        }
    }

    private fun handleFile(intent: Intent) {
        val uri = intent.data ?: intent.getParcelableExtra(Intent.EXTRA_STREAM)
        if (uri != null) {
            val path = uri.path
            if (path == null) {
                unsupported()
                return
            }
            this.uri = uri
            val fileName = File(path).name
            viewModel.content.value = fileName
        } else {
            unsupported()
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
            val clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
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

    private fun shareText() {
        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, viewModel.content.value)
            type = "text/plain"
        }
        startActivity(Intent.createChooser(shareIntent, getString(R.string.share_with)))
        finish()
    }

    private fun saveFile() {
        viewModel.processing.value = true
        if (uri != null) {
            val saveDirectoryUri = App.prefs.saveDirectory
            if (saveDirectoryUri == null) {
                showToast(R.string.save_directory_not_set)
                finish()
                return
            }
            val file = createFile(
                this,
                saveDirectoryUri,
                getMimeType(contentResolver, uri!!) ?: "application/octet-stream",
                viewModel.content.value!!
            )
            if (file == null) {
                showToast(R.string.create_file_failed)
                finish()
                return
            }
            saveOtherFile(uri!!, file)
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

    private fun saveOtherFile(fromUri: Uri, targetFile: DocumentFile) {
        lifecycleScope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    contentResolver.openInputStream(fromUri)?.buffered()?.use { input ->
                        contentResolver.openOutputStream(targetFile.uri)?.use { output ->
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

    private fun showAppendSelectDialog() {
        showSingleSelectDialog(
            this,
            R.string.append_choose_dialog_title,
            textFileList.toTypedArray(),
            { appendToFile(textFileList[it]) }
        )
    }

    private fun appendToFile(fileName: String) {
        val saveDirectoryUri = App.prefs.saveDirectory
        if (saveDirectoryUri == null) {
            showToast(R.string.save_directory_not_set)
            finish()
            return
        }

        val file = openTextFileForAppend(this, saveDirectoryUri, fileName)
        if (file == null) {
            showToast(R.string.open_selected_file_failed)
            showAppendSelectDialog()
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
            } else {
                openInBrowser()
            }
        } else {
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
        } catch (e: Exception) {
            showToast(R.string.no_available_browsers)
        }
    }


}
