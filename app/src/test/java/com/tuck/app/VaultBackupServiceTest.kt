package com.tuck.app

import android.content.Context
import com.tuck.app.data.local.db.dao.CollectionDao
import com.tuck.app.data.local.db.dao.SavedItemDao
import com.tuck.app.data.local.db.dao.SavedItemFtsDao
import com.tuck.app.data.local.db.entity.SavedItemEntity
import com.tuck.app.data.local.storage.VaultBackupService
import com.tuck.app.domain.model.ContentType
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class VaultBackupServiceTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private val context = mockk<Context>(relaxed = true)
    private val savedItemDao = mockk<SavedItemDao>(relaxed = true)
    private val savedItemFtsDao = mockk<SavedItemFtsDao>(relaxed = true)
    private val collectionDao = mockk<CollectionDao>(relaxed = true)

    private lateinit var service: VaultBackupService
    private lateinit var cacheDir: File
    private lateinit var filesDir: File

    @Before
    fun setUp() {
        cacheDir = tempFolder.newFolder("cache")
        filesDir = tempFolder.newFolder("files")
        every { context.cacheDir } returns cacheDir
        every { context.filesDir } returns filesDir

        service = VaultBackupService(
            context = context,
            savedItemDao = savedItemDao,
            savedItemFtsDao = savedItemFtsDao,
            collectionDao = collectionDao
        )
    }

    @Test
    fun testExportVaultJsonCreatesValidJsonFile() = runBlocking {
        val item1 = SavedItemEntity(
            id = 1L,
            contentType = ContentType.URL,
            title = "Article on SQLite",
            sourceDomain = "sqlite.org",
            userNote = "Crucial reference"
        )
        val item2 = SavedItemEntity(
            id = 2L,
            contentType = ContentType.TEXT,
            title = "Meeting notes",
            originalText = "Discuss architecture"
        )

        coEvery { savedItemDao.getAllActiveItemsList() } returns listOf(item1, item2)

        val path = service.exportVaultJson()
        assertNotNull(path)

        val exportedFile = File(path)
        assertTrue(exportedFile.exists())

        val root = Json.parseToJsonElement(exportedFile.readText()).jsonObject
        assertEquals(1, root["version"]?.jsonPrimitive?.intOrNull)
        assertEquals("Tuck", root["appName"]?.jsonPrimitive?.content)
        assertEquals(2, root["items"]?.jsonArray?.size)
    }

    @Test
    fun testRestoreVaultJsonInsertsItemsAndUpdatesFts() = runBlocking {
        val json = """
            {
              "version": 1,
              "items": [
                {
                  "id": 1,
                  "contentType": "URL",
                  "title": "Restored Article",
                  "sourceDomain": "android.com"
                }
              ]
            }
        """.trimIndent()

        coEvery { savedItemDao.insert(any()) } returns 101L

        val count = service.restoreVaultJson(json)
        assertEquals(1, count)

        coVerify(exactly = 1) { savedItemDao.insert(any()) }
        coVerify(exactly = 1) { savedItemFtsDao.insertOrUpdate(any()) }
    }

    @Test
    fun testExportAndRestoreFullVaultZip() = runBlocking {
        val item = SavedItemEntity(
            id = 10L,
            contentType = ContentType.TEXT,
            title = "Archived Note",
            userNote = "Important note to keep"
        )
        coEvery { savedItemDao.getAllActiveItemsList() } returns listOf(item)
        coEvery { savedItemDao.insert(any()) } returns 201L

        val zipTarget = File(cacheDir, "test_backup.tuck")
        val exportedZip = service.exportFullVaultZip(zipTarget)
        assertTrue(exportedZip.exists())
        assertTrue(exportedZip.length() > 0)

        val restoredCount = service.restoreFullVaultZip(exportedZip)
        assertEquals(1, restoredCount)
    }
}
