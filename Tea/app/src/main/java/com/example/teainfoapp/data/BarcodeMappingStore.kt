package com.example.teainfoapp.data

import android.content.Context
import androidx.core.content.edit

/**
 * Stores user-added barcode -> tea type mappings for unknown products.
 */
object BarcodeMappingStore {
    private const val PREFS_NAME = "barcode_mappings"
    private const val KEY_PREFIX = "barcode_"
    private const val SEP = "||"

    data class Mapping(
        val teaType: String,
        val brand: String = "",
        val product: String = ""
    )

    fun saveMapping(
        context: Context,
        barcode: String,
        teaType: String,
        brand: String = "",
        product: String = ""
    ) {
        val safeBarcode = ScanNormalization.normalizeBarcode(barcode)
        if (safeBarcode.isBlank() || teaType.isBlank()) return

        val payload = listOf(teaType.trim(), brand.trim(), product.trim()).joinToString(SEP)
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit {
            putString(KEY_PREFIX + safeBarcode, payload)
        }
    }

    fun getMapping(context: Context, barcode: String): Mapping? {
        val safeBarcode = ScanNormalization.normalizeBarcode(barcode)
        if (safeBarcode.isBlank()) return null

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val payload = prefs.getString(KEY_PREFIX + safeBarcode, null)
            ?: prefs.getString(KEY_PREFIX + barcode.trim(), null)
            ?: return null

        val parts = payload.split(SEP)
        return Mapping(
            teaType = parts.getOrElse(0) { "" },
            brand = parts.getOrElse(1) { "" },
            product = parts.getOrElse(2) { "" }
        ).takeIf { it.teaType.isNotBlank() }
    }

    fun getMappedTeaType(context: Context, barcode: String): String? {
        return getMapping(context, barcode)?.teaType
    }

    fun getAllMappings(context: Context): Map<String, Mapping> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.all
            .filterKeys { it.startsWith(KEY_PREFIX) }
            .mapNotNull { (key, value) ->
                val payload = value as? String ?: return@mapNotNull null
                val barcode = ScanNormalization.normalizeBarcode(key.removePrefix(KEY_PREFIX))
                if (barcode.isBlank()) return@mapNotNull null
                val parts = payload.split(SEP)
                val mapping = Mapping(
                    teaType = parts.getOrElse(0) { "" },
                    brand = parts.getOrElse(1) { "" },
                    product = parts.getOrElse(2) { "" }
                )
                if (mapping.teaType.isBlank()) null else barcode to mapping
            }
            .toMap()
    }

}

