package com.vestis.wardrobe.data.backup

import android.content.Context
import android.net.Uri
import com.vestis.wardrobe.data.model.Outfit
import com.vestis.wardrobe.data.model.WardrobeItem
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.Serializable
import java.io.File
import java.io.FileInputStream
import java.time.LocalDate
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * Top-level manifest included in every backup archive.
 * Schema uses a reverse-DNS style URN so third-party tools can identify the format.
 */
@Serializable
data class BackupManifest(
    val schema: String = "vestis:wardrobe:v1",
    val schemaVersion: String = "1.0",
    val exportDate: String,
    val hemisphere: String,
    val totals: BackupTotals
)

@Serializable
data class BackupTotals(val items: Int, val outfits: Int)

/** Portable item representation (decoupled from Room entity) */
@Serializable
data class BackupItem(
    val id: String,
    val emoji: String,
    val name: String,
    val category: String,
    val color: String,
    val brand: String,
    val notes: String,
    val photoFile: String,   // relative path inside zip e.g. "photos/abc123.jpg"
    val dateAdded: String
)

/** Portable outfit representation */
@Serializable
data class BackupOutfit(
    val id: String,
    val date: String,
    val itemIds: List<String>,
    val notes: String,
    val season: String
)

private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }

object BackupManager {

    // ─── EXPORT ──────────────────────────────────────────────────────────────

    /**
     * Creates a ZIP backup at [destUri] containing:
     *   manifest.json   – schema info
     *   items.json      – wardrobe catalogue
     *   outfits.json    – outfit history
     *   photos/         – item photos (PNG/JPEG) referenced by items.json
     */
    fun export(
        context: Context,
        items: List<WardrobeItem>,
        outfits: List<Outfit>,
        hemisphere: String,
        destUri: Uri
    ) {
        val backupItems = items.map { item ->
            val photoFile = if (item.photoPath.isNotBlank())
                "photos/${item.id}.jpg" else ""
            BackupItem(
                id = item.id, emoji = item.emoji, name = item.name,
                category = item.category, color = item.color,
                brand = item.brand, notes = item.notes,
                photoFile = photoFile, dateAdded = item.dateAdded
            )
        }

        val backupOutfits = outfits.map { o ->
            val ids = json.decodeFromString<List<String>>(o.itemIds)
            BackupOutfit(
                id = o.id, date = o.date, itemIds = ids,
                notes = o.notes, season = o.season
            )
        }

        val manifest = BackupManifest(
            exportDate = LocalDate.now().toString(),
            hemisphere = hemisphere,
            totals = BackupTotals(items.size, outfits.size)
        )

        context.contentResolver.openOutputStream(destUri)?.use { out ->
            ZipOutputStream(out.buffered()).use { zip ->

                // manifest.json
                zip.putNextEntry(ZipEntry("manifest.json"))
                zip.write(json.encodeToString(manifest).toByteArray())
                zip.closeEntry()

                // items.json
                zip.putNextEntry(ZipEntry("items.json"))
                zip.write(json.encodeToString(backupItems).toByteArray())
                zip.closeEntry()

                // outfits.json
                zip.putNextEntry(ZipEntry("outfits.json"))
                zip.write(json.encodeToString(backupOutfits).toByteArray())
                zip.closeEntry()

                // photos/
                items.forEach { item ->
                    if (item.photoPath.isNotBlank()) {
                        val photoFile = File(item.photoPath)
                        if (photoFile.exists()) {
                            zip.putNextEntry(ZipEntry("photos/${item.id}.jpg"))
                            FileInputStream(photoFile).use { it.copyTo(zip) }
                            zip.closeEntry()
                        }
                    }
                }
            }
        }
    }

    // ─── IMPORT ──────────────────────────────────────────────────────────────

    data class ImportResult(
        val manifest: BackupManifest,
        val items: List<WardrobeItem>,
        val outfits: List<Outfit>
    )

    /**
     * Reads a VESTIS backup ZIP and returns parsed data ready to insert into Room.
     * Photos are extracted into [photoDir].
     */
    fun import(context: Context, sourceUri: Uri, photoDir: File): ImportResult {
        photoDir.mkdirs()

        var manifest: BackupManifest? = null
        var backupItems: List<BackupItem> = emptyList()
        var backupOutfits: List<BackupOutfit> = emptyList()

        context.contentResolver.openInputStream(sourceUri)?.use { input ->
            ZipInputStream(input.buffered()).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    when (entry.name) {
                        "manifest.json" ->
                            manifest = json.decodeFromString(zip.readBytes().decodeToString())
                        "items.json" ->
                            backupItems = json.decodeFromString(zip.readBytes().decodeToString())
                        "outfits.json" ->
                            backupOutfits = json.decodeFromString(zip.readBytes().decodeToString())
                        else -> if (entry.name.startsWith("photos/")) {
                            val name = entry.name.removePrefix("photos/")
                            if (name.isNotBlank()) {
                                File(photoDir, name).outputStream().use { zip.copyTo(it) }
                            }
                        }
                    }
                    zip.closeEntry()
                    entry = zip.nextEntry
                }
            }
        }

        requireNotNull(manifest) { "Invalid backup: missing manifest.json" }

        val items = backupItems.map { b ->
            val photoPath = if (b.photoFile.isNotBlank()) {
                File(photoDir, b.photoFile.removePrefix("photos/")).absolutePath
            } else ""
            WardrobeItem(
                id = b.id, emoji = b.emoji, name = b.name, category = b.category,
                color = b.color, brand = b.brand, notes = b.notes,
                photoPath = photoPath, dateAdded = b.dateAdded
            )
        }

        val outfits = backupOutfits.map { b ->
            Outfit(
                id = b.id, date = b.date,
                itemIds = json.encodeToString(b.itemIds),
                notes = b.notes, season = b.season
            )
        }

        return ImportResult(manifest!!, items, outfits)
    }
}
