package me.ikirby.shareagent

import android.content.*
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.webkit.MimeTypeMap
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
import me.ikirby.shareagent.databinding.ActivityShareBinding
import me.ikirby.shareagent.databinding.ShareActivityViewModel
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

        if (intent.action == Intent.ACTION_SEND) {
            if (intent.type == "text/plain") {
                viewModel.isText.value = true
                handleText(intent)
            } else {
                viewModel.isFile.value = true
                handleFile(intent)
            }
        } else {
            unsupported()
        }

        binding.actionCopy.setOnClickListener {
            copyText()
        }
        binding.actionShare.setOnClickListener {
            shareText()
        }
        binding.actionSave.setOnClickListener {
            saveFile()
        }
    }

    private fun showToast(@StringRes resId: Int) {
        Toast.makeText(this, resId, Toast.LENGTH_LONG).show()
    }

    private fun showToast(text: String) {
        Toast.makeText(this, text, Toast.LENGTH_LONG).show()
    }

    private fun unsupported() {
        showToast(R.string.unsupported_intent)
        finish()
    }

    private fun handleText(intent: Intent) {
        var text = intent.getStringExtra(Intent.EXTRA_TEXT)
        if (text == null) {
            handleFile(intent)
            return
        }
        if (text.isURL() && App.prefs.removeURLParamsEnabled) {
            text = removeParamsFromURL(text)
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

    private fun removeParamsFromURL(urlWithParams: String): String {
        if (!urlWithParams.contains("?")) {
            return urlWithParams
        }

        val paramsToRemove = App.prefs.removeParams
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

    private fun getMimeType(uri: Uri): String? {
        return if (uri.scheme == ContentResolver.SCHEME_CONTENT) {
            contentResolver.getType(uri)
        } else {
            val extension = MimeTypeMap.getFileExtensionFromUrl(uri.toString())
            MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension.toLowerCase(Locale.ROOT))
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
        val saveDirectoryUri = App.prefs.saveDirectory
        if (saveDirectoryUri == null) {
            showToast(R.string.save_directory_not_set)
            finish()
            return
        }

        val hasPermission = hasUriWritePermission(saveDirectoryUri)
        if (!hasPermission) {
            showToast(R.string.no_write_permission)
            finish()
            return
        }

        viewModel.processing.value = true
        val dir = DocumentFile.fromTreeUri(this, saveDirectoryUri)!!
        if (uri != null) {
            val file = dir.createFile(getMimeType(uri!!) ?: "application/octet-stream", viewModel.content.value!!)
            if (file == null) {
                showToast(R.string.create_file_failed)
                finish()
                return
            }
            saveOtherFile(uri!!, file)
        } else {
            val fileName = if (viewModel.subject.value.isNullOrBlank()) {
                "Text_${Date().format()}"
            } else {
                viewModel.subject.value!!.toFileName()
            }
            val file = dir.createFile("text/plain", "$fileName.txt")
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
            var error = true
            withContext(Dispatchers.IO) {
                contentResolver.openInputStream(fromUri)?.use { input ->
                    contentResolver.openOutputStream(targetFile.uri)?.use { output ->
                        try {
                            output.write(input.readBytes())
                            error = false
                        } catch (e: Exception) {
                            Log.d("WriteFile", Log.getStackTraceString(e))
                            showToast(R.string.write_file_failed)
                        }
                    } ?: showToast(R.string.open_output_file_failed)
                } ?: showToast(R.string.read_file_failed)
            }
            if (!error) {
                showToast(R.string.file_saved)
                finish()
            }
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
                        showToast(R.string.write_file_failed)
                    }
                } ?: showToast(R.string.open_output_file_failed)
            }
            if (!error) {
                showToast(R.string.text_file_saved)
                finish()
            }
        }
    }

}
