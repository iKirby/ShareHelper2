package me.ikirby.shareagent

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.annotation.StringRes
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
import me.ikirby.shareagent.databinding.ActivityShareBinding
import me.ikirby.shareagent.databinding.ShareActivityViewModel
import me.ikirby.shareagent.util.removeParamsFromURL
import me.ikirby.shareagent.widget.showSingleInputDialog
import java.io.File
import java.nio.charset.StandardCharsets
import java.util.*

class ShareActivity : AppCompatActivity() {

    private val viewModel by lazy { ViewModelProvider(this).get(ShareActivityViewModel::class.java) }
    private lateinit var binding: ActivityShareBinding

    private var uri: Uri? = null

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
    }

    private fun unsupported() {
        App.showToast(R.string.unsupported_intent)
        finish()
    }

    private fun handleText(intent: Intent) {
        var text = intent.getStringExtra(Intent.EXTRA_TEXT)
            ?: intent.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT)?.toString()
        if (text == null) {
            handleFile(intent)
            return
        }
        if (text.isURL() && App.prefs.removeURLParamsEnabled) {
            text = removeParamsFromURL(text, App.prefs.removeParams)
        }

        val subject = intent.getStringExtra(Intent.EXTRA_SUBJECT) ?: ""
        val content = if (subject.isNotBlank() && !text.contains(subject)) {
            "$subject\n$text"
        } else {
            text
        }
        viewModel.subject.value = subject
        viewModel.content.value = content
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
            App.showToast(getString(R.string.copied_text, toastText))
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
        val saveDirectoryUri = App.prefs.saveDirectory
        if (saveDirectoryUri == null) {
            App.showToast(R.string.save_directory_not_set)
            finish()
            return
        }

        viewModel.processing.value = true
        if (uri != null) {
            val file = createFile(
                this,
                saveDirectoryUri,
                getMimeType(contentResolver, uri!!) ?: "application/octet-stream",
                viewModel.content.value!!
            )
            if (file == null) {
                App.showToast(R.string.create_file_failed)
                finish()
                return
            }
            saveOtherFile(uri!!, file)
        } else {
            val file = createFile(
                this,
                saveDirectoryUri,
                "text/plain",
                "${getTextFileName()}.txt"
            )
            if (file == null) {
                App.showToast(R.string.create_file_failed)
                finish()
                return
            }
            saveTextFile(viewModel.content.value!!, file)
        }
    }

    private fun saveOtherFile(fromUri: Uri, targetFile: DocumentFile) {
        lifecycleScope.launch {
            var error = true
            withContext(Dispatchers.IO) {
                contentResolver.openInputStream(fromUri)?.use { input ->
                    contentResolver.openOutputStream(targetFile.uri)?.use { output ->
                        try {
                            output.write(input.readBytes())
                            error = false
                        } catch (e: Exception) {
                            Log.d("WriteFile", Log.getStackTraceString(e))
                            App.showToast(R.string.write_file_failed)
                        }
                    } ?: App.showToast(R.string.open_output_file_failed)
                } ?: App.showToast(R.string.read_file_failed)
            }
            if (!error) {
                App.showToast(R.string.file_saved)
            }
            finish()
        }
    }

    private fun saveTextFile(content: String, targetFile: DocumentFile) {
        lifecycleScope.launch {
            var error = true
            withContext(Dispatchers.IO) {
                contentResolver.openOutputStream(targetFile.uri)?.use { output ->
                    try {
                        output.write(content.toByteArray(StandardCharsets.UTF_8))
                        error = false
                    } catch (e: Exception) {
                        Log.d("WriteFile", Log.getStackTraceString(e))
                        App.showToast(R.string.write_file_failed)
                    }
                } ?: App.showToast(R.string.open_output_file_failed)
            }
            if (!error) {
                App.showToast(R.string.text_file_saved)
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

}
