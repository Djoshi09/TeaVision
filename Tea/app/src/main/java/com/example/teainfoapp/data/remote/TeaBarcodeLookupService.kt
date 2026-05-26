package com.example.teainfoapp.data.remote

import android.util.Log
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

/**
 * Barcode lookup fallback chain:
 * 1) Open Food Facts
 * 2) UPCitemDB trial API
 */
object TeaBarcodeLookupService {
    private const val TAG = "TeaBarcodeLookup"
    private const val UPC_ITEM_DB_URL = "https://api.upcitemdb.com/prod/trial/lookup?upc="
    private const val OPEN_FOOD_FACTS_URL = "https://world.openfoodfacts.org/api/v0/product/%s.json"

    private val teaKeywords = listOf(
        "tea", "chai", "matcha", "earl grey", "earl gray", "oolong", "sencha",
        "darjeeling", "assam", "rooibos", "hibiscus", "peppermint", "chamomile",
        "jasmine", "green tea", "black tea", "white tea", "herbal", "tisane", "infusion", "tea bag", "teabag"
    )

    // Some products are labeled as "ginger drink"/"herbal drink" instead of "tea".
    // Accept these when a tea-herb cue appears together with a beverage/instant context cue.
    private val herbalCues = listOf(
        "ginger", "turmeric", "peppermint", "chamomile", "hibiscus", "rooibos", "lavender",
        "rosehip", "lemon", "matcha"
    )

    private val beverageContextCues = listOf(
        "drink", "beverage", "mix", "instant", "powder", "infusion", "herbal"
    )

    sealed interface LookupResult {
        data class Found(
            val teaHint: String,
            val brand: String,
            val productName: String,
            val source: String
        ) : LookupResult

        data class NotFound(val reason: String) : LookupResult
        data class Error(val reason: String) : LookupResult
    }

    suspend fun lookupTeaByBarcode(rawBarcode: String): LookupResult = withContext(Dispatchers.IO) {
        val barcode = rawBarcode.filter { it.isDigit() }.ifBlank { rawBarcode.trim() }
        if (barcode.isBlank()) {
            return@withContext LookupResult.NotFound("Invalid barcode")
        }

        lookupFromOpenFoodFacts(barcode)?.let { return@withContext it }
        lookupFromUpcItemDb(barcode)?.let { return@withContext it }

        LookupResult.NotFound("No tea product found in external APIs")
    }

    private fun lookupFromUpcItemDb(barcode: String): LookupResult? {
        return try {
            val response = httpGet(UPC_ITEM_DB_URL + barcode)
            val root = JsonParser.parseString(response).asJsonObject
            val items = root.getAsJsonArray("items") ?: return null

            for (item in items) {
                val obj = item.asJsonObject
                val title = obj.optString("title")
                val brand = obj.optString("brand")
                val category = obj.optString("category")
                val description = obj.optString("description")
                val searchText = listOf(title, category, description, brand).joinToString(" ")

                if (looksLikeTea(searchText)) {
                    return LookupResult.Found(
                        teaHint = listOf(title, category, description).joinToString(" ").trim(),
                        brand = brand,
                        productName = title.ifBlank { "UPC Product" },
                        source = "UPCitemDB"
                    )
                }
            }
            null
        } catch (e: Exception) {
            Log.w(TAG, "UPCitemDB lookup failed for $barcode: ${e.message}")
            null
        }
    }

    private fun lookupFromOpenFoodFacts(barcode: String): LookupResult? {
        return try {
            val response = httpGet(OPEN_FOOD_FACTS_URL.format(barcode))
            val root = JsonParser.parseString(response).asJsonObject
            val status = root.optInt("status")
            if (status != 1) return null

            val product = root.getAsJsonObject("product") ?: return null
            val productName = product.optString("product_name")
            val productNameEn = product.optString("product_name_en")
            val brands = product.optString("brands")
            val categories = product.optString("categories")
            val genericName = product.optString("generic_name")
            val searchText = listOf(productName, productNameEn, brands, categories, genericName).joinToString(" ")

            if (!looksLikeTea(searchText)) return null

            LookupResult.Found(
                teaHint = listOf(productName, productNameEn, categories, genericName).joinToString(" ").trim(),
                brand = brands,
                productName = productName.ifBlank { productNameEn.ifBlank { "OpenFoodFacts Product" } },
                source = "OpenFoodFacts"
            )
        } catch (e: Exception) {
            Log.w(TAG, "OpenFoodFacts lookup failed for $barcode: ${e.message}")
            null
        }
    }

    private fun looksLikeTea(text: String): Boolean {
        val lower = text.lowercase()
        if (teaKeywords.any { lower.contains(it) }) {
            return true
        }

        val hasHerbalCue = herbalCues.any { lower.contains(it) }
        val hasBeverageContextCue = beverageContextCues.any { lower.contains(it) }
        return hasHerbalCue && hasBeverageContextCue
    }

    private fun httpGet(url: String): String {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 7000
            readTimeout = 7000
            setRequestProperty("Accept", "application/json")
            setRequestProperty("User-Agent", "TeaInfoApp/1.0")
        }

        return try {
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val body = BufferedReader(InputStreamReader(stream)).use { it.readText() }
            if (code !in 200..299) {
                throw IllegalStateException("HTTP $code")
            }
            body
        } finally {
            connection.disconnect()
        }
    }

    private fun JsonObject.optString(key: String): String {
        val value = this.get(key) ?: return ""
        return if (value.isJsonNull) "" else value.asString.orEmpty()
    }

    private fun JsonObject.optInt(key: String): Int {
        val value = this.get(key) ?: return 0
        return if (value.isJsonNull) 0 else value.asInt
    }

}
