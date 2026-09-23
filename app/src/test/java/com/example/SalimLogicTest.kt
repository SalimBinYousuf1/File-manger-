package com.example

import com.example.data.model.FileCategory
import com.example.data.model.FileItem
import com.example.util.CryptoUtils
import com.example.util.MimeUtils
import com.example.util.ZipUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class SalimLogicTest {

  @get:Rule
  val tempFolder = TemporaryFolder()

  @Test
  fun testMimeCategoryDetection() {
    val imageFile = File("photo.JPG")
    assertEquals(FileCategory.IMAGE, MimeUtils.getCategory(imageFile))

    val videoFile = File("movie.mp4")
    assertEquals(FileCategory.VIDEO, MimeUtils.getCategory(videoFile))

    val audioFile = File("song.mp3")
    assertEquals(FileCategory.AUDIO, MimeUtils.getCategory(audioFile))

    val docFile = File("report.pdf")
    assertEquals(FileCategory.DOCUMENT, MimeUtils.getCategory(docFile))

    val archiveFile = File("archive.zip")
    assertEquals(FileCategory.ARCHIVE, MimeUtils.getCategory(archiveFile))

    val apkFile = File("app.apk")
    assertEquals(FileCategory.APK, MimeUtils.getCategory(apkFile))

    val codeFile = File("script.py")
    assertEquals(FileCategory.CODE, MimeUtils.getCategory(codeFile))
  }

  @Test
  fun testFileItemByteFormatting() {
    assertEquals("0 B", FileItem.formatBytes(0))
    assertEquals("512 B", FileItem.formatBytes(512))
    assertEquals("1.0 KB", FileItem.formatBytes(1024))
    assertEquals("1.5 MB", FileItem.formatBytes((1.5 * 1024 * 1024).toLong()))
    assertEquals("10.0 GB", FileItem.formatBytes(10L * 1024 * 1024 * 1024))
  }

  @Test
  fun testCryptoAesEncryptionDecryption() {
    val sampleFile = tempFolder.newFile("sample.txt")
    val originalText = "Salim Native File Manager AES-256 test string."
    sampleFile.writeText(originalText)

    val encFile = File(tempFolder.root, "sample.enc")
    val decFile = File(tempFolder.root, "sample.dec")

    val salt = "SalimSaltKey2026".toByteArray(Charsets.UTF_8)
    val secretKey = CryptoUtils.deriveKey("123456", salt)

    val iv = CryptoUtils.encryptFile(sampleFile, encFile, secretKey)
    assertTrue(encFile.exists() && encFile.length() > 0)

    CryptoUtils.decryptFile(encFile, decFile, secretKey, iv)
    assertTrue(decFile.exists())
    assertEquals(originalText, decFile.readText())
  }

  @Test
  fun testCryptoHashCalculation() {
    val file = tempFolder.newFile("hash_test.txt")
    file.writeText("hello world\n")
    val sha256 = CryptoUtils.calculateHash(file, "SHA-256")
    assertTrue(sha256.isNotEmpty())
    assertEquals(64, sha256.length)
  }

  @Test
  fun testZipCompressAndExtract() {
    val dirToZip = tempFolder.newFolder("source")
    File(dirToZip, "test1.txt").writeText("Content 1")
    File(dirToZip, "test2.txt").writeText("Content 2")

    val zipFile = File(tempFolder.root, "output.zip")
    val compressSuccess = ZipUtils.zipFiles(listOf(dirToZip), zipFile)
    assertTrue(compressSuccess)
    assertTrue(zipFile.exists() && zipFile.length() > 0)

    val extractDir = tempFolder.newFolder("extracted")
    val extractedCount = ZipUtils.unzipFile(zipFile, extractDir)
    assertTrue(extractedCount >= 2)
  }
}
