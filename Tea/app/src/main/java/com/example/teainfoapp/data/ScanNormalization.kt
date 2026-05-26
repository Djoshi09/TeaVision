package com.example.teainfoapp.data

import java.util.Locale
/**
 * Shared normalization helpers for scan results and barcode lookups.
 */
object ScanNormalization {
    fun normalizeBarcode(raw: String?): String {
        if (raw.isNullOrBlank()) return ""
        return raw.filter { it.isDigit() }.ifBlank { raw.trim() }
    }

    fun normalizeTeaName(name: String?): String {
        if (name.isNullOrBlank()) return ""
        return name.lowercase(Locale.US)
            .replace(Regex("[^a-z ]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    fun tokenizeTeaName(name: String?): Set<String> {
        val stopWords = setOf("tea", "teas", "leaf", "leaves", "bag", "bags", "with", "and")
        return normalizeTeaName(name)
            .split(" ")
            .filter { it.isNotBlank() && !stopWords.contains(it) }
            .toSet()
    }
}
