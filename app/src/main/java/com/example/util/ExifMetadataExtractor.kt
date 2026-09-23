package com.example.util

import android.content.Context
import android.media.ExifInterface
import android.media.MediaMetadataRetriever
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.security.SecureRandom

data class FileMediaMetadata(
    val dimensions: String? = null,
    val cameraModel: String? = null,
    val aperture: String? = null,
    val iso: String? = null,
    val focalLength: String? = null,
    val exposureTime: String? = null,
    val dateTaken: String? = null,
    val audioTitle: String? = null,
    val audioArtist: String? = null,
    val audioAlbum: String? = null,
    val audioDurationMs: Long? = null,
    val audioBitrateKbps: Int? = null
)

object ExifMetadataExtractor {

    suspend fun extractMetadata(file: File): FileMediaMetadata = withContext(Dispatchers.IO) {
        if (!file.exists() || file.isDirectory) return@withContext FileMediaMetadata()

        val ext = file.extension.lowercase()
        var dimensions: String? = null
        var cameraModel: String? = null
        var aperture: String? = null
        var iso: String? = null
        var focalLength: String? = null
        var exposureTime: String? = null
        var dateTaken: String? = null
        var title: String? = null
        var artist: String? = null
        var album: String? = null
        var durationMs: Long? = null
        var bitrate: Int? = null

        // 1. Image EXIF
        if (ext in setOf("jpg", "jpeg", "png", "webp", "heic", "dng")) {
            try {
                val exif = ExifInterface(file.absolutePath)
                val width = exif.getAttribute(ExifInterface.TAG_IMAGE_WIDTH)
                val height = exif.getAttribute(ExifInterface.TAG_IMAGE_LENGTH)
                if (width != null && height != null) {
                    dimensions = "$width × $height"
                }

                val make = exif.getAttribute(ExifInterface.TAG_MAKE) ?: ""
                val model = exif.getAttribute(ExifInterface.TAG_MODEL) ?: ""
                if (model.isNotEmpty()) {
                    cameraModel = if (model.startsWith(make)) model else "$make $model".trim()
                }

                val fNumber = exif.getAttribute(ExifInterface.TAG_F_NUMBER)
                if (fNumber != null) aperture = "f/$fNumber"

                val isoVal = exif.getAttribute(ExifInterface.TAG_ISO_SPEED_RATINGS)
                if (isoVal != null) iso = "ISO $isoVal"

                val focal = exif.getAttribute(ExifInterface.TAG_FOCAL_LENGTH)
                if (focal != null) focalLength = "${focal.substringBefore('/')} mm"

                val exp = exif.getAttribute(ExifInterface.TAG_EXPOSURE_TIME)
                if (exp != null) exposureTime = "$exp s"

                dateTaken = exif.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL)
                    ?: exif.getAttribute(ExifInterface.TAG_DATETIME)
            } catch (e: Throwable) {
                // Ignore EXIF errors for non-exif images
            }
        }

        // 2. Audio & Video MediaMetadataRetriever
        if (ext in setOf("mp3", "m4a", "flac", "wav", "ogg", "aac", "mp4", "mkv", "mov", "webm")) {
            try {
                val retriever = MediaMetadataRetriever()
                retriever.setDataSource(file.absolutePath)

                title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
                artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
                album = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)

                val durStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                durationMs = durStr?.toLongOrNull()

                val bitStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)
                bitrate = bitStr?.toIntOrNull()?.let { it / 1000 }

                val w = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
                val h = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
                if (w != null && h != null) {
                    dimensions = "$w × $h"
                }

                retriever.release()
            } catch (e: Throwable) {
                // Media metadata could not be read
            }
        }

        FileMediaMetadata(
            dimensions = dimensions,
            cameraModel = cameraModel,
            aperture = aperture,
            iso = iso,
            focalLength = focalLength,
            exposureTime = exposureTime,
            dateTaken = dateTaken,
            audioTitle = title,
            audioArtist = artist,
            audioAlbum = album,
            audioDurationMs = durationMs,
            audioBitrateKbps = bitrate
        )
    }
}
