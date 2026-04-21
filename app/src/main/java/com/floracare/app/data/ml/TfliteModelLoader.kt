package com.floracare.app.data.ml

import android.content.Context
import android.util.Log
import java.io.FileInputStream
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

/**
 * Locates a TFLite asset and (when present) memory-maps it into a
 * [MappedByteBuffer] suitable for feeding into `Interpreter`. Returns
 * [isLoaded] = false when the model file is absent so callers can
 * transparently fall back to a mock prediction path.
 */
class TfliteModelLoader(
    private val context: Context,
    private val modelAssetName: String,
    private val labelsAssetName: String,
) {
    val isLoaded: Boolean
    val labels: List<String>

    init {
        val assets = context.assets.list("ml")?.toSet().orEmpty()
        isLoaded = modelAssetName in assets
        labels = if (labelsAssetName in assets) {
            runCatching {
                context.assets.open("ml/$labelsAssetName").bufferedReader().useLines { seq ->
                    parseLabels(seq.toList())
                }
            }.getOrDefault(emptyList())
        } else {
            emptyList()
        }
        if (!isLoaded) {
            Log.i(TAG, "TFLite model $modelAssetName not in assets/ml — using mock predictions.")
        }
    }

    /**
     * Memory-maps the model asset into a direct byte buffer. Returns `null`
     * when the model is missing or cannot be opened (e.g. compressed).
     * Callers should keep the returned buffer alive for the interpreter's
     * lifetime — closing the interpreter does not close this buffer.
     */
    fun loadModelBuffer(): MappedByteBuffer? {
        if (!isLoaded) return null
        return runCatching {
            val afd = context.assets.openFd("ml/$modelAssetName")
            FileInputStream(afd.fileDescriptor).channel.use { channel ->
                channel.map(
                    FileChannel.MapMode.READ_ONLY,
                    afd.startOffset,
                    afd.declaredLength,
                )
            }
        }.onFailure { Log.e(TAG, "Failed to mmap $modelAssetName", it) }
            .getOrNull()
    }

    companion object {
        private const val TAG = "TfliteModelLoader"
    }
}

/**
 * Pure label-file parser. Tolerates:
 *  - One label per line (plain text).
 *  - CSV / TSV with an optional header (first row skipped if it looks like
 *    column names, e.g. contains `id` / `name` / `label` / `class`).
 *  - Multiple columns — the *last non-numeric* column is taken as the
 *    human-facing label (AIY Vision label maps, for example, use
 *    `id,name` with the scientific name in the second column).
 *
 * Exposed as a top-level function so it can be exercised by plain JVM tests.
 */
internal fun parseLabels(rawLines: List<String>): List<String> {
    val cleaned = rawLines
        .map { it.trim() }
        .filter { it.isNotEmpty() && !it.startsWith("#") }
    if (cleaned.isEmpty()) return emptyList()

    val separator = detectSeparator(cleaned.first())
    val rows: List<List<String>> = cleaned.map { line ->
        if (separator == null) listOf(line) else line.split(separator).map { it.trim() }
    }

    val dataRows = if (looksLikeHeader(rows.first())) rows.drop(1) else rows

    val indexed = dataRows.mapIndexedNotNull { fallback, row ->
        val label = pickLabelColumn(row).takeIf { it.isNotBlank() } ?: return@mapIndexedNotNull null
        val id = detectRowIndex(row) ?: fallback
        id to label
    }
    if (indexed.isEmpty()) return emptyList()

    // Produce a list where position[id] = label so the model's output index
    // maps to the correct name even when the source file lists labels in a
    // non-sequential order (AIY label maps do this — background at 2101 is
    // listed first).
    val maxIndex = indexed.maxOf { it.first }
    val byIndex = indexed.toMap()
    return (0..maxIndex).map { idx -> byIndex[idx].orEmpty() }
}

/**
 * Returns the numeric row index if the first column is a plain integer.
 * For AIY-style `id,name` files this is the class index; fallback lets plain
 * one-per-line lists keep their natural ordering.
 */
private fun detectRowIndex(row: List<String>): Int? =
    row.firstOrNull()?.toIntOrNull()

private fun detectSeparator(firstLine: String): Char? = when {
    firstLine.contains(',') -> ','
    firstLine.contains('\t') -> '\t'
    else -> null
}

private fun looksLikeHeader(firstRow: List<String>): Boolean {
    val joined = firstRow.joinToString(",").lowercase()
    return listOf("id", "name", "label", "class", "index").any { joined.contains(it) } &&
        firstRow.none { it.toIntOrNull() != null }
}

private fun pickLabelColumn(row: List<String>): String {
    // Drop purely-numeric cells (usually an index) and `/m/...` freebase ids.
    val readable = row.filter { cell ->
        cell.isNotBlank() &&
            cell.toIntOrNull() == null &&
            !cell.startsWith("/m/")
    }
    return readable.lastOrNull() ?: row.lastOrNull().orEmpty()
}
