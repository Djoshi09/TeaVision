package com.example.teainfoapp

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.YuvImage
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import com.example.teainfoapp.data.BarcodeMappingStore
import com.example.teainfoapp.data.ScanNormalization
import com.example.teainfoapp.databinding.ActivityCameraScanBinding
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.io.ByteArrayOutputStream
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Camera Scanning Activity - ENHANCED
 * Uses CameraX and ML Kit for multi-modal tea recognition:
 * 1. Barcode scanning (fastest)
 * 2. Text recognition (packaging labels)
 * 3. Advanced image recognition (tea leaf/bag visual analysis)
 */
class CameraScanActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "CameraScanActivity"

        // Realistic confidence threshold for real-world phone camera images
        private const val IMAGE_CONFIDENCE_MIN = 0.80f

        const val EXTRA_SCAN_MODE = "scan_mode"
        const val SCAN_MODE_IMAGE = "image"
        const val SCAN_MODE_BARCODE = "barcode"
        const val SCAN_MODE_TEXT = "text"
    }

    private lateinit var binding: ActivityCameraScanBinding
    private var cameraExecutor: ExecutorService? = null
    private var imageAnalyzer: ImageAnalysis? = null
    private var camera: Camera? = null
    private var cameraProvider: ProcessCameraProvider? = null

    // ML Kit components
    private val textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    private val barcodeScanner = BarcodeScanning.getClient(
        BarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS)
            .build()
    )

    // Advanced recognition components
    private lateinit var teaLeafRecognizer: TeaLeafRecognizer
    private lateinit var qualityAnalyzer: ImageQualityAnalyzer

    private val supportedTeaTypes = setOf(
        "green tea",
        "black tea",
        "oolong tea",
        "chamomile tea",
        "peppermint tea",
        "ginger tea",
        "hibiscus tea",
        "rooibos tea",
        "lavender tea",
        "matcha tea",
        "chai tea",
        "turmeric tea",
        "rosehip tea",
        "blueberry tea",
        "raspberry tea",
        "kukicha tea",
        "genmaicha tea",
        "lemon tea"
    )

    enum class ScanMode {
        TEXT_ONLY, BARCODE_ONLY, IMAGE_ONLY, ALL
    }

    private var scanMode = ScanMode.ALL

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Log.d(TAG, "Camera permission granted")
            startCamera()
        } else {
            Log.d(TAG, "Camera permission denied")
            Toast.makeText(this, "Camera permission required", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            binding = ActivityCameraScanBinding.inflate(layoutInflater)
            setContentView(binding.root)
            cameraExecutor = Executors.newSingleThreadExecutor()

            // Initialize advanced recognition components
            scanMode = parseScanMode(intent?.getStringExtra(EXTRA_SCAN_MODE))

            teaLeafRecognizer = TeaLeafRecognizer(this)
            qualityAnalyzer = ImageQualityAnalyzer(this)

            setupUI()
            checkCameraPermission()
        } catch (e: Exception) {
            Log.e(TAG, "onCreate error", e)
            Toast.makeText(this, "Initialization error: ${e.message}", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun setupUI() {
        try {
            binding.closeButton.setOnClickListener { finish() }
            binding.flashButton.setOnClickListener {
                try {
                    camera?.cameraControl?.enableTorch(
                        camera?.cameraInfo?.torchState?.value != TorchState.ON
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Flash toggle error", e)
                }
            }

            // Update UI based on scan mode
            val (modeText, modeIcon, modeDescription) = when (scanMode) {
                ScanMode.BARCODE_ONLY -> Triple("Barcode", "📦 Barcode", "Position barcode in frame")
                ScanMode.IMAGE_ONLY -> Triple("Image", "📷 Image", "Scan tea image or bag")
                ScanMode.TEXT_ONLY -> Triple("Text", "📄 Text", "Scan packaging label")
                else -> Triple("Smart", "🔍 Smart", "Barcode • Text • Image")
            }

            binding.scanModeIndicator.text = "$modeText Mode"
            binding.modeChip.text = modeIcon
            binding.scanInfoText.text = modeDescription

            Log.d(TAG, "Camera UI updated for mode: $modeText")
        } catch (e: Exception) {
            Log.e(TAG, "setupUI error", e)
        }
    }

    private fun parseScanMode(raw: String?): ScanMode {
        return when (raw) {
            SCAN_MODE_BARCODE -> ScanMode.BARCODE_ONLY
            SCAN_MODE_IMAGE -> ScanMode.IMAGE_ONLY
            SCAN_MODE_TEXT -> ScanMode.TEXT_ONLY
            else -> ScanMode.ALL
        }
    }

    private fun checkCameraPermission() {
        try {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                startCamera()
            } else {
                requestPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Permission check error", e)
            finish()
        }
    }

    private fun startCamera() {
        try {
            val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
            cameraProviderFuture.addListener({
                try {
                    cameraProvider = cameraProviderFuture.get()

                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(binding.previewView.surfaceProvider)
                    }

                    imageAnalyzer = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()
                        .also {
                            it.setAnalyzer(cameraExecutor!!, ImageAnalyzer())
                        }

                    cameraProvider?.unbindAll()
                    camera = cameraProvider?.bindToLifecycle(
                        this@CameraScanActivity,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        imageAnalyzer
                    )
                    Log.d(TAG, "Camera started successfully")

                } catch (e: Exception) {
                    Log.e(TAG, "Camera setup error", e)
                    runOnUiThread {
                        Toast.makeText(this@CameraScanActivity, "Camera error: ${e.message}", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                }
            }, ContextCompat.getMainExecutor(this))
        } catch (e: Exception) {
            Log.e(TAG, "StartCamera error", e)
            Toast.makeText(this, "Camera initialization failed", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    @OptIn(ExperimentalGetImage::class)
    private inner class ImageAnalyzer : ImageAnalysis.Analyzer {
        private var lastAnalyzedTimestamp = 0L
        private var isProcessing = false
        
        // Multi-frame voting buffer
        private val recognitionBuffer = mutableListOf<String>()
        private val BUFFER_SIZE = 5

        private val barcodeDatabase by lazy {
            loadBarcodesFromCSV()
        }

        private fun loadBarcodesFromCSV(): Map<String, String> {
            val barcodeMap = mutableMapOf<String, String>()
            try {
                val inputStream = this@CameraScanActivity.assets.open("tea_barcodes.csv")
                inputStream.bufferedReader().useLines { lines ->
                    val rows = lines.toList()
                    if (rows.isEmpty()) return@useLines

                    val headerParts = rows.first().split(",").map { it.trim().lowercase() }
                    val barcodeIndex = headerParts.indexOf("barcode")
                    val teaTypeIndex = headerParts.indexOf("tea_type")

                    if (barcodeIndex == -1 || teaTypeIndex == -1) {
                        Log.w(TAG, "tea_barcodes.csv is missing required columns 'barcode' and 'tea_type'")
                    } else {
                        rows.drop(1).forEach { line ->
                            val parts = line.split(",")
                            if (parts.size <= maxOf(barcodeIndex, teaTypeIndex)) return@forEach

                            val barcode = ScanNormalization.normalizeBarcode(parts[barcodeIndex])
                            val teaType = parts[teaTypeIndex].trim().lowercase()
                            if (barcode.isNotBlank() && teaType in supportedTeaTypes) {
                                barcodeMap[barcode] = teaType
                            }
                        }
                    }
                }

                // Merge user-added mappings only when mapped tea remains supported.
                val customMappings = BarcodeMappingStore.getAllMappings(this@CameraScanActivity)
                customMappings.forEach { (barcode, mapping) ->
                    val mappedTea = mapping.teaType.lowercase()
                    if (mappedTea in supportedTeaTypes) {
                        barcodeMap[ScanNormalization.normalizeBarcode(barcode)] = mappedTea
                    }
                }

                Log.d(TAG, "Loaded ${barcodeMap.size} supported barcodes (CSV + custom mappings)")
            } catch (e: Exception) {
                Log.e(TAG, "Error loading barcodes from CSV: ${e.message}")
                // Fallback to hardcoded barcodes if CSV fails
                return mapOf(
                    "041000000027" to "green tea",
                    "794522219001" to "chai tea",
                    "794522219018" to "chamomile tea"
                )
            }
            return barcodeMap
        }

        private val teaTypeMap = mapOf(
            "green tea" to listOf("green tea", "sencha"),
            "black tea" to listOf("black tea", "english breakfast", "ceylon", "assam", "darjeeling", "earl grey", "earl gray"),
            "oolong tea" to listOf("oolong tea", "oolong", "wulong"),
            "chamomile tea" to listOf("chamomile tea", "chamomile", "camomile"),
            "peppermint tea" to listOf("peppermint tea", "peppermint", "mint tea"),
            "ginger tea" to listOf("ginger tea", "ginger"),
            "hibiscus tea" to listOf("hibiscus tea", "hibiscus"),
            "rooibos tea" to listOf("rooibos tea", "rooibos", "red bush"),
            "lavender tea" to listOf("lavender tea", "lavender"),
            "matcha tea" to listOf("matcha tea", "matcha"),
            "chai tea" to listOf("chai tea", "chai", "masala chai"),
            "turmeric tea" to listOf("turmeric tea", "turmeric"),
            "rosehip tea" to listOf("rosehip tea", "rosehip", "rose hip"),
            "blueberry tea" to listOf("blueberry tea", "blueberry"),
            "raspberry tea" to listOf("raspberry tea", "raspberry"),
            "kukicha tea" to listOf("kukicha tea", "kukicha"),
            "genmaicha tea" to listOf("genmaicha tea", "genmaicha"),
            "lemon tea" to listOf("lemon tea", "lemon")
        )

        @ExperimentalGetImage
        override fun analyze(imageProxy: ImageProxy) {
            try {
                val currentTimestamp = System.currentTimeMillis()
                // Increased analysis frequency (500ms)
                if (currentTimestamp - lastAnalyzedTimestamp < 500 || isProcessing) {
                    imageProxy.close()
                    return
                }
                lastAnalyzedTimestamp = currentTimestamp
                isProcessing = true

                val mediaImage = imageProxy.image
                if (mediaImage == null) {
                    isProcessing = false
                    imageProxy.close()
                    return
                }

                val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

                if (isFinishing || isDestroyed) {
                    isProcessing = false
                    imageProxy.close()
                    return
                }

                // TIER 1: Try Barcode Scanning
                if (scanMode == ScanMode.ALL || scanMode == ScanMode.BARCODE_ONLY) {
                    barcodeScanner.process(image)
                        .addOnSuccessListener(cameraExecutor!!) { barcodes ->
                            try {
                                if (isFinishing || isDestroyed) {
                                    imageProxy.close()
                                    return@addOnSuccessListener
                                }

                                var barcodeFound = false
                                var unknownBarcode: String? = null
                                
                                if (barcodes.isNotEmpty()) {
                                    for (barcode in barcodes) {
                                        val barcodeValue = ScanNormalization.normalizeBarcode(barcode.rawValue ?: "")
                                        barcodeDatabase[barcodeValue]?.let { teaType ->
                                            barcodeFound = true
                                            val displayName = formatTeaName(teaType)
                                            Log.d(TAG, "✓ Barcode match (Tier 1): $displayName from barcode: $barcodeValue (99%)")
                                            returnResult(
                                                teaName = displayName,
                                                method = "barcode",
                                                confidence = 0.99f,
                                                barcodeRawValue = barcodeValue,
                                                barcodeMappedTeaType = teaType
                                            )
                                            imageProxy.close()
                                            return@addOnSuccessListener
                                        }
                                    }
                                    if (!barcodeFound && barcodes.isNotEmpty()) {
                                        unknownBarcode = ScanNormalization.normalizeBarcode(barcodes[0].rawValue ?: "")
                                        Log.d(TAG, "⚠ Unknown barcode detected: $unknownBarcode")
                                    }
                                }

                                if (!barcodeFound) {
                                    if (scanMode == ScanMode.BARCODE_ONLY && !unknownBarcode.isNullOrBlank()) {
                                        returnResult(
                                            teaName = "UNRECOGNIZED_BARCODE:$unknownBarcode",
                                            method = "barcode_unverified",
                                            confidence = 0.0f,
                                            barcodeRawValue = unknownBarcode
                                        )
                                        imageProxy.close()
                                    } else {
                                        tryTier2TextRecognition(image, imageProxy, unknownBarcode)
                                    }
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "Barcode processing error: ${e.message}")
                                isProcessing = false
                                tryTier2TextRecognition(image, imageProxy, null)
                            }
                        }
                        .addOnFailureListener(cameraExecutor!!) { e ->
                            Log.e(TAG, "Barcode scanning failed: ${e.message}")
                            isProcessing = false
                            tryTier2TextRecognition(image, imageProxy, null)
                        }
                } else {
                    tryTier2TextRecognition(image, imageProxy, null)
                }

            } catch (e: Exception) {
                Log.e(TAG, "Analyze error: ${e.message}", e)
                isProcessing = false
                try {
                    imageProxy.close()
                } catch (closeException: Exception) {
                    Log.e(TAG, "Error closing imageProxy in exception handler: ${closeException.message}")
                }
            }
        }

        private fun tryTier2TextRecognition(image: InputImage, imageProxy: ImageProxy, detectedBarcode: String?) {
            if (scanMode == ScanMode.ALL || scanMode == ScanMode.TEXT_ONLY) {
                textRecognizer.process(image)
                    .addOnSuccessListener(cameraExecutor!!) { visionText ->
                        try {
                            if (isFinishing || isDestroyed) {
                                imageProxy.close()
                                return@addOnSuccessListener
                            }

                            if (visionText.text.isNotEmpty()) {
                                val detectedText = visionText.text.lowercase()

                                val matches = mutableListOf<Triple<String, String, Int>>()

                                teaTypeMap.forEach { (teaName, variations) ->
                                    variations.forEach { variation ->
                                        val boundaryPattern = Regex("\\b${Regex.escape(variation)}\\b")
                                        if (boundaryPattern.containsMatchIn(detectedText)) {
                                            val score = variation.length
                                            matches += Triple(teaName, variation, score)
                                        }
                                    }
                                }

                                if (matches.isNotEmpty()) {
                                    val top = matches.maxByOrNull { it.third }!!
                                    val displayName = formatTeaName(top.first)
                                    Log.d(TAG, "✓ Text match (Tier 2): $displayName (85%) - Matched: '${top.second}'")
                                    returnResult(displayName, "text", 0.85f)
                                    imageProxy.close()
                                    return@addOnSuccessListener
                                }
                            }

                            tryTier3ImageRecognition(imageProxy, detectedBarcode)
                        } catch (e: Exception) {
                            Log.e(TAG, "Text processing error: ${e.message}")
                            isProcessing = false
                            tryTier3ImageRecognition(imageProxy, detectedBarcode)
                        }
                    }
                    .addOnFailureListener(cameraExecutor!!) { e ->
                        Log.e(TAG, "Text recognition failed: ${e.message}")
                        isProcessing = false
                        tryTier3ImageRecognition(imageProxy, detectedBarcode)
                    }
            } else {
                tryTier3ImageRecognition(imageProxy, detectedBarcode)
            }
        }

        private fun tryTier3ImageRecognition(imageProxy: ImageProxy, detectedBarcode: String?) {
            if (scanMode == ScanMode.ALL || scanMode == ScanMode.IMAGE_ONLY) {
                val bitmap = imageProxyToBitmap(imageProxy)
                if (bitmap == null) {
                    Log.e(TAG, "Image conversion failed for Tier 3")
                    isProcessing = false
                    imageProxy.close()
                    return
                }

                try {
                    val qualityMetrics = qualityAnalyzer.analyzeImageQuality(bitmap)
                    Log.d(TAG, "🖼️ Image Quality Score: ${qualityMetrics.overallScore.toInt()}% | Sharpness: ${qualityMetrics.sharpness.toInt()} | Contrast: ${qualityMetrics.contrast.toInt()} | Brightness: ${qualityMetrics.brightness.toInt()}")

                    Log.d(TAG, "🚀 Running TFLite model inference (size: ${bitmap.width}x${bitmap.height}, quality: ${qualityMetrics.overallScore.toInt()}%)...")
                    teaLeafRecognizer.recognizeTeaFromImage(bitmap) { result ->
                        try {
                            if (isFinishing || isDestroyed) {
                                return@recognizeTeaFromImage
                            }

                            if (result.error.isEmpty() && result.method == "neural_network") {
                                val detectedType = result.teaType
                                
                                // TEMPORAL VOTING SYSTEM
                                recognitionBuffer.add(detectedType)
                                if (recognitionBuffer.size > BUFFER_SIZE) {
                                    recognitionBuffer.removeAt(0)
                                }

                                // Count occurrences
                                val counts = recognitionBuffer.groupingBy { it }.eachCount()
                                val bestMatch = counts.maxByOrNull { it.value }
                                
                                Log.d(TAG, "🗳️ Voting Buffer: ${recognitionBuffer.joinToString(", ")} | Consensus: ${bestMatch?.key} (${bestMatch?.value}/$BUFFER_SIZE)")

                                // Require majority (e.g., 3 out of 5)
                                if (bestMatch != null && bestMatch.value >= 3) {
                                    val finalType = formatTeaName(bestMatch.key)
                                    Log.d(TAG, "🏆 Consensus Reached: $finalType - Confidence: ${"%.2f".format(result.confidence)}")
                                    returnResult(
                                        teaName = finalType,
                                        method = "image_recognition",
                                        confidence = result.confidence,
                                        topPredictions = result.topPredictions
                                    )
                                    return@recognizeTeaFromImage
                                }
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Recognition handling error: ${e.message}")
                        } finally {
                            isProcessing = false
                            try {
                                imageProxy.close()
                            } catch (e: Exception) {
                                // Ignore
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Advanced analysis error: ${e.message}", e)
                    isProcessing = false
                    imageProxy.close()
                }
            } else {
                isProcessing = false
                imageProxy.close()
            }
        }


        private fun determineQualityIssue(metrics: ImageQualityAnalyzer.QualityMetrics): String? {
            // Only reject for OBVIOUS non-tea images, not just lower-quality photos
            // Real smartphone photos won't have perfect metrics
            
            Log.d(TAG, "Quality Issue Check: brightness=${metrics.brightness.toInt()}, saturation=${metrics.saturation.toInt()}, contrast=${metrics.contrast.toInt()}, noiseLevel=${metrics.noiseLevel.toInt()}")

            // EXTREMELY FLAT: No texture details - nearly unrecognizable
            if (metrics.overallScore < 15f) {
                Log.d(TAG, "❌ REJECTION: Extremely flat image (score < 15%)")
                return "extremely_flat"
            }

            // PAPER-LIKE: Very bright, desaturated, flat (typical of random products/documents)
            if (metrics.brightness > 190f && metrics.saturation < 12f && metrics.contrast < 20f) {
                Log.d(TAG, "❌ REJECTION: Paper-like appearance detected")
                return "paper_like"
            }

            // WASHED OUT: Extreme overexposure with very low saturation
            if (metrics.brightness > 200f && metrics.saturation < 6f) {
                Log.d(TAG, "❌ REJECTION: Washed out image (extreme overexposure)")
                return "washed_out"
            }

            // TOO DARK: Cannot see details
            if (metrics.brightness < ImageQualityAnalyzer.MIN_BRIGHTNESS) {
                Log.d(TAG, "❌ REJECTION: Too dark (brightness < ${ImageQualityAnalyzer.MIN_BRIGHTNESS})")
                return "too_dark"
            }

            // TOO BRIGHT: Extreme glare
            if (metrics.brightness > 220f) {
                Log.d(TAG, "❌ REJECTION: Too bright (extreme glare)")
                return "too_bright"
            }

            // VERY NOISY: Very low quality camera feed
            if (metrics.noiseLevel > 900f) {
                Log.d(TAG, "❌ REJECTION: Very noisy image")
                return "noisy"
            }

            // Allow normal smartphone photos through - don't reject just for being slightly blurry or low contrast
            Log.d(TAG, "✅ PASS: Image quality acceptable for inference")
            return null
        }

        @OptIn(ExperimentalGetImage::class)
        private fun imageProxyToBitmap(imageProxy: ImageProxy): Bitmap? {
            return try {
                // Use CameraX's built-in conversion which is more robust than manual YUV parsing
                val bitmap = imageProxy.toBitmap()

                // Precise Center-Square Crop to prevent distortion
                val width = bitmap.width
                val height = bitmap.height
                val squareSize = minOf(width, height)
                
                val left = (width - squareSize) / 2
                val top = (height - squareSize) / 2
                
                Bitmap.createBitmap(bitmap, left, top, squareSize, squareSize)
            } catch (e: Exception) {
                Log.e(TAG, "ImageProxy to Bitmap conversion error: ${e.message}")
                null
            }
        }

        private fun formatTeaName(tea: String): String {
            return tea.split(" ").joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
        }

        private fun hasTeaContext(text: String): Boolean {
            val contextKeywords = listOf(
                "tea", "teabag", "tea bag", "chai", "matcha", "herbal", "rooibos", "oolong", "sencha"
            )
            return contextKeywords.any { keyword ->
                Regex("\\b${Regex.escape(keyword)}\\b").containsMatchIn(text)
            }
        }

        private fun returnResult(
            teaName: String,
            method: String,
            confidence: Float = 1.0f,
            barcodeRawValue: String? = null,
            barcodeMappedTeaType: String? = null,
            qualityIssue: String? = null,
            topPredictions: List<TeaLeafRecognizer.Prediction>? = null
        ) {
            try {
                val resultIntent = android.content.Intent().apply {
                    putExtra("scannedTeaType", teaName)
                    putExtra("scanMethod", method)
                    putExtra("confidence", confidence)
                    
                    if (topPredictions != null && topPredictions.isNotEmpty()) {
                        val names = ArrayList(topPredictions.map { it.teaType })
                        val confs = topPredictions.map { it.confidence }.toFloatArray()
                        putStringArrayListExtra("topPredictionNames", names)
                        putExtra("topPredictionConfidences", confs)
                    }

                    if (!qualityIssue.isNullOrBlank()) {
                        putExtra("scanIssue", qualityIssue)
                    }
                    if (!barcodeRawValue.isNullOrBlank()) {
                        putExtra("barcodeRawValue", barcodeRawValue)
                    }
                    if (!barcodeMappedTeaType.isNullOrBlank()) {
                        putExtra("barcodeMappedTeaType", formatTeaName(barcodeMappedTeaType))
                    }
                }
                setResult(RESULT_OK, resultIntent)
                finish()
            } catch (e: Exception) {
                Log.e(TAG, "Return result error: ${e.message}")
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor?.shutdown()
        textRecognizer.close()
        barcodeScanner.close()
        teaLeafRecognizer.release()
    }
}
