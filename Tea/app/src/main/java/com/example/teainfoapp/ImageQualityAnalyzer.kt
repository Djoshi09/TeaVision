package com.example.teainfoapp

import android.graphics.Bitmap
import android.util.Log
import androidx.core.graphics.scale
import kotlin.math.sqrt

/**
 * Image Quality Analyzer
 * Evaluates image quality for better tea recognition
 * Works with any background and lighting conditions
 */
class ImageQualityAnalyzer(private val context: android.content.Context? = null) {

    companion object {
        private const val TAG = "ImageQualityAnalyzer"

        // Quality thresholds - RELAXED for real-world smartphone cameras
        // Smartphone cameras rarely exceed sharpness of 50-80 even in good conditions
        const val MIN_SHARPNESS = 40f         // Allows normal smartphone photos (was 110)
        const val MIN_CONTRAST = 15f          // More realistic for tea on various backgrounds (was 30)
        const val IDEAL_BRIGHTNESS = 120f    // Adjusted range
        const val MIN_BRIGHTNESS = 40f        // Minimum acceptable brightness (was 50)
        const val MAX_BRIGHTNESS = 210f       // Maximum acceptable brightness (was 200)
    }

    data class QualityMetrics(
        val sharpness: Float,
        val contrast: Float,
        val brightness: Float,
        val noiseLevel: Float,
        val saturation: Float,
        val overallScore: Float,  // 0-100
        val isGoodQuality: Boolean,
        val recommendations: List<String> = emptyList()
    )

