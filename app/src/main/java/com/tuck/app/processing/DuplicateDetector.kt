package com.tuck.app.processing

import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DuplicateDetector @Inject constructor(
    private val urlMetadataProcessor: UrlMetadataProcessor
) {
    fun getCanonicalUrl(rawUrl: String): String {
        return urlMetadataProcessor.cleanUrl(rawUrl).trim().lowercase()
    }

    fun hashText(text: String): String {
        val normalized = text.trim().replace("\\s+".toRegex(), " ").lowercase()
        val md = MessageDigest.getInstance("MD5")
        val digest = md.digest(normalized.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }

    fun computeSha256(file: File): String {
        if (!file.exists() || file.length() == 0L) return ""
        val md = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(8192)
        FileInputStream(file).use { input ->
            var read: Int
            while (input.read(buffer).also { read = it } != -1) {
                md.update(buffer, 0, read)
            }
        }
        return md.digest().joinToString("") { "%02x".format(it) }
    }
}
