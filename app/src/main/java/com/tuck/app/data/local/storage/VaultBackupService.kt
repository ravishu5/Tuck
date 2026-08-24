package com.tuck.app.data.local.storage

import android.content.Context
import com.tuck.app.data.local.db.dao.SavedItemDao
import com.tuck.app.data.local.db.dao.SavedItemFtsDao
import com.tuck.app.data.local.db.entity.SavedItemEntity
import com.tuck.app.data.local.db.entity.SavedItemFtsEntity
import com.tuck.app.domain.model.ContentType
import com.tuck.app.domain.model.ProcessingStatus
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VaultBackupService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val savedItemDao: SavedItemDao,
    private val savedItemFtsDao: SavedItemFtsDao
) {

    suspend fun exportVaultJson(): String = withContext(Dispatchers.IO) {
        val items = savedItemDao.getAllActiveItemsList()
        val root = JSONObject()
        root.put("version", 1)
        root.put("exportedAt", System.currentTimeMillis())
        root.put("appName", "Tuck")

        val jsonArray = JSONArray()
        for (item in items) {
            val obj = JSONObject().apply {
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
                put("isFavorite", item.isFavorite)
                put("createdAt", item.createdAt)
            }
            jsonArray.put(obj)
        }
        root.put("items", jsonArray)

        val backupFile = File(context.cacheDir, "tuck_vault_backup.json")
        backupFile.writeText(root.toString(2))
        backupFile.absolutePath
    }

    suspend fun restoreVaultJson(jsonString: String): Int = withContext(Dispatchers.IO) {
        val root = JSONObject(jsonString)
        val array = root.getJSONArray("items")
        var restoredCount = 0

        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            val typeStr = obj.optString("contentType", ContentType.TEXT.name)
            val type = try { ContentType.valueOf(typeStr) } catch (e: Exception) { ContentType.TEXT }

            val entity = SavedItemEntity(
                contentType = type,
                title = obj.optString("title", "Imported Item"),
                description = obj.optString("description").takeIf { it.isNotBlank() },
                originalUrl = obj.optString("originalUrl").takeIf { it.isNotBlank() },
                canonicalUrl = obj.optString("canonicalUrl").takeIf { it.isNotBlank() },
                sourceDomain = obj.optString("sourceDomain").takeIf { it.isNotBlank() },
                sourceApp = obj.optString("sourceApp").takeIf { it.isNotBlank() },
                originalText = obj.optString("originalText").takeIf { it.isNotBlank() },
                extractedText = obj.optString("extractedText").takeIf { it.isNotBlank() },
                ocrText = obj.optString("ocrText").takeIf { it.isNotBlank() },
                isFavorite = obj.optBoolean("isFavorite", false),
                processingStatus = ProcessingStatus.READY,
                createdAt = obj.optLong("createdAt", System.currentTimeMillis())
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
