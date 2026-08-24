package com.tuck.app.data.local.storage

import android.content.Context
import com.tuck.app.data.local.db.dao.CollectionDao
import com.tuck.app.data.local.db.dao.SavedItemDao
import com.tuck.app.data.local.db.dao.SavedItemFtsDao
import com.tuck.app.data.local.db.entity.SavedItemEntity
import com.tuck.app.data.local.db.entity.SavedItemFtsEntity
import com.tuck.app.domain.model.ContentType
import com.tuck.app.domain.model.ProcessingStatus
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VaultBackupService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val savedItemDao: SavedItemDao,
    private val savedItemFtsDao: SavedItemFtsDao,
    private val collectionDao: CollectionDao
) {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        isLenient = true
    }

    suspend fun exportVaultJson(): String = withContext(Dispatchers.IO) {
        val items = savedItemDao.getAllActiveItemsList()
        val root = buildJsonObject {
            put("version", 1)
            put("exportedAt", System.currentTimeMillis())
            put("appName", "Tuck")
            put("items", buildJsonArray {
                for (item in items) {
                    add(buildJsonObject {
                        put("id", item.id)
                        put("contentType", item.contentType.name)
                        put("title", item.title)
                        put("description", item.description ?: "")
                        put("originalUrl", item.originalUrl ?: "")
                        put("canonicalUrl", item.canonicalUrl ?: "")
                        put("sourceDomain", item.sourceDomain ?: "")
                        put("sourceApp", item.sourceApp ?: "")
                        put("originalText", item.originalText ?: "")
                        put("extractedText", item.extractedText ?: "")
                        put("ocrText", item.ocrText ?: "")
                        put("userNote", item.userNote ?: "")
                        put("captureNote", item.captureNote ?: "")
                        put("isFavorite", item.isFavorite)
                        put("createdAt", item.createdAt)
                    })
                }
            })
        }

        val backupFile = File(context.cacheDir, "tuck_vault_backup.json")
        backupFile.writeText(json.encodeToString(JsonObject.serializer(), root))
        backupFile.absolutePath
    }

    suspend fun exportFullVaultZip(destZip: File? = null): File = withContext(Dispatchers.IO) {
        val targetZip = destZip ?: File(context.cacheDir, "tuck_vault_backup_${System.currentTimeMillis()}.tuck")
        val items = savedItemDao.getAllActiveItemsList()

        val manifest = buildJsonObject {
            put("version", 2)
            put("exportedAt", System.currentTimeMillis())
            put("appName", "Tuck")
            put("items", buildJsonArray {
                for (item in items) {
                    add(buildJsonObject {
                        put("id", item.id)
                        put("contentType", item.contentType.name)
                        put("title", item.title)
                        put("description", item.description ?: "")
                        put("originalUrl", item.originalUrl ?: "")
                        put("canonicalUrl", item.canonicalUrl ?: "")
                        put("sourceDomain", item.sourceDomain ?: "")
                        put("sourceApp", item.sourceApp ?: "")
                        put("originalText", item.originalText ?: "")
                        put("extractedText", item.extractedText ?: "")
                        put("ocrText", item.ocrText ?: "")
                        put("userNote", item.userNote ?: "")
                        put("captureNote", item.captureNote ?: "")
                        put("isFavorite", item.isFavorite)
                        put("createdAt", item.createdAt)
                        if (!item.localFilePath.isNullOrBlank()) {
                            val file = File(item.localFilePath)
                            if (file.exists()) {
                                put("localMediaName", file.name)
                            }
                        }
                    })
                }
            })
        }

        ZipOutputStream(BufferedOutputStream(FileOutputStream(targetZip))).use { zos ->
            // 1. Write manifest
            val manifestEntry = ZipEntry("manifest.json")
            zos.putNextEntry(manifestEntry)
            zos.write(json.encodeToString(JsonObject.serializer(), manifest).toByteArray())
            zos.closeEntry()

            // 2. Write media files
            val mediaDir = File(context.filesDir, "media")
            if (mediaDir.exists() && mediaDir.isDirectory) {
                mediaDir.listFiles()?.forEach { file ->
                    if (file.isFile) {
                        val entry = ZipEntry("media/${file.name}")
                        zos.putNextEntry(entry)
                        FileInputStream(file).use { fis ->
                            fis.copyTo(zos)
                        }
                        zos.closeEntry()
                    }
                }
            }
        }

        targetZip
    }

    suspend fun restoreFullVaultZip(zipFile: File): Int = withContext(Dispatchers.IO) {
        val tempExtractDir = File(context.cacheDir, "restore_temp_${System.currentTimeMillis()}").apply { mkdirs() }
        var manifestJson: String? = null

        try {
            ZipInputStream(BufferedInputStream(FileInputStream(zipFile))).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    val outFile = File(tempExtractDir, entry.name)
                    if (entry.isDirectory) {
                        outFile.mkdirs()
                    } else {
                        outFile.parentFile?.mkdirs()
                        FileOutputStream(outFile).use { fos ->
                            zis.copyTo(fos)
                        }
                        if (entry.name == "manifest.json") {
                            manifestJson = outFile.readText()
                        }
                    }
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            }

            if (manifestJson == null) {
                return@withContext 0
            }

            // Copy media files into app-private media directory
            val extractedMediaDir = File(tempExtractDir, "media")
            val targetMediaDir = File(context.filesDir, "media").apply { mkdirs() }
            if (extractedMediaDir.exists() && extractedMediaDir.isDirectory) {
                extractedMediaDir.listFiles()?.forEach { mediaFile ->
                    val dest = File(targetMediaDir, mediaFile.name)
                    mediaFile.copyTo(dest, overwrite = true)
                }
            }

            // Restore DB records
            val root = json.parseToJsonElement(manifestJson!!).jsonObject
            val array = root["items"]?.jsonArray ?: JsonArray(emptyList())
            var restoredCount = 0

            for (element in array) {
                val obj = element.jsonObject
                val typeStr = obj["contentType"]?.jsonPrimitive?.content ?: ContentType.TEXT.name
                val type = try { ContentType.valueOf(typeStr) } catch (e: Exception) { ContentType.TEXT }
                val mediaName = obj["localMediaName"]?.jsonPrimitive?.content?.takeIf { it.isNotBlank() }
                val localPath = if (mediaName != null) File(targetMediaDir, mediaName).absolutePath else null

                val entity = SavedItemEntity(
                    contentType = type,
                    title = obj["title"]?.jsonPrimitive?.content ?: "Imported Item",
                    description = obj["description"]?.jsonPrimitive?.content?.takeIf { it.isNotBlank() },
                    originalUrl = obj["originalUrl"]?.jsonPrimitive?.content?.takeIf { it.isNotBlank() },
                    canonicalUrl = obj["canonicalUrl"]?.jsonPrimitive?.content?.takeIf { it.isNotBlank() },
                    sourceDomain = obj["sourceDomain"]?.jsonPrimitive?.content?.takeIf { it.isNotBlank() },
                    sourceApp = obj["sourceApp"]?.jsonPrimitive?.content?.takeIf { it.isNotBlank() },
                    originalText = obj["originalText"]?.jsonPrimitive?.content?.takeIf { it.isNotBlank() },
                    extractedText = obj["extractedText"]?.jsonPrimitive?.content?.takeIf { it.isNotBlank() },
                    ocrText = obj["ocrText"]?.jsonPrimitive?.content?.takeIf { it.isNotBlank() },
                    userNote = obj["userNote"]?.jsonPrimitive?.content?.takeIf { it.isNotBlank() },
                    captureNote = obj["captureNote"]?.jsonPrimitive?.content?.takeIf { it.isNotBlank() },
                    localFilePath = localPath,
                    isFavorite = obj["isFavorite"]?.jsonPrimitive?.booleanOrNull ?: false,
                    processingStatus = ProcessingStatus.READY,
                    createdAt = obj["createdAt"]?.jsonPrimitive?.longOrNull ?: System.currentTimeMillis()
                )

                val newId = savedItemDao.insert(entity)
                if (newId > 0) {
                    val fts = SavedItemFtsEntity(
                        rowid = newId,
                        title = entity.title,
                        description = entity.description.orEmpty(),
                        originalUrl = entity.originalUrl.orEmpty(),
                        sourceDomain = entity.sourceDomain.orEmpty(),
                        originalText = entity.originalText.orEmpty(),
                        extractedText = entity.extractedText.orEmpty(),
                        ocrText = entity.ocrText.orEmpty(),
                        tags = "",
                        entities = ""
                    )
                    savedItemFtsDao.insertOrUpdate(fts)
                    restoredCount++
                }
            }

            restoredCount
        } finally {
            tempExtractDir.deleteRecursively()
        }
    }

    suspend fun exportCollectionZip(collectionId: Long, destZip: File? = null): File = withContext(Dispatchers.IO) {
        val targetZip = destZip ?: File(context.cacheDir, "collection_${collectionId}_backup.tuck")
        val collection = collectionDao.getById(collectionId)
        val items = savedItemDao.getItemsByCollectionList(collectionId)

        val manifest = buildJsonObject {
            put("version", 2)
            put("exportedAt", System.currentTimeMillis())
            put("collectionName", collection?.name ?: "Collection")
            put("items", buildJsonArray {
                for (item in items) {
                    add(buildJsonObject {
                        put("id", item.id)
                        put("contentType", item.contentType.name)
                        put("title", item.title)
                        put("description", item.description ?: "")
                        put("originalUrl", item.originalUrl ?: "")
                        put("canonicalUrl", item.canonicalUrl ?: "")
                        put("sourceDomain", item.sourceDomain ?: "")
                        put("sourceApp", item.sourceApp ?: "")
                        put("originalText", item.originalText ?: "")
                        put("extractedText", item.extractedText ?: "")
                        put("ocrText", item.ocrText ?: "")
                        put("userNote", item.userNote ?: "")
                        put("captureNote", item.captureNote ?: "")
                        put("isFavorite", item.isFavorite)
                        put("createdAt", item.createdAt)
                    })
                }
            })
        }

        ZipOutputStream(BufferedOutputStream(FileOutputStream(targetZip))).use { zos ->
            val manifestEntry = ZipEntry("manifest.json")
            zos.putNextEntry(manifestEntry)
            zos.write(json.encodeToString(JsonObject.serializer(), manifest).toByteArray())
            zos.closeEntry()
        }

        targetZip
    }

    suspend fun restoreVaultJson(jsonString: String): Int = withContext(Dispatchers.IO) {
        val root = json.parseToJsonElement(jsonString).jsonObject
        val array = root["items"]?.jsonArray ?: JsonArray(emptyList())
        var restoredCount = 0

        for (element in array) {
            val obj = element.jsonObject
            val typeStr = obj["contentType"]?.jsonPrimitive?.content ?: ContentType.TEXT.name
            val type = try { ContentType.valueOf(typeStr) } catch (e: Exception) { ContentType.TEXT }

            val entity = SavedItemEntity(
                contentType = type,
                title = obj["title"]?.jsonPrimitive?.content ?: "Imported Item",
                description = obj["description"]?.jsonPrimitive?.content?.takeIf { it.isNotBlank() },
                originalUrl = obj["originalUrl"]?.jsonPrimitive?.content?.takeIf { it.isNotBlank() },
                canonicalUrl = obj["canonicalUrl"]?.jsonPrimitive?.content?.takeIf { it.isNotBlank() },
                sourceDomain = obj["sourceDomain"]?.jsonPrimitive?.content?.takeIf { it.isNotBlank() },
                sourceApp = obj["sourceApp"]?.jsonPrimitive?.content?.takeIf { it.isNotBlank() },
                originalText = obj["originalText"]?.jsonPrimitive?.content?.takeIf { it.isNotBlank() },
                extractedText = obj["extractedText"]?.jsonPrimitive?.content?.takeIf { it.isNotBlank() },
                ocrText = obj["ocrText"]?.jsonPrimitive?.content?.takeIf { it.isNotBlank() },
                userNote = obj["userNote"]?.jsonPrimitive?.content?.takeIf { it.isNotBlank() },
                captureNote = obj["captureNote"]?.jsonPrimitive?.content?.takeIf { it.isNotBlank() },
                isFavorite = obj["isFavorite"]?.jsonPrimitive?.booleanOrNull ?: false,
                processingStatus = ProcessingStatus.READY,
                createdAt = obj["createdAt"]?.jsonPrimitive?.longOrNull ?: System.currentTimeMillis()
            )

            val newId = savedItemDao.insert(entity)
            if (newId > 0) {
                val fts = SavedItemFtsEntity(
                    rowid = newId,
                    title = entity.title,
                    description = entity.description.orEmpty(),
                    originalUrl = entity.originalUrl.orEmpty(),
                    sourceDomain = entity.sourceDomain.orEmpty(),
                    originalText = entity.originalText.orEmpty(),
                    extractedText = entity.extractedText.orEmpty(),
                    ocrText = entity.ocrText.orEmpty(),
                    tags = "",
                    entities = ""
                )
                savedItemFtsDao.insertOrUpdate(fts)
                restoredCount++
            }
        }
        restoredCount
    }
}