    /**
     * Analyze image quality for tea leaf recognition
     */
    fun analyzeImageQuality(bitmap: Bitmap): QualityMetrics {
        try {
            val resized = bitmap.scale(256, 256)
            val pixels = IntArray(resized.width * resized.height)
            resized.getPixels(pixels, 0, resized.width, 0, 0, resized.width, resized.height)

            // Calculate metrics
            val sharpness = calculateSharpness(resized, pixels)
            val contrast = calculateContrast(pixels)
            val brightness = calculateBrightness(pixels)
            val noiseLevel = estimateNoiseLevel(pixels)
            val saturation = calculateSaturation(pixels)

            // Calculate overall quality score
            val overallScore = calculateOverallScore(sharpness, contrast, brightness, noiseLevel, saturation)

            // Generate recommendations
            val recommendations = generateRecommendations(
                sharpness, contrast, brightness, noiseLevel, saturation
            )

            return QualityMetrics(
                sharpness = sharpness,
                contrast = contrast,
                brightness = brightness,
                noiseLevel = noiseLevel,
                saturation = saturation,
                overallScore = overallScore,
                isGoodQuality = overallScore >= 30f,  // Relaxed from 50 to allow real-world photos (smartphone limitations)
                recommendations = recommendations
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error analyzing image quality: ${e.message}", e)
            return QualityMetrics(
                sharpness = 0f,
                contrast = 0f,
                brightness = 0f,
                noiseLevel = 0f,
                saturation = 0f,
                overallScore = 0f,
                isGoodQuality = false,
                recommendations = listOf("Error analyzing image quality")
            )
        }
    }

    /**
     * Calculate image sharpness using Laplacian operator
     * High value = sharp image
     */
    private fun calculateSharpness(bitmap: Bitmap, pixels: IntArray): Float {
        var laplacianSum = 0f
        var count = 0

        val width = bitmap.width
        val height = bitmap.height

        // Apply Laplacian operator
        for (y in 1 until height - 1) {
            for (x in 1 until width - 1) {
                val idx = y * width + x
                val center = getGray(pixels[idx])

                val laplacian =
                    -getGray(pixels[(y - 1) * width + (x - 1)]) -
                    (2 * getGray(pixels[(y - 1) * width + x])) -
                    getGray(pixels[(y - 1) * width + (x + 1)]) -
                    (2 * getGray(pixels[y * width + (x - 1)])) +
                    (12 * center) -
                    (2 * getGray(pixels[y * width + (x + 1)])) -
                    getGray(pixels[(y + 1) * width + (x - 1)]) -
                    (2 * getGray(pixels[(y + 1) * width + x])) -
                    getGray(pixels[(y + 1) * width + (x + 1)])

                laplacianSum += kotlin.math.abs(laplacian).toFloat()
                count++
            }
        }

        return if (count > 0) laplacianSum / count else 0f
    }

    /**
     * Calculate image contrast
     * High value = good contrast between tea and background
     */
    private fun calculateContrast(pixels: IntArray): Float {
        val mean = pixels.map { getGray(it) }.average().toFloat()
        val variance = pixels.map { pixel ->
            val gray = getGray(pixel).toFloat()
            (gray - mean) * (gray - mean)
        }.average()

        return sqrt(variance).toFloat()
    }

    /**
     * Calculate average brightness
     * Closer to 128 is better (mid-range optimal for recognition)
     */
    private fun calculateBrightness(pixels: IntArray): Float {
        return pixels.map { getGray(it) }.average().toFloat()
    }

    /**
     * Estimate noise level
     * Low value = less noise (better quality)
     */
    private fun estimateNoiseLevel(pixels: IntArray): Float {
        val width = 256
        val height = 256
        var localVarianceSum = 0f
        var count = 0

        // Analyze local variance in 8x8 blocks
        for (by in 0 until height step 8) {
            for (bx in 0 until width step 8) {
                val blockPixels = mutableListOf<Int>()

                for (y in by until minOf(by + 8, height)) {
                    for (x in bx until minOf(bx + 8, width)) {
                        blockPixels.add(getGray(pixels[y * width + x]))
                    }
                }

                if (blockPixels.isNotEmpty()) {
                    val mean = blockPixels.average()
                    val variance = blockPixels.map { (it - mean) * (it - mean) }.average()
                    localVarianceSum += variance.toFloat()
                    count++
                }
            }
        }

        return if (count > 0) localVarianceSum / count else 0f
    }

    /**
     * Calculate color saturation
     * High value = rich colors (better for identification)
     */
    private fun calculateSaturation(pixels: IntArray): Float {
        var totalSaturation = 0f

        for (pixel in pixels) {
            val r = ((pixel shr 16) and 0xFF).toFloat() / 255f
            val g = ((pixel shr 8) and 0xFF).toFloat() / 255f
            val b = (pixel and 0xFF).toFloat() / 255f

            val max = maxOf(r, g, b)
            val min = minOf(r, g, b)

            val saturation = if (max > 0) (max - min) / max else 0f
            totalSaturation += saturation
        }

        return (totalSaturation / pixels.size) * 100f
    }

    /**
     * Calculate overall quality score (0-100)
     */
    private fun calculateOverallScore(
        sharpness: Float,
        contrast: Float,
        brightness: Float,
        noiseLevel: Float,
        saturation: Float
    ): Float {
        var score = 0f

        // Sharpness component (0-25 points) - RELAXED for phone cameras
        // Scoring formula: (sharpness / 500f) instead of /1200f gives more credit to smartphone images
        score += minOf(25f, (sharpness / 500f) * 25f)

        // Contrast component (0-25 points) - RELAXED for mixed backgrounds
        // Scoring formula: (contrast / 60f) instead of /120f 
        score += minOf(25f, (contrast / 60f) * 25f)

        // Brightness component (0-20 points) - less penalty for variation
        val brightnessDiff = kotlin.math.abs(brightness - IDEAL_BRIGHTNESS)
        score += maxOf(0f, 20f - (brightnessDiff / 300f) * 20f)

        // Noise component (0-15 points) - more lenient
        score += maxOf(0f, 15f - (noiseLevel / 1200f) * 15f)

        // Saturation component (0-15 points)
        score += minOf(15f, (saturation / 80f) * 15f)

        return minOf(100f, maxOf(0f, score))
    }

    /**
     * Generate actionable recommendations
     */
    private fun generateRecommendations(
        sharpness: Float,
        contrast: Float,
        brightness: Float,
        noiseLevel: Float,
        saturation: Float
    ): List<String> {
        val recommendations = mutableListOf<String>()

        if (sharpness < MIN_SHARPNESS) {
            recommendations.add("📷 Image is blurry - Hold camera steady, clean the lens, and wait for focus lock")
        }

        if (contrast < MIN_CONTRAST) {
            recommendations.add("⚫ Low contrast - Place tea on contrasting background or improve lighting")
        }

        val brightnessDiff = kotlin.math.abs(brightness - IDEAL_BRIGHTNESS)
        if (brightness < MIN_BRIGHTNESS) {
            recommendations.add("💡 Too dark - Move to brighter area or add side lighting")
        } else if (brightness > MAX_BRIGHTNESS) {
            recommendations.add("🌞 Too bright - Reduce direct light and avoid glare and reflections")
        } else if (brightnessDiff > 60) {
            if (brightness < IDEAL_BRIGHTNESS) {
                recommendations.add("💡 Lighting uneven - Add more diffuse light")
            } else {
                recommendations.add("🌞 Too bright - Use softer lighting without direct sun")
            }
        }

        if (noiseLevel > 600) {
            recommendations.add("🔊 Noisy image - Increase lighting to reduce noise")
        }

        if (saturation < 15) {
            recommendations.add("🎨 Colors washed out - Need better lighting and focus on tea")
        }

        if (recommendations.isEmpty()) {
            recommendations.add(context?.getString(R.string.good_image_quality) ?: "✅ Good image quality - Ready for tea analysis")
        }

        return recommendations
    }

    /**
     * Get grayscale value from pixel
     */
    private fun getGray(pixel: Int): Int {
        val r = (pixel shr 16) and 0xFF
        val g = (pixel shr 8) and 0xFF
        val b = pixel and 0xFF
        return (0.299 * r + 0.587 * g + 0.114 * b).toInt()
    }

    /**
     * Min of two values
     */
    private fun minOf(a: Float, b: Float): Float = if (a < b) a else b

    /**
     * Max of two values
     */
    private fun maxOf(a: Float, b: Float): Float = if (a > b) a else b

    /**
     * Min of three values
     */
    private fun minOf(a: Float, b: Float, c: Float): Float = minOf(minOf(a, b), c)

    /**
     * Max of three values
     */
    private fun maxOf(a: Float, b: Float, c: Float): Float = maxOf(maxOf(a, b), c)
}
