package me.ikirby.shareagent.contextual

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import androidx.documentfile.provider.DocumentFile
import me.ikirby.shareagent.util.Logger
import java.util.*

fun openDirectory(context: Context, uri: Uri): DocumentFile? {
    var hasPermission = false
    context.contentResolver.persistedUriPermissions.forEach {
        if (it.uri == uri && it.isReadPermission && it.isWritePermission) {
            hasPermission = true
        }
    }
    if (!hasPermission) {
        Logger.e("No permission, please set save dir again")
        return null
    }

    val dir = DocumentFile.fromTreeUri(context, uri)
    if (dir == null || !dir.isDirectory) {
        Logger.e("DocumentFile is null or is not a directory")
        return null
    }
    return dir
}

fun createFile(context: Context, directoryUri: Uri, mimeType: String, fileName: String): DocumentFile? {
    val dir = openDirectory(context, directoryUri)
    if (dir == null) {
        Logger.e("open directory failed")
        return null
    }

    val file = dir.createFile(mimeType, fileName)
    if (file == null) {
        Logger.e("Create new file failed")
        return null
    }
    return file
}

fun openTextFileForAppend(context: Context, directoryUri: Uri, fileName: String): DocumentFile? {
    val dir = openDirectory(context, directoryUri)
    if (dir == null) {
        Logger.e("open directory failed")
        return null
    }

    val file = dir.findFile(fileName)
    if (file == null) {
        Logger.e("File not found")
        return null
    }

    if (!file.isFile || !file.canWrite()) {
        Logger.e("Target is not a file or is not writable")
        return null
    }
    return file
}

fun listTextFiles(context: Context, directoryUri: Uri): List<String> {
    val dir = openDirectory(context, directoryUri)
    if (dir == null) {
        Logger.e("open directory failed")
        return emptyList()
    }

    if (!dir.isDirectory) {
        Logger.e("DocumentFile is not a directory, please set save dir again")
        return emptyList()
    }

    val list = mutableListOf<String>()
    dir.listFiles().forEach { file ->
        if (file.type == "text/plain" && file.canWrite()) {
            file.name?.let { list.add(it) }
        }
    }
    return list
}

fun getMimeType(contentResolver: ContentResolver, uri: Uri): String? {
    return if (uri.scheme == ContentResolver.SCHEME_CONTENT) {
        contentResolver.getType(uri)
    } else {
        val extension = MimeTypeMap.getFileExtensionFromUrl(uri.toString())
        MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension.lowercase(Locale.ROOT))
    }
}
