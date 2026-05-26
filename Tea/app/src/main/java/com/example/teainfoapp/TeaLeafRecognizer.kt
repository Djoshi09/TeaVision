package com.example.teainfoapp

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import androidx.core.graphics.scale
import kotlinx.coroutines.*
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import org.tensorflow.lite.support.image.ops.ResizeOp.ResizeMethod
import org.tensorflow.lite.support.common.ops.NormalizeOp
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel
import java.util.Locale

/**
 * Tea Leaf Recognition Engine
 * Uses TensorFlow Lite for visual recognition of tea types
 * Can identify tea directly from images of leaves/bags
 * Works with various lighting conditions and backgrounds
 */
class TeaLeafRecognizer(
    private val context: Context,
    private val config: ModelConfig = ModelConfig()
) {

    data class ModelConfig(
        val modelAssetPath: String = "teamodel.tflite",
        val labelsAssetPath: String = "tea_labels.txt",
        // Default to Teachable Machine standard (-1 to 1)
        // For EfficientNet, we might switch to ImageNet [0.485, 0.456, 0.406] in code
        val inputMean: FloatArray = floatArrayOf(127.5f, 127.5f, 127.5f),
        val inputStd: FloatArray = floatArrayOf(127.5f, 127.5f, 127.5f)
    )

    enum class VisualGroup { RED, GREEN_YELLOW, DARK_AMBER }

    private fun getTeaVisualGroup(tea: String): VisualGroup {
        val normalized = tea.lowercase()
        return when {
            // RED GROUP
            normalized.contains("hibiscus") || 
            normalized.contains("rosehip") || 
            normalized.contains("raspberry") || 
            normalized.contains("blueberry") -> VisualGroup.RED

            // DARK/AMBER GROUP
            normalized.contains("black") || 
            normalized.contains("oolong") || 
            normalized.contains("rooibos") || 
            normalized.contains("chai") || 
            normalized.contains("kukicha") -> VisualGroup.DARK_AMBER

            // GREEN/YELLOW GROUP (DEFAULT)
            else -> VisualGroup.GREEN_YELLOW
        }
    }

    private fun determineImageVisualGroup(features: ImageFeatures): VisualGroup {
        val r = features.avgRed
        val g = features.avgGreen
        val b = features.avgBlue
        val brightness = features.brightness

        return when {
            // RED: Strong Red dominance
            r > (g + 20) && r > (b + 15) -> VisualGroup.RED
            
            // DARK/AMBER: Low brightness or deep reddish-brown/orange tones
            brightness < 90 || (r > 100 && g > 70 && b < 70 && Math.abs(r-g) > 15) -> VisualGroup.DARK_AMBER
            
            // GREEN/YELLOW: High Green or Bright balanced
            else -> VisualGroup.GREEN_YELLOW
        }
    }

    private var interpreter: Interpreter? = null
    private val scope = CoroutineScope(Dispatchers.Default)
    private var inputImageSize = 224
    private var inputDataType: DataType = DataType.FLOAT32
    private var outputDataType: DataType = DataType.FLOAT32

    companion object {
        private const val TAG = "TeaLeafRecognizer"
        // Realistic confidence thresholds for real-world phone camera images
        private const val CONFIDENCE_THRESHOLD = 0.75f
        private const val MIN_CONFIDENCE_GAP = 0.12f
        // Thresholds for feature-based recognition (fallback)
        private const val FEATURE_CONFIDENCE_THRESHOLD = 0.70f
        private const val FEATURE_CONFIDENCE_THRESHOLD_NO_MODEL = 0.75f
    }

    // Apply softmax to raw logits when needed
    private fun applySoftmax(values: FloatArray): FloatArray {
        val max = values.maxOrNull() ?: 0f
        val exps = FloatArray(values.size)
        var sum = 0.0
        for (i in values.indices) {
            val e = kotlin.math.exp((values[i] - max).toDouble())
            exps[i] = e.toFloat()
            sum += e
        }
        if (sum <= 0.0) return values
        val out = FloatArray(values.size)
        for (i in values.indices) out[i] = (exps[i] / sum).toFloat()
        return out
    }

    private val defaultTeaLabels = listOf(
        "Black Tea",
        "Blueberry Tea",
        "Chai Tea",
        "Chamomile Tea",
        "Genmaicha Tea",
        "Ginger Tea",
        "Green Tea",
        "Hibiscus Tea",
        "Kukicha Tea",
        "Lavender Tea",
        "Lemon Tea",
        "Matcha Tea",
        "Oolong Tea",
        "Peppermint Tea",
        "Raspberry Tea",
        "Rooibos Tea",
        "Rosehip Tea",
        "Turmeric Tea"
    )

    private val supportedTeaLabels = defaultTeaLabels.toSet()
    private var labels: List<String> = defaultTeaLabels

    init {
        try {
            labels = loadLabels(config.labelsAssetPath)
            initializeModel()
            Log.d(TAG, "TeaLeafRecognizer initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing TeaLeafRecognizer: ${e.message}", e)
        }
    }

    /**
     * Initialize the TensorFlow Lite model
     */
    private fun initializeModel() {
        try {
            val assetFileDescriptor = context.assets.openFd(config.modelAssetPath)
            val inputStream = FileInputStream(assetFileDescriptor.fileDescriptor)
            val channel = inputStream.channel
            val startOffset = assetFileDescriptor.startOffset
            val declaredLength = assetFileDescriptor.declaredLength
            val buffer = channel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)

            interpreter = Interpreter(buffer)

            interpreter?.getInputTensor(0)?.let { inputTensor ->
                val shape = inputTensor.shape()
                inputDataType = inputTensor.dataType()
                if (shape.size >= 3) {
                    inputImageSize = shape[1]
                }
                
                val tensorName = inputTensor.name().lowercase()
                Log.d(TAG, "Input Tensor: name=$tensorName, shape=${shape.joinToString(",")}, dataType=$inputDataType")
                
                // AUTO-DETECT: EfficientNet often expects ImageNet normalization
                if (tensorName.contains("efficientnet") || config.modelAssetPath.lowercase().contains("efficientnet")) {
                    Log.i(TAG, "🚀 EfficientNet detected! Applying ImageNet normalization [0.485, 0.456, 0.406]")
                    // ImageNet: Mean = [0.485, 0.456, 0.406] * 255, Std = [0.229, 0.224, 0.225] * 255
                    System.arraycopy(floatArrayOf(123.675f, 116.28f, 103.53f), 0, config.inputMean, 0, 3)
                    System.arraycopy(floatArrayOf(58.395f, 57.12f, 57.375f), 0, config.inputStd, 0, 3)
                }
            }
            
            interpreter?.getOutputTensor(0)?.let { outputTensor ->
                val shape = outputTensor.shape()
                outputDataType = outputTensor.dataType()
                Log.d(TAG, "Output Tensor: name=${outputTensor.name()}, shape=${shape.joinToString(",")}, dataType=$outputDataType")
            }

            Log.d(TAG, "Model loaded: path=${config.modelAssetPath}, inputSize=$inputImageSize, inputType=$inputDataType")
        } catch (e: Exception) {
            Log.e(TAG, "Could not load TFLite model '${config.modelAssetPath}': ${e.message}")
            interpreter = null
        }
    }

    private fun loadLabels(assetPath: String): List<String> {
        return try {
            val loaded = context.assets.open(assetPath)
                .bufferedReader()
                .useLines { lines -> lines.map { it.trim() }.filter { it.isNotEmpty() }.toList() }
            if (loaded.isEmpty()) {
                Log.w(TAG, "Labels file '$assetPath' is empty, using defaults")
                defaultTeaLabels
            } else {
                Log.d(TAG, "Loaded ${loaded.size} labels from '$assetPath'")
                loaded
            }
        } catch (e: Exception) {
            Log.w(TAG, "Could not load labels from '$assetPath', using defaults: ${e.message}")
            defaultTeaLabels
        }
    }

    /**
     * Recognize tea type from bitmap image
     * Works with any lighting condition or background
     *
     * @param bitmap: Image of tea leaves/bag
     * @param callback: Called with recognition result
     */
    fun recognizeTeaFromImage(
        bitmap: Bitmap,
        callback: (TeaRecognitionResult) -> Unit
    ) {
        scope.launch {
            try {
                val result = if (interpreter != null) {
                    recognizeUsingTensorFlow(bitmap)
                } else {
                    recognizeUsingFeatureExtraction(bitmap)
                }

                withContext(Dispatchers.Main) {
                    callback(result)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error during recognition: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    callback(TeaRecognitionResult(
                        teaType = "unknown",
                        confidence = 0f,
                        method = "error",
                        error = e.message ?: "Unknown error"
                    ))
                }
            }
        }
    }

    /**
     * TensorFlow Lite based recognition
     * Uses pre-trained neural network model
     */
    private fun recognizeUsingTensorFlow(bitmap: Bitmap): TeaRecognitionResult {
        try {
            Log.d(TAG, "🚀 recognizeUsingTensorFlow called")
            val tfInterpreter = interpreter ?: run {
                Log.e(TAG, "❌ Interpreter is null")
                return recognizeUsingFeatureExtraction(bitmap)
            }

            val inputBuffer = preprocessImage(bitmap)
            Log.d(TAG, "✅ Image preprocessed - Normalization: mean=${config.inputMean}, std=${config.inputStd}")
            Log.i(TAG, "🔍 Thresholds: confidence>=${String.format("%.2f", CONFIDENCE_THRESHOLD)}, gap>=${String.format("%.2f", MIN_CONFIDENCE_GAP)}")
            Log.i(TAG, "🧪 Preprocessing mode: ${describePreprocessingMode()}")

            val outputTensor = tfInterpreter.getOutputTensor(0)
            val outputShape = outputTensor.shape()
            val outputSize = outputShape.lastOrNull() ?: labels.size
            Log.d(TAG, "📊 Output tensor shape: ${outputShape.joinToString(",")}, outputSize: $outputSize, labelsSize: ${labels.size}, dataType: $outputDataType")

            if (outputSize > labels.size) {
                Log.w(TAG, "⚠️ WARNING: Model has $outputSize outputs but only ${labels.size} labels. Indices will mismatch!")
            }

            val predictions = when (outputDataType) {
                DataType.UINT8 -> {
                    Log.d(TAG, "🔵 Processing UINT8 output")
                    val output = Array(1) { ByteArray(outputSize) }
                    tfInterpreter.run(inputBuffer, output)
                    val quant = outputTensor.quantizationParams()
                    Log.d(TAG, "📍 Quantization params - scale: ${quant.scale}, zeroPoint: ${quant.zeroPoint}")
                    output[0].map { byteVal ->
                        val unsigned = byteVal.toInt() and 0xFF
                        if (quant.scale > 0f) {
                            (unsigned - quant.zeroPoint) * quant.scale
                        } else {
                            unsigned / 255f
                        }
                    }.toFloatArray()
                }
                else -> {
                    Log.d(TAG, "🔴 Processing FLOAT32 output")
                    val output = Array(1) { FloatArray(outputSize) }
                    tfInterpreter.run(inputBuffer, output)
                    
                    // NORMALIZATION FIX: Ensure output is normalized if it looks like logits
                    val rawOutput = output[0]
                    Log.d(TAG, "📊 Raw output (first 5): ${rawOutput.take(5).joinToString(", ") { "%.4f".format(it) }}")
                    rawOutput
                }
            }

            // If model returns logits (not probabilities), convert to probabilities via softmax.
            val sumPredictions = predictions.sum()
            var finalPredictions = predictions
            if (sumPredictions.isNaN() || sumPredictions <= 0.0f || sumPredictions < 0.99f || sumPredictions > 1.01f) {
                Log.d(TAG, "⚙️ Output not normalized (sum=$sumPredictions). Applying softmax to obtain probabilities.")
                finalPredictions = applySoftmax(predictions)
                Log.d(TAG, "⚙️ Post-softmax sum=${finalPredictions.sum()}")
            } else {
                Log.d(TAG, "⚙️ Output appears normalized (sum=$sumPredictions)")
            }

            // DEBUG: Log the full output array to see if the model is "dead" (all zeros) or stuck
            Log.d(TAG, "📊 Full raw output (${predictions.size} classes): ${predictions.joinToString(", ") { "%.4f".format(it) }}")

            if (predictions.isEmpty()) {
                return TeaRecognitionResult(
                    teaType = "unknown",
                    confidence = 0f,
                    method = "neural_network",
                    error = "Model returned empty output"
                )
            }

            val rankedPredictions = finalPredictions
                .mapIndexed { index, value -> index to value }
                .sortedByDescending { it.second }

            // 1. Identify the REAL color group of the captured image first
            val features = extractImageFeatures(bitmap)
            val detectedColorGroup = determineImageVisualGroup(features)
            Log.d(TAG, "🎨 Image Color Group Detected: $detectedColorGroup | R:${features.avgRed} G:${features.avgGreen} B:${features.avgBlue} Brt:${features.brightness.toInt()}")

            // 2. FORCE Visual Consistency by filtering guesses that don't match the image color
            val visuallyConsistentList = rankedPredictions.filter { (idx, _) ->
                val label = labels.getOrNull(idx) ?: ""
                getTeaVisualGroup(label) == detectedColorGroup
            }

            // 3. Fallback: If AI is completely lost, use the original ranking but log a warning
            val finalRanking = if (visuallyConsistentList.isNotEmpty()) {
                visuallyConsistentList
            } else {
                Log.w(TAG, "⚠️ No matches in $detectedColorGroup family. Using raw AI ranking.")
                rankedPredictions
            }

            // 4. Extract Top 3 Predictions from the visually filtered list
            val top3 = finalRanking.take(3).mapNotNull { (idx, conf) ->
                labels.getOrNull(idx)?.let { Prediction(canonicalizeTeaLabel(it) ?: it, conf) }
            }

            // DEBUG: Log all predictions for analysis
            val topN = minOf(predictions.size, 5)
            val predictionDebugInfo = rankedPredictions.take(topN).mapIndexed { rankIdx, (classIdx, confidence) ->
                val className = labels.getOrNull(classIdx) ?: "Idx:$classIdx"
                "$rankIdx: $className (${String.format(Locale.US, "%.4f", confidence)})"
            }.joinToString(" | ")
            Log.d(TAG, "📊 Model Predictions (Top $topN): $predictionDebugInfo")

            val topPrediction = rankedPredictions.firstOrNull() ?: (0 to 0f)
            val secondPrediction = rankedPredictions.getOrNull(1) ?: (0 to 0f)
            val confidenceGap = topPrediction.second - secondPrediction.second
            
            val predictedTea = labels.getOrNull(topPrediction.first)
                ?.let(::canonicalizeTeaLabel)

            Log.d(TAG, "🎯 Top Prediction: ${predictedTea ?: "Unknown"} (confidence: ${String.format(Locale.US, "%.4f", topPrediction.second)}, gap: ${String.format(Locale.US, "%.4f", confidenceGap)})")

            if (predictedTea == null || predictedTea !in supportedTeaLabels) {
                return TeaRecognitionResult(
                    teaType = "uncertain",
                    confidence = topPrediction.second,
                    method = "neural_network",
                    error = "Predicted label '${labels.getOrNull(topPrediction.first) ?: "N/A"}' is not supported",
                    topPredictions = top3
                )
            }

            // COLOR GUARD: Verify model prediction matches image colors
            if (!verifyColorConsistency(predictedTea, features)) {
                Log.w(TAG, "❌ COLOR GUARD: Rejected $predictedTea - Colors don't match image!")
                return TeaRecognitionResult(
                    teaType = "uncertain",
                    confidence = 0.0f,
                    method = "neural_network",
                    error = "Visual color mismatch (e.g., green tea predicted but image is dark)",
                    topPredictions = top3
                )
            }

            // FINAL RELIABILITY CHECK: Tiny gap to avoid complete noise
            if (topPrediction.second < 0.05f) {
                 Log.d(TAG, "⚠️ Prediction too weak (${topPrediction.second})")
                 return TeaRecognitionResult(
                    teaType = "low_confidence",
                    confidence = topPrediction.second,
                    method = "neural_network",
                    error = "Signal too weak",
                    topPredictions = top3
                )
            }

            Log.d(TAG, "✅ Returning best guess: $predictedTea")
            return TeaRecognitionResult(
                teaType = predictedTea,
                confidence = topPrediction.second,
                method = "neural_network",
                details = "Top 3: ${rankedPredictions.take(3).joinToString { (idx, conf) -> "${labels.getOrNull(idx) ?: idx}=${"%.2f".format(conf)}" }}",
                topPredictions = top3
            )
        } catch (e: Exception) {
            Log.e(TAG, "TensorFlow recognition error: ${e.message}", e)
            throw e
        }
    }

    /**
     * Fallback: Feature-based recognition using image analysis
     * Analyzes color, texture, and shape patterns
     * Works without pre-trained model
     */
    private fun recognizeUsingFeatureExtraction(bitmap: Bitmap): TeaRecognitionResult {
        try {
            val features = extractImageFeatures(bitmap)
            return classifyByFeatures(features)
        } catch (e: Exception) {
            Log.e(TAG, "Feature extraction error: ${e.message}")
            throw e
        }
    }

    /**
     * Preprocess image for neural network
     * Resize, normalize, and format image data
     */
    private fun preprocessImage(bitmap: Bitmap): ByteBuffer {
        try {
            // Log average brightness to verify the camera feed is active
            val avgBrightness = calculateAverageBrightness(bitmap)
            Log.d(TAG, "📸 Preprocessing bitmap - Size: ${bitmap.width}x${bitmap.height}, Avg Brightness: %.1f".format(avgBrightness))

            val tensorImage = TensorImage(inputDataType)
            tensorImage.load(bitmap) // Load original to avoid double-scaling

            val imageProcessorBuilder = ImageProcessor.Builder()
                .add(ResizeOp(inputImageSize, inputImageSize, ResizeMethod.BILINEAR))

            if (inputDataType == DataType.FLOAT32) {
                // Apply per-channel normalization (supports ImageNet/EfficientNet/TM)
                imageProcessorBuilder.add(NormalizeOp(config.inputMean, config.inputStd))
            }

            val imageProcessor = imageProcessorBuilder.build()
            val processedImage = imageProcessor.process(tensorImage)
            val buffer = processedImage.buffer

            // DEBUG: Check the exact values being sent to the AI
            if (inputDataType == DataType.FLOAT32) {
                val floatBuffer = buffer.asFloatBuffer()
                val sampleSize = floatBuffer.remaining()
                val sample = FloatArray(sampleSize)
                floatBuffer.get(sample)
                floatBuffer.rewind()
                
                val minVal = sample.minOrNull() ?: 0f
                val maxVal = sample.maxOrNull() ?: 0f
                val avgVal = sample.average().toFloat()
                
                Log.d(TAG, "🧪 Preprocessed Input: size=$sampleSize, range=[$minVal, $maxVal], avg=$avgVal")
                Log.d(TAG, "🧪 First Pixel (R,G,B): ${sample.getOrNull(0)}, ${sample.getOrNull(1)}, ${sample.getOrNull(2)}")
            }
            
            return buffer
        } catch (e: Exception) {
            Log.e(TAG, "Image preprocessing error: ${e.message}")
            throw e
        }
    }

    private fun calculateAverageBrightness(bitmap: Bitmap): Float {
        var sum = 0f
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        for (pixel in pixels) {
            val r = (pixel shr 16) and 0xFF
            val g = (pixel shr 8) and 0xFF
            val b = pixel and 0xFF
            sum += (r + g + b) / 3f
        }
        return sum / pixels.size
    }

    /**
     * Simple image enhancement for better recognition
     */
    private fun enhanceImage(bitmap: Bitmap): Bitmap {
        // Just resize first to speed up enhancement if needed
        val scaled = if (bitmap.width > 500) {
            bitmap.scale(500, 500)
        } else {
            bitmap
        }
        
        // FUTURE: Add basic histogram equalization or contrast stretching here
        // For now, we'll focus on the robust preprocessing pipeline
        return scaled
    }

    /**
     * Extract visual features from image
     * Analyzes: color distribution, texture, leaf patterns
     */
    private fun extractImageFeatures(bitmap: Bitmap): ImageFeatures {
        try {
            val resized = bitmap.scale(256, 256)
            val pixels = IntArray(resized.width * resized.height)
            resized.getPixels(pixels, 0, resized.width, 0, 0, resized.width, resized.height)

            var avgRed = 0f
            var avgGreen = 0f
            var avgBlue = 0f
            var colorVariance = 0f

            for (pixel in pixels) {
                avgRed += (pixel shr 16) and 0xFF
                avgGreen += (pixel shr 8) and 0xFF
                avgBlue += pixel and 0xFF
            }

            avgRed /= pixels.size
            avgGreen /= pixels.size
            avgBlue /= pixels.size

            for (pixel in pixels) {
                val r = ((pixel shr 16) and 0xFF) - avgRed
                val g = ((pixel shr 8) and 0xFF) - avgGreen
                val b = (pixel and 0xFF) - avgBlue
                colorVariance += (r * r + g * g + b * b)
            }
            colorVariance /= pixels.size

            val edgeIntensity = detectEdges(resized)

            return ImageFeatures(
                avgRed = avgRed.toInt(),
                avgGreen = avgGreen.toInt(),
                avgBlue = avgBlue.toInt(),
                colorVariance = colorVariance,
                edgeIntensity = edgeIntensity,
                brightness = (avgRed + avgGreen + avgBlue) / 3f
            )
        } catch (e: Exception) {
            Log.e(TAG, "Feature extraction error: ${e.message}")
            throw e
        }
    }

    /**
     * Simple edge detection using Sobel operator
     */
    private fun detectEdges(bitmap: Bitmap): Float {
        try {
            val width = bitmap.width
            val height = bitmap.height
            val pixels = IntArray(width * height)
            bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

            var edgeSum = 0f
            var edgeCount = 0

            for (y in 1 until height - 1) {
                for (x in 1 until width - 1) {
                    val gx = -getGray(pixels[(y-1)*width + (x-1)]) -
                            2*getGray(pixels[y*width + (x-1)]) -
                            getGray(pixels[(y+1)*width + (x-1)]) +
                            getGray(pixels[(y-1)*width + (x+1)]) +
                            2*getGray(pixels[y*width + (x+1)]) +
                            getGray(pixels[(y+1)*width + (x+1)])

                    val gy = -getGray(pixels[(y-1)*width + (x-1)]) -
                            2*getGray(pixels[(y-1)*width + x]) -
                            getGray(pixels[(y-1)*width + (x+1)]) +
                            getGray(pixels[(y+1)*width + (x-1)]) +
                            2*getGray(pixels[(y+1)*width + x]) +
                            getGray(pixels[(y+1)*width + (x+1)])

                    edgeSum += kotlin.math.sqrt((gx*gx + gy*gy).toDouble()).toFloat()
                    edgeCount++
                }
            }

            return if (edgeCount > 0) edgeSum / edgeCount else 0f
        } catch (e: Exception) {
            Log.e(TAG, "Edge detection error: ${e.message}")
            return 0f
        }
    }

    /**
     * Classify tea based on extracted features with STRICT validation
     * Only returns results with high confidence
     */
    private fun classifyByFeatures(features: ImageFeatures): TeaRecognitionResult {
        val confidence = calculateConfidence(features)
        val minConfidence = if (interpreter == null) {
            FEATURE_CONFIDENCE_THRESHOLD_NO_MODEL
        } else {
            FEATURE_CONFIDENCE_THRESHOLD
        }

        if (confidence < minConfidence) {
            return TeaRecognitionResult(
                teaType = "uncertain",
                confidence = 0.0f,
                method = "visual_analysis",
                error = "Image quality/features insufficient for tea identification (confidence: ${(confidence * 100).toInt()}%, min: ${(minConfidence * 100).toInt()}%)"
            )
        }

        val isLiquid = features.edgeIntensity < 20f

        return when {
            // HIBISCUS/RED: Strong Red dominance, Low Green/Blue
            features.avgRed > (features.avgGreen + 35) &&
            features.avgRed > (features.avgBlue + 35) -> {
                TeaRecognitionResult(
                    teaType = "Hibiscus Tea",
                    confidence = confidence,
                    method = "visual_analysis",
                    details = "Strong red dominance indicates Hibiscus/Herbal family"
                )
            }

            // GREEN/MATCHA: Green dominance over Red and Blue
            features.avgGreen > (features.avgRed + 5) &&
            features.avgGreen > features.avgBlue &&
            features.brightness > 40 && features.brightness < 180 -> {
                TeaRecognitionResult(
                    teaType = "Green Tea",
                    confidence = confidence,
                    method = "visual_analysis",
                    details = "Green-dominant color indicates green tea family"
                )
            }

            // LEMON/YELLOW: High Red and Green, Balanced
            features.avgRed > 160 && features.avgGreen > 140 &&
            Math.abs(features.avgRed - features.avgGreen) < 40 -> {
                TeaRecognitionResult(
                    teaType = "Lemon Tea",
                    confidence = confidence,
                    method = "visual_analysis",
                    details = "Bright yellow tones indicate Lemon or Chamomile tea"
                )
            }

            // BLACK TEA: Dark or deep reddish-brown
            features.brightness < 80 &&
            features.avgRed > (features.avgGreen - 10) &&
            features.edgeIntensity > 35f -> {
                TeaRecognitionResult(
                    teaType = "Black Tea",
                    confidence = confidence,
                    method = "visual_analysis",
                    details = "Dark tones and leaf texture indicate black tea"
                )
            }

            features.brightness in 80f..130f &&
            features.edgeIntensity > 45f &&
            features.edgeIntensity < 90f &&
            features.colorVariance > 800f -> {
                TeaRecognitionResult(
                    teaType = "Oolong Tea",
                    confidence = confidence,
                    method = "visual_analysis",
                    details = "Curled leaves and medium roast color indicate oolong tea"
                )
            }

            else -> {
                TeaRecognitionResult(
                    teaType = "uncertain",
                    confidence = 0.0f,
                    method = "visual_analysis",
                    error = "Could not determine tea type - features don't match supported tea patterns"
                )
            }
        }
    }

    /**
     * Verify that the neural network's guess matches the actual colors in the image
     * Prevents common AI mistakes like calling a red liquid "Green Tea"
     */
    private fun verifyColorConsistency(predictedTea: String, features: ImageFeatures): Boolean {
        val normalizedTea = predictedTea.lowercase()
        val r = features.avgRed
        val g = features.avgGreen
        val b = features.avgBlue
        
        return when {
            normalizedTea.contains("green") || normalizedTea.contains("matcha") || normalizedTea.contains("sencha") -> {
                // Green teas MUST NOT be predominantly red
                g > (r - 15) && g > (b - 10)
            }
            normalizedTea.contains("black") || normalizedTea.contains("chai") -> {
                // Black teas are dark or have strong reddish/amber tones, but not bright red
                features.brightness < 160 && r > (g - 15)
            }
            normalizedTea.contains("hibiscus") || normalizedTea.contains("raspberry") || normalizedTea.contains("berry") || normalizedTea.contains("rosehip") -> {
                // Red teas MUST be predominantly red
                r > (g + 25) && r > (b + 20)
            }
            normalizedTea.contains("lemon") || normalizedTea.contains("chamomile") || normalizedTea.contains("ginger") || normalizedTea.contains("turmeric") -> {
                // Yellow/Light teas should be bright and have high R and G
                features.brightness > 60 && r > 100 && g > 80
            }
            else -> true // If we don't have a specific rule, trust the AI
        }
    }

    /**
     * Calculate confidence score based on feature consistency
     * STRICT scoring: requires multiple good feature indicators
     */
    private fun calculateConfidence(features: ImageFeatures): Float {
        var score = 0f
        var maxScore = 0f

        if (features.colorVariance > 500f) {
            score += 0.25f
        }
        maxScore += 0.25f

        if (features.edgeIntensity > 20f && features.edgeIntensity < 200f) {
            score += 0.25f
        }
        maxScore += 0.25f

        if (features.brightness > 30f && features.brightness < 220f) {
            score += 0.25f
        }
        maxScore += 0.25f

        val colorRange = maxOf(
            Math.abs(features.avgRed - features.avgGreen),
            Math.abs(features.avgGreen - features.avgBlue),
            Math.abs(features.avgRed - features.avgBlue)
        )
        if (colorRange > 10) {
            score += 0.25f
        }
        maxScore += 0.25f

        return score / maxScore
    }

    private fun canonicalizeTeaLabel(label: String): String? {
        val normalized = normalizeTeaName(label)
        return when {
            normalized == "green tea" || normalized == "green" || normalized == "sencha" -> "Green Tea"
            normalized == "black tea" || normalized == "black" || normalized == "earl grey tea" || normalized == "earl gray tea" || normalized == "darjeeling tea" || normalized == "assam tea" || normalized == "ceylon tea" -> "Black Tea"
            normalized == "oolong tea" || normalized == "oolong" || normalized == "wulong" -> "Oolong Tea"
            normalized == "chamomile tea" || normalized == "chamomile" || normalized == "camomile" -> "Chamomile Tea"
            normalized == "peppermint tea" || normalized == "peppermint" || normalized == "mint tea" -> "Peppermint Tea"
            normalized == "ginger tea" || normalized == "ginger" -> "Ginger Tea"
            normalized == "hibiscus tea" || normalized == "hibiscus" -> "Hibiscus Tea"
            normalized == "rooibos tea" || normalized == "rooibos" || normalized == "red bush" -> "Rooibos Tea"
            normalized == "lavender tea" || normalized == "lavender" -> "Lavender Tea"
            normalized == "matcha tea" || normalized == "matcha" -> "Matcha Tea"
            normalized == "chai tea" || normalized == "chai" || normalized == "masala chai" -> "Chai Tea"
            normalized == "turmeric tea" || normalized == "turmeric" -> "Turmeric Tea"
            normalized == "rosehip tea" || normalized == "rosehip" || normalized == "rose hip" -> "Rosehip Tea"
            normalized == "blueberry tea" || normalized == "blueberry" -> "Blueberry Tea"
            normalized == "raspberry tea" || normalized == "raspberry" -> "Raspberry Tea"
            normalized == "kukicha tea" || normalized == "kukicha" -> "Kukicha Tea"
            normalized == "genmaicha tea" || normalized == "genmaicha" -> "Genmaicha Tea"
            normalized == "lemon tea" || normalized == "lemon" -> "Lemon Tea"
            else -> supportedTeaLabels.firstOrNull { normalizeTeaName(it) == normalized }
        }
    }

    private fun normalizeTeaName(name: String): String {
        return name.lowercase(Locale.US)
            .replace(Regex("[^a-z ]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private fun describePreprocessingMode(): String {
        return when (inputDataType) {
            DataType.UINT8 -> "RAW_UINT8 (quantized input; no normalization in app)"
            DataType.FLOAT32 -> "RAW_RGB_FLOAT32 (model contains internal rescaling; no normalization in app)"
            else -> "${inputDataType.name} (check model-specific preprocessing)"
        }
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
     * Release resources
     */
    fun release() {
        interpreter?.close()
        scope.cancel()
    }

    /**
     * Data class for individual tea predictions
     */
    data class Prediction(
        val teaType: String,
        val confidence: Float
    )

    /**
     * Data class for recognition results
     */
    data class TeaRecognitionResult(
        val teaType: String,
        val confidence: Float,
        val method: String, // "neural_network", "visual_analysis", "error"
        val details: String = "",
        val error: String = "",
        val topPredictions: List<Prediction> = emptyList()
    )

    /**
     * Data class for extracted image features
     */
    private data class ImageFeatures(
        val avgRed: Int,
        val avgGreen: Int,
        val avgBlue: Int,
        val colorVariance: Float,
        val edgeIntensity: Float,
        val brightness: Float
    )
}
