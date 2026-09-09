package com.tuck.app

import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.tuck.app.data.local.storage.FileStorageService
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.security.MessageDigest

/**
 * Proves Tuck never degrades what it stores.
 *
 * A competitor in this space silently stripped audio from saved videos and recompressed
 * photos from megabytes to kilobytes - after telling users it was safe to delete the
 * originals. These tests exist so that class of bug cannot ship here.
 */
@RunWith(AndroidJUnit4::class)
class FileFidelityTest {

    private lateinit var storage: FileStorageService
    private lateinit var scratch: File

    private fun sha256(file: File): String =
        MessageDigest.getInstance("SHA-256").digest(file.readBytes())
            .joinToString("") { "%02x".format(it) }

    /** Writes a file whose bytes are deliberately incompressible, so re-encoding would show. */
    private fun sourceFile(name: String, sizeBytes: Int, header: ByteArray): File {
        val file = File(scratch, name)
        val random = java.util.Random(42)
        val body = ByteArray(sizeBytes).also { random.nextBytes(it) }
        file.writeBytes(header + body)
        return file
    }

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        storage = FileStorageService(context)
        scratch = File(context.cacheDir, "fidelity-${System.nanoTime()}").apply { mkdirs() }
    }

    @Test
    fun imageBytesAreCopiedVerbatim() = runBlocking {
        // PNG magic so the mime/extension path treats it as a real PNG.
        val source = sourceFile(
            "shot.png", 200_000,
            byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)
        )

        val result = storage.saveImageFromUri(Uri.fromFile(source))
        val saved = File(result.localFilePath)

        assertEquals("image must not be re-encoded", sha256(source), sha256(saved))
        assertEquals("byte length must match exactly", source.length(), saved.length())
        assertEquals("reported hash must match the file on disk", sha256(source), result.sha256)
    }

    @Test
    fun videoBytesAndThereforeAudioAreCopiedVerbatim() = runBlocking {
        val source = sourceFile("clip.mp4", 500_000, ByteArray(0))

        val result = storage.saveStreamUri(Uri.fromFile(source), "video/mp4")
        val saved = File(result.localFilePath)

        // Byte equality is a stronger guarantee than checking for an audio track:
        // if every byte survives, every track survives.
        assertEquals("video must not be transcoded", sha256(source), sha256(saved))
        assertEquals(source.length(), saved.length())
    }

    @Test
    fun pdfBytesAreCopiedVerbatim() = runBlocking {
        val source = sourceFile("doc.pdf", 120_000, "%PDF-1.7\n".toByteArray())

        val result = storage.saveStreamUri(Uri.fromFile(source), "application/pdf")
        val saved = File(result.localFilePath)

        assertEquals("pdf must not be rewritten", sha256(source), sha256(saved))
    }

    @Test
    fun originalExtensionIsPreservedRatherThanForcedToJpg() = runBlocking {
        val png = storage.saveStreamUri(
            Uri.fromFile(sourceFile("a.png", 1000, byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47))),
            "image/png"
        )
        val gif = storage.saveStreamUri(Uri.fromFile(sourceFile("b.gif", 1000, "GIF89a".toByteArray())), "image/gif")
        val mov = storage.saveStreamUri(Uri.fromFile(sourceFile("c.mov", 1000, ByteArray(0))), "video/quicktime")

        assertTrue("a PNG must not be stored as .jpg", png.localFilePath.endsWith(".png"))
        assertTrue("a GIF must not be stored as .jpg", gif.localFilePath.endsWith(".gif"))
        assertTrue("a MOV must not be stored as .mp4", mov.localFilePath.endsWith(".mov"))
    }

    @Test
    fun mimeTypeAndSizeAreReportedNotGuessed() = runBlocking {
        val source = sourceFile("clip.mp4", 64_000, ByteArray(0))

        val result = storage.saveStreamUri(Uri.fromFile(source), "video/mp4")

        assertEquals("video/mp4", result.mimeType)
        assertEquals(source.length(), result.sizeBytes)
        assertNotEquals("size must be measured, not left at zero", 0L, result.sizeBytes)
    }

    @Test
    fun thumbnailIsAnAdditionalFileAndNeverReplacesTheOriginal() = runBlocking {
        val source = sourceFile(
            "shot.png", 300_000,
            byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)
        )

        val result = storage.saveImageFromUri(Uri.fromFile(source))

        assertEquals("the stored original is untouched", sha256(source), sha256(File(result.localFilePath)))
        assertNotEquals(
            "thumbnail must be a separate file, never the original",
            result.localFilePath,
            result.thumbnailPath
        )
    }
}
