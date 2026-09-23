package com.example.util

import android.webkit.MimeTypeMap
import com.example.data.model.FileCategory
import java.io.File
import java.util.Locale

object MimeUtils {
    fun getExtension(file: File): String {
        val name = file.name
        val dotIndex = name.lastIndexOf('.')
        return if (dotIndex >= 0 && dotIndex < name.length - 1) {
            name.substring(dotIndex + 1).lowercase(Locale.ROOT)
        } else {
            ""
        }
    }

    fun getMimeType(file: File): String {
        if (file.isDirectory) return "inode/directory"
        val ext = getExtension(file)
        if (ext.isEmpty()) return "application/octet-stream"
        
        val mime = try {
            MimeTypeMap.getSingleton()?.getMimeTypeFromExtension(ext)
        } catch (e: Throwable) {
            null
        }
        if (!mime.isNullOrEmpty()) return mime

        return when (ext) {
            "kt", "java", "xml", "json", "c", "cpp", "h", "py", "sh", "js", "ts", "html", "css", "md", "txt", "gradle" -> "text/plain"
            "apk" -> "application/vnd.android.package-archive"
            "zip" -> "application/zip"
            "rar" -> "application/x-rar-compressed"
            "7z" -> "application/x-7z-compressed"
            "tar" -> "application/x-tar"
            "gz" -> "application/gzip"
            "pdf" -> "application/pdf"
            "epub" -> "application/epub+zip"
            "mp3", "m4a", "wav", "flac", "ogg", "aac" -> "audio/*"
            "mp4", "mkv", "mov", "avi", "webm", "3gp" -> "video/*"
            "jpg", "jpeg", "png", "webp", "gif", "bmp", "svg" -> "image/*"
            else -> "application/octet-stream"
        }
    }

    fun getCategory(file: File): FileCategory {
        if (file.isDirectory) return FileCategory.FOLDER
        val ext = getExtension(file)
        val mime = getMimeType(file).lowercase(Locale.ROOT)

        return when {
            mime.startsWith("image/") || ext in setOf("jpg", "jpeg", "png", "webp", "gif", "bmp", "svg", "heic") -> FileCategory.IMAGE
            mime.startsWith("video/") || ext in setOf("mp4", "mkv", "mov", "avi", "webm", "3gp", "flv") -> FileCategory.VIDEO
            mime.startsWith("audio/") || ext in setOf("mp3", "m4a", "wav", "flac", "ogg", "aac", "opus") -> FileCategory.AUDIO
            ext == "apk" -> FileCategory.APK
            ext in setOf("zip", "rar", "7z", "tar", "gz", "bz2", "xz") -> FileCategory.ARCHIVE
            ext in setOf("kt", "java", "c", "cpp", "h", "py", "sh", "js", "ts", "html", "css", "xml", "json", "gradle", "sql") -> FileCategory.CODE
            mime.startsWith("text/") || ext in setOf("pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt", "md", "csv", "epub") -> FileCategory.DOCUMENT
            else -> FileCategory.OTHER
        }
    }
}
