package com.example

import com.example.data.model.FileCategory
import com.example.data.model.FileItem
import com.example.util.ArchiveExplorer
import com.example.util.CryptoUtils
import com.example.util.DiffType
import com.example.util.FileShredder
import com.example.util.HexInspector
import com.example.util.MimeUtils
import com.example.util.TextDiffTool
import com.example.util.ZipUtils
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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

  @Test
  fun testFileShredder() = runBlocking {
    val secretFile = tempFolder.newFile("confidential.txt")
    secretFile.writeText("Top secret government record to be shredded.")
    assertTrue(secretFile.exists())

    val success = FileShredder.shred(secretFile)
    assertTrue(success)
    assertFalse(secretFile.exists())
  }

  @Test
  fun testArchiveExplorer() = runBlocking {
    val dirToZip = tempFolder.newFolder("archive_src")
    File(dirToZip, "itemA.txt").writeText("Alpha")
    File(dirToZip, "itemB.txt").writeText("Beta")

    val zipFile = File(tempFolder.root, "inspect.zip")
    ZipUtils.zipFiles(listOf(dirToZip), zipFile)

    val entries = ArchiveExplorer.listEntries(zipFile)
    assertTrue(entries.isNotEmpty())
    assertTrue(entries.any { it.name.contains("itemA.txt") })
  }

  @Test
  fun testHexInspector() = runBlocking {
    val hexFile = tempFolder.newFile("binary_demo.bin")
    hexFile.writeBytes(byteArrayOf(0x48, 0x65, 0x6C, 0x6C, 0x6F)) // "Hello"

    val rows = HexInspector.readHexPage(hexFile, startOffset = 0L, length = 16)
    assertEquals(1, rows.size)
    assertEquals("00000000", rows[0].offsetHex)
    assertTrue(rows[0].hexValues.startsWith("48 65 6C 6C 6F") || rows[0].hexValues.startsWith("48 65 6c 6c 6f"))
    assertTrue(rows[0].asciiString.startsWith("Hello"))
  }

  @Test
  fun testTextDiffTool() = runBlocking {
    val file1 = tempFolder.newFile("version1.txt")
    val file2 = tempFolder.newFile("version2.txt")
    file1.writeText("Line 1\nLine 2\nLine 3\n")
    file2.writeText("Line 1\nLine 2 Modified\nLine 3\nLine 4 Added\n")

    val diff = TextDiffTool.computeDiff(file1, file2)
    assertTrue(diff.any { it.type == DiffType.ADDED && it.text.contains("Line 4 Added") })
    assertTrue(diff.any { it.type == DiffType.SAME && it.text == "Line 1" })
  }
}
