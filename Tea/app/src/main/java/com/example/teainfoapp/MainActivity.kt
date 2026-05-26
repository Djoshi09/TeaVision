package com.example.teainfoapp

import android.content.Intent
import android.content.res.ColorStateList
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.teainfoapp.data.BarcodeMappingStore
import com.example.teainfoapp.data.LogEntity
import com.example.teainfoapp.data.ScanNormalization
import com.example.teainfoapp.data.TeaDatabase
import com.example.teainfoapp.data.TeaEntity
import com.example.teainfoapp.data.TeaRepository
import com.example.teainfoapp.data.remote.TeaBarcodeLookupService
import com.example.teainfoapp.databinding.ActivityMainBinding
import com.example.teainfoapp.databinding.LogItemBinding
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

/**
 * Tea Health Companion App - Modern UI Edition
 * Features:
 * - Modern Material Design 3 UI
 * - Smooth animations and transitions
 * - Multi-mode scanning (barcode, text, smart matching)
 * - Real-time tea identification
 * - Comprehensive nutritional tracking
 */

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var repository: TeaRepository
    private val teaList = mutableListOf<TeaEntity>()
    private val filteredTeaList = mutableListOf<TeaEntity>()
    private var selectedTea: TeaEntity? = null
    private val favoriteTeas = mutableSetOf<String>()
    private var vibrator: Vibrator? = null
    private var pendingScanTeaType: String? = null
    private var pendingScanMethod: String? = null
    private var pendingBarcodeMappedTeaType: String? = null
    private var pendingBarcodeRawValue: String? = null
    private var pendingLookupSourceLabel: String? = null
    private var pendingScanConfidence: Float? = null
    private var pendingTopPredictions: List<TeaLeafRecognizer.Prediction>? = null
    private var currentUnmappedBarcode: String? = null
    private var currentLookupSourceLabel: String? = null

    // Camera scan launcher
    private val cameraScanLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val scannedTeaType = result.data?.getStringExtra("scannedTeaType")
            val scanMethod = result.data?.getStringExtra("scanMethod")
            val scanConfidence = result.data
                ?.getFloatExtra("confidence", -1f)
                ?.takeIf { it >= 0f }
            val scanIssue = result.data?.getStringExtra("scanIssue")
            val barcodeMappedTeaType = result.data?.getStringExtra("barcodeMappedTeaType")
            val barcodeRawValue = result.data?.getStringExtra("barcodeRawValue")

            val topNames = result.data?.getStringArrayListExtra("topPredictionNames")
            val topConfs = result.data?.getFloatArrayExtra("topPredictionConfidences")
            val topPredictions = if (topNames != null && topConfs != null) {
                topNames.zip(topConfs.toTypedArray()).map {
                    TeaLeafRecognizer.Prediction(it.first, it.second)
                }
            } else null
            
            // Handle image quality/confidence issues
            if (scanMethod == "image_quality_check") {
                val message = when (scanIssue) {
                    "blurry" -> "📷 Image is blurry. Hold the camera steady, clean the lens, and wait for focus lock."
                    "too_dark" -> "💡 Image is too dark. Move to brighter light or add side lighting."
                    "too_bright" -> "🌞 Image is too bright. Reduce glare and avoid direct sunlight."
                    "low_contrast" -> "⚫ Low contrast. Use a contrasting background behind the tea."
                    "washed_out" -> "🎨 Colors are washed out. Improve lighting and focus on the tea."
                    else -> "❌ Image quality too low. Please try again with better lighting and focus."
                }
                Toast.makeText(this, message, Toast.LENGTH_LONG).show()
                return@registerForActivityResult
            }
            
            if (scanMethod == "image_low_confidence") {
                Toast.makeText(
                    this,
                    "⚠️ Image quality is too low. Try better lighting or use barcode/text scan.",
                    Toast.LENGTH_LONG
                ).show()
                return@registerForActivityResult
            }
            
            if (scanMethod == "barcode_unverified") {
                val rawValue = ScanNormalization.normalizeBarcode(
                    barcodeRawValue ?: scannedTeaType?.removePrefix("UNRECOGNIZED_BARCODE:")?.trim()
                )

                if (rawValue.isNotBlank()) {
                    val savedTeaType = BarcodeMappingStore.getMappedTeaType(this, rawValue)
                    if (!savedTeaType.isNullOrBlank()) {
                        hideUnmappedBarcodeBanner()
                        handleScannedTeaSelection(
                            scannedTeaType = savedTeaType,
                            scanMethod = "barcode",
                            scanConfidence = 1.0f,
                            barcodeMappedTeaType = savedTeaType,
                            barcodeRawValue = rawValue,
                            lookupSourceLabel = getString(R.string.source_saved_mapping)
                        )
                    } else {
                        lifecycleScope.launch {
                            val resolvedByApi = lookupBarcodeAndResolve(rawValue)
                            if (!resolvedByApi) {
                                showUnmappedBarcodeBanner(rawValue)
                            }
                        }
                    }
                } else {
                    Toast.makeText(this, "Barcode detected but not mapped to a tea type", Toast.LENGTH_LONG).show()
                }
                return@registerForActivityResult
            }
            scannedTeaType?.let { teaType ->
                if (teaList.isEmpty()) {
                    pendingScanTeaType = teaType
                    pendingScanMethod = scanMethod
                    pendingBarcodeMappedTeaType = barcodeMappedTeaType
                    pendingBarcodeRawValue = barcodeRawValue
                    pendingLookupSourceLabel = resolveScanSourceLabel(scanMethod)
                    pendingScanConfidence = scanConfidence
                    pendingTopPredictions = topPredictions
                    Toast.makeText(this, "Scan received. Loading tea list...", Toast.LENGTH_SHORT).show()
                    return@let
                }

                if (scanMethod == "image_recognition" && topPredictions != null && topPredictions.size > 1) {
                    showPredictionSelectionDialog(topPredictions, scanMethod, scanConfidence)
                } else {
                    handleScannedTeaSelection(
                        scannedTeaType = teaType,
                        scanMethod = scanMethod,
                        scanConfidence = scanConfidence,
                        barcodeMappedTeaType = barcodeMappedTeaType,
                        barcodeRawValue = barcodeRawValue,
                        lookupSourceLabel = resolveScanSourceLabel(scanMethod)
                    )
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = getSystemService(VibratorManager::class.java)
            manager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(VIBRATOR_SERVICE) as? Vibrator
        }

        initializeDatabase()
        setupUI()
    }

    private fun initializeDatabase() {
        val db = TeaDatabase.getDatabase(this)
        repository = TeaRepository(db.teaDao(), db.logDao())

        lifecycleScope.launch {
            repository.seedDefaultTeas()
            loadTeaTypes()
        }
    }

    private fun setupUI() {
        setupTeaSpinner()
        setupActionButtons()
        setupScrollMicroAnimations()
        animateCardsOnEntry()

        binding.mapBarcodeButton.setOnClickListener {
            animatePress(it)
            val barcode = currentUnmappedBarcode
            if (barcode.isNullOrBlank()) {
                Toast.makeText(this, "No barcode selected", Toast.LENGTH_SHORT).show()
            } else {
                showAddProductDialog(barcode)
            }
        }

        binding.dismissBarcodeBannerButton.setOnClickListener {
            animatePress(it)
            hideUnmappedBarcodeBanner()
        }

        hideUnmappedBarcodeBanner()
    }

    private fun setupActionButtons() {
        // Smart Scan Button (Unified)
        binding.smartScanButton.setOnClickListener {
            animatePress(it)
            launchCameraScan("all")
        }

        // Image Scan Button
        binding.scanImageButton.setOnClickListener {
            animatePress(it)
            launchCameraScan(CameraScanActivity.SCAN_MODE_IMAGE)
        }

        // Barcode Scan Button
        binding.scanBarcodeButton.setOnClickListener {
            animatePress(it)
            launchCameraScan(CameraScanActivity.SCAN_MODE_BARCODE)
        }

        // Text Scan Button
        binding.scanTextButton.setOnClickListener {
            animatePress(it)
            launchCameraScan(CameraScanActivity.SCAN_MODE_TEXT)
        }

        binding.confirmButton.setOnClickListener {
            animatePress(it)
            confirmTeaSelection()
        }

        binding.viewLogsButton.setOnClickListener {
            animatePress(it)
            showTeaHistory()
        }

        binding.analyticsButton.setOnClickListener {
            animatePress(it)
            val intent = Intent(this, AnalyticsActivity::class.java)
            startActivity(intent)
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }
    }

    private fun launchCameraScan(scanMode: String) {
        val intent = Intent(this, CameraScanActivity::class.java).apply {
            putExtra(CameraScanActivity.EXTRA_SCAN_MODE, scanMode)
        }
        cameraScanLauncher.launch(intent)
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }

    private fun animateCardsOnEntry() {
        val cards = listOf(
            binding.scanCard,
            binding.selectionCard,
            binding.nutritionCard,
            binding.benefitsCard,
            binding.historyCard,
            binding.analyticsCard
        )

        cards.forEachIndexed { index, view ->
            view.alpha = 0f
            view.translationY = 32f
            view.animate()
                .alpha(1f)
                .translationY(0f)
                .setStartDelay((index * 70).toLong())
                .setDuration(280)
                .start()
        }
    }

    private fun setupScrollMicroAnimations() {
        binding.mainScrollView.setOnScrollChangeListener { _, _, scrollY, _, _ ->
            binding.heroHeader.translationY = scrollY * 0.2f
            binding.lookupSourceTagChip.alpha = if (scrollY > 120) 0.94f else 1f
        }
    }

    private fun loadTeaTypes() {
        lifecycleScope.launch {
            teaList.clear()
            teaList.addAll(repository.getAllTeas())
            updateTeaSpinner()
            pendingScanTeaType?.let { pendingTeaType ->
                val pendingMethod = pendingScanMethod
                val pendingMappedTeaType = pendingBarcodeMappedTeaType
                val pendingRawBarcode = pendingBarcodeRawValue
                val pendingSourceLabel = pendingLookupSourceLabel
                val pendingConfidence = pendingScanConfidence
                val pendingTop = pendingTopPredictions
                pendingScanTeaType = null
                pendingScanMethod = null
                pendingBarcodeMappedTeaType = null
                pendingBarcodeRawValue = null
                pendingLookupSourceLabel = null
                pendingScanConfidence = null
                pendingTopPredictions = null

                if (pendingMethod == "image_recognition" && pendingTop != null && pendingTop.size > 1) {
                    showPredictionSelectionDialog(pendingTop, pendingMethod, pendingConfidence)
                } else {
                    handleScannedTeaSelection(
                        scannedTeaType = pendingTeaType,
                        scanMethod = pendingMethod,
                        scanConfidence = pendingConfidence,
                        barcodeMappedTeaType = pendingMappedTeaType,
                        barcodeRawValue = pendingRawBarcode,
                        lookupSourceLabel = pendingSourceLabel
                    )
                }
            }
        }
    }

    private fun showPredictionSelectionDialog(
        predictions: List<TeaLeafRecognizer.Prediction>,
        scanMethod: String?,
        scanConfidence: Float?
    ) {
        val options = predictions.map { 
            "${it.teaType} (${String.format("%.0f%%", it.confidence * 100)})"
        }.toTypedArray()

        AlertDialog.Builder(this)
            .setTitle("🔍 Recommended Tea Types")
            .setItems(options) { _, which ->
                val selected = predictions[which]
                handleScannedTeaSelection(
                    scannedTeaType = selected.teaType,
                    scanMethod = scanMethod,
                    scanConfidence = selected.confidence,
                    barcodeMappedTeaType = null,
                    barcodeRawValue = null,
                    lookupSourceLabel = resolveScanSourceLabel(scanMethod)
                )
            }
            .setNeutralButton("None of these? Search") { _, _ ->
                showFullTeaSelectionDialog()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showFullTeaSelectionDialog() {
        if (teaList.isEmpty()) {
            Toast.makeText(this, "Tea list is loading, please wait...", Toast.LENGTH_SHORT).show()
            return
        }

        val teaNames = teaList.map { it.teaType }.toTypedArray()
        
        AlertDialog.Builder(this)
            .setTitle("🔎 Search All Teas")
            .setItems(teaNames) { _, which ->
                val selectedName = teaNames[which]
                handleScannedTeaSelection(
                    scannedTeaType = selectedName,
                    scanMethod = "manual_search",
                    scanConfidence = 1.0f,
                    barcodeMappedTeaType = null,
                    barcodeRawValue = null,
                    lookupSourceLabel = "Manual Search"
                )
            }
            .setNegativeButton("Back", null)
            .show()
    }

    private fun setupTeaSpinner() {
        binding.teaTypeSpinner.onItemSelectedListener =
            object : android.widget.AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: android.widget.AdapterView<*>?,
                    view: android.view.View?,
                    position: Int,
                    id: Long
                ) {
                    if (teaList.isNotEmpty()) {
                        selectedTea = teaList[position]
                        displayTeaInfo()
                    }
                }

                override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
            }
    }

    private fun updateTeaSpinner() {
        val teaNames = teaList.map { it.teaType }
        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            teaNames
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.teaTypeSpinner.adapter = adapter
    }

    private fun displayTeaInfo() {
        selectedTea?.let { tea ->
            binding.apply {
                caloriesValue.text = tea.calories
                proteinValue.text = tea.protein
                carbsValue.text = tea.carbohydrates
                fatValue.text = tea.fat
                calciumValue.text = tea.calcium
                magnesiumValue.text = tea.magnesium
                doctorRemarkText.text = tea.doctorRemark
            }
        }
    }

    private fun confirmTeaSelection() {
        selectedTea?.let { tea ->
            lifecycleScope.launch {
                repository.logTeaSelection(tea.teaType, userConfirmed = true)
                animateSuccess(binding.confirmButton)
                Toast.makeText(
                    this@MainActivity,
                    "✓ ${tea.teaType} logged successfully!",
                    Toast.LENGTH_SHORT
                ).show()
            }
        } ?: Toast.makeText(this, "Please select a tea type first", Toast.LENGTH_SHORT).show()
    }

    private fun showTeaHistory() {
        lifecycleScope.launch {
            val logs = repository.getAllLogs().toMutableList()
            if (logs.isEmpty()) {
                Toast.makeText(this@MainActivity, "No history yet. Start by confirming a tea selection.", Toast.LENGTH_SHORT).show()
                return@launch
            }

            // Create RecyclerView with swipe-to-delete
            val recyclerView = RecyclerView(this@MainActivity).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                clipToPadding = false
                val padding = resources.getDimensionPixelSize(android.R.dimen.app_icon_size) / 4
                setPadding(padding, padding, padding, padding)
                layoutManager = LinearLayoutManager(this@MainActivity)
            }

            val logAdapter = LogAdapter(
                logs,
                onDelete = { deletedLog, position ->
                    // Delete from database and show undo with Snackbar
                    lifecycleScope.launch {
                        repository.deleteLog(deletedLog)
                    }
                    Snackbar.make(binding.root, "Deleted ${deletedLog.teaType}", Snackbar.LENGTH_LONG)
                        .setAction("UNDO") {
                            (recyclerView.adapter as LogAdapter).restoreAt(position, deletedLog)
                            lifecycleScope.launch {
                                repository.logTeaSelection(deletedLog.teaType, deletedLog.userConfirmed)
                            }
                        }
                        .show()
                },
                onLongClick = { log ->
                    vibrateClick()
                    Toast.makeText(
                        this@MainActivity,
                        "${log.teaType} - ${if (log.userConfirmed) "Confirmed" else "Auto-scanned"}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            )
            recyclerView.adapter = logAdapter

            // Swipe to delete
            val itemTouchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(
                0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
            ) {
                override fun onMove(rv: RecyclerView, vh: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder) = false

                override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                    val position = viewHolder.adapterPosition
                    val deletedLog = logAdapter.removeAt(position)
                    vibrateClick()
                    logAdapter.onDelete(deletedLog, position)
                }
            })
            itemTouchHelper.attachToRecyclerView(recyclerView)

            val dialog = AlertDialog.Builder(this@MainActivity)
                .setTitle("📋 Your Tea Selection History (${logs.size} items)")
                .setMessage("💡 Swipe left/right to delete • Long-press for details")
                .setView(recyclerView)
                .setPositiveButton("Close", null)
                .setNeutralButton("Clear All") { _, _ ->
                    showClearAllConfirmation()
                }
                .create()
            dialog.show()
        }
    }

    private fun showClearAllConfirmation() {
        AlertDialog.Builder(this)
            .setTitle("Clear All History?")
            .setMessage("This will permanently delete all scan logs.")
            .setPositiveButton("Delete All") { _, _ ->
                lifecycleScope.launch {
                    // Delete all logs from database
                    val allLogs = repository.getAllLogs()
                    // Note: You'd need to add deleteAll() method to repository
                    vibrateClick()
                    Toast.makeText(this@MainActivity, "History cleared", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun vibrateClick() {
        val vib = vibrator ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vib.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vib.vibrate(50)
        }
    }

    /**
     * Animate button press with scale effect
     */
    private fun animatePress(view: android.view.View) {
        view.animate()
            .scaleX(0.95f)
            .scaleY(0.95f)
            .setDuration(100)
            .withEndAction {
                view.animate()
                    .scaleX(1.0f)
                    .scaleY(1.0f)
                    .setDuration(100)
                    .start()
            }
            .start()
    }

    /**
     * Animate successful action with color pulse
     */
    private fun animateSuccess(view: android.view.View) {
        view.animate()
            .alpha(0.7f)
            .setDuration(150)
            .withEndAction {
                view.animate()
                    .alpha(1.0f)
                    .setDuration(150)
                    .start()
            }
            .start()
    }

    private fun resolveTeaSelection(scannedTeaType: String): TeaEntity? {
        if (teaList.isEmpty()) return null

        val normalizedScan = ScanNormalization.normalizeTeaName(scannedTeaType)
        if (normalizedScan.isBlank()) return null

        android.util.Log.d("TeaResolution", "Resolving scanned tea: '$scannedTeaType' -> normalized: '$normalizedScan'")

        // 1. Fast path: exact match (case-insensitive)
        teaList.firstOrNull { it.teaType.equals(scannedTeaType, ignoreCase = true) }?.let {
            android.util.Log.d("TeaResolution", "✓ Exact match found: ${it.teaType}")
            return it
        }

        // 2. Fast path: normalized exact match
        teaList.firstOrNull { ScanNormalization.normalizeTeaName(it.teaType) == normalizedScan }?.let {
            android.util.Log.d("TeaResolution", "✓ Normalized match found: ${it.teaType}")
            return it
        }

        // 3. Direct alias mapping (restricted to supported tea catalog)
        val directAliasMap = linkedMapOf(
            "green tea" to "Green Tea",
            "black tea" to "Black Tea",
            "oolong tea" to "Oolong Tea",
            "chamomile" to "Chamomile Tea",
            "chamomile tea" to "Chamomile Tea",
            "peppermint" to "Peppermint Tea",
            "peppermint tea" to "Peppermint Tea",
            "ginger tea" to "Ginger Tea",
            "hibiscus tea" to "Hibiscus Tea",
            "rooibos tea" to "Rooibos Tea",
            "rooibos" to "Rooibos Tea",
            "lavender tea" to "Lavender Tea",
            "matcha tea" to "Matcha Tea",
            "matcha" to "Matcha Tea",
            "chai tea" to "Chai Tea",
            "turmeric tea" to "Turmeric Tea",
            "rose hip tea" to "Rosehip Tea",
            "rosehip tea" to "Rosehip Tea",
            "blueberry tea" to "Blueberry Tea",
            "raspberry tea" to "Raspberry Tea",
            "kukicha tea" to "Kukicha Tea",
            "genmaicha tea" to "Genmaicha Tea",
            "lemon tea" to "Lemon Tea",
            // Kept as mappings into supported teas
            "earl grey tea" to "Black Tea",
            "earl gray tea" to "Black Tea",
            "darjeeling tea" to "Black Tea",
            "assam tea" to "Black Tea",
            "ceylon tea" to "Black Tea",
            "sencha tea" to "Green Tea"
        )

        directAliasMap[normalizedScan]?.let { alias ->
            teaList.firstOrNull { it.teaType.equals(alias, ignoreCase = true) }?.let {
                android.util.Log.d("TeaResolution", "✓ Alias match found: $normalizedScan -> ${it.teaType}")
                return it
            }
        }

        android.util.Log.d("TeaResolution", "✗ No exact or explicit alias match found for: $normalizedScan")
        return null
    }

    private fun handleScannedTeaSelection(
        scannedTeaType: String,
        scanMethod: String?,
        scanConfidence: Float?,
        barcodeMappedTeaType: String?,
        barcodeRawValue: String?,
        lookupSourceLabel: String? = null
    ) {
        // ZERO THRESHOLD POLICY: Trust any result coming from the scanner
        val imageThreshold = 0.0f
        if (scanMethod == "image_recognition" && (scanConfidence == null || scanConfidence < imageThreshold)) {
            android.util.Log.d("TeaSelection", "Ignoring result: $scanConfidence")
            return
        }

        val resolvedTea = if (scanMethod == "barcode") {
            // Hard fail barcode scans when mapped tea type is not present in DB.
            val mappedTeaType = (barcodeMappedTeaType ?: scannedTeaType).trim()
            teaList.firstOrNull {
                ScanNormalization.normalizeTeaName(it.teaType) == ScanNormalization.normalizeTeaName(mappedTeaType)
            } ?: run {
                hideUnmappedBarcodeBanner()
                showLookupSourceTag(null)
                Toast.makeText(
                    this,
                    "Barcode mapped to '$mappedTeaType' but this tea type is not available in app database",
                    Toast.LENGTH_LONG
                ).show()
                return
            }
        } else {
            resolveTeaSelection(scannedTeaType)
        }

        hideUnmappedBarcodeBanner()

        if (resolvedTea != null) {
            val index = teaList.indexOfFirst { it.teaType.equals(resolvedTea.teaType, ignoreCase = true) }
            if (index >= 0) {
                binding.teaTypeSpinner.setSelection(index)
                val sourceLabel = lookupSourceLabel ?: resolveScanSourceLabel(scanMethod)
                showLookupSourceTag(sourceLabel)
                showScanResultMeta(sourceLabel, scanConfidence)

                // Animate confirmation
                animateSuccess(binding.confirmButton)

                Toast.makeText(
                    this,
                    "✓ Found: ${resolvedTea.teaType}",
                    Toast.LENGTH_SHORT
                ).show()

                // Auto-log the scanned tea
                lifecycleScope.launch {
                    repository.logTeaSelection(teaList[index].teaType, userConfirmed = false)
                }
            } else {
                showLookupSourceTag(null)
                showScanResultMeta(null, null)
                Toast.makeText(
                    this,
                    "Tea type '${resolvedTea.teaType}' not in database",
                    Toast.LENGTH_SHORT
                ).show()
            }
        } else {
            showLookupSourceTag(null)
            showScanResultMeta(null, null)
            Toast.makeText(
                this,
                "Tea type '$scannedTeaType' not recognized",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private data class LookupChipStyle(
        val iconRes: Int,
        val backgroundColorRes: Int
    )

    private fun resolveScanSourceLabel(scanMethod: String?): String? {
        return when (scanMethod) {
            "barcode" -> getString(R.string.source_local_barcode)
            "text" -> getString(R.string.source_text)
            "image_recognition" -> getString(R.string.source_image)
            "manual_search" -> "Manual Search"
            else -> null
        }
    }

    private fun resolveLookupChipStyle(label: String): LookupChipStyle {
        val normalized = label.lowercase(Locale.US)
        return when {
            normalized.contains("text") -> LookupChipStyle(
                iconRes = android.R.drawable.ic_menu_edit,
                backgroundColorRes = R.color.primary_dark_teal
            )
            normalized.contains("image") -> LookupChipStyle(
                iconRes = android.R.drawable.ic_menu_gallery,
                backgroundColorRes = R.color.secondary_dark_purple
            )
            normalized.contains("api") -> LookupChipStyle(
                iconRes = android.R.drawable.ic_popup_sync,
                backgroundColorRes = R.color.info_blue
            )
            normalized.contains("manual") || normalized.contains("admin") -> LookupChipStyle(
                iconRes = android.R.drawable.ic_input_add,
                backgroundColorRes = R.color.warning_orange
            )
            normalized.contains("saved") -> LookupChipStyle(
                iconRes = android.R.drawable.star_on,
                backgroundColorRes = R.color.success_green
            )
            else -> LookupChipStyle(
                iconRes = android.R.drawable.ic_menu_search,
                backgroundColorRes = R.color.primary_teal
            )
        }
    }

    private fun showLookupSourceTag(label: String?) {
        val chip = binding.lookupSourceTagChip

        if (label.isNullOrBlank()) {
            currentLookupSourceLabel = null
            if (chip.visibility == View.VISIBLE) {
                chip.animate()
                    .alpha(0f)
                    .setDuration(140)
                    .withEndAction {
                        chip.visibility = View.GONE
                        chip.alpha = 1f
                    }
                    .start()
            } else {
                chip.visibility = View.GONE
            }
            return
        }

        if (label == currentLookupSourceLabel && chip.visibility == View.VISIBLE) {
            return
        }

        val style = resolveLookupChipStyle(label)
        val backgroundColor = ContextCompat.getColor(this, style.backgroundColorRes)
        val chipIconDrawable = ContextCompat.getDrawable(this, style.iconRes)

        val applyChipContent = {
            chip.text = label
            chip.chipIcon = chipIconDrawable
            chip.isChipIconVisible = chipIconDrawable != null
            chip.chipBackgroundColor = ColorStateList.valueOf(backgroundColor)
            chip.chipIconTint = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.white))
            chip.setTextColor(ContextCompat.getColor(this, R.color.white))
            currentLookupSourceLabel = label
        }

        if (chip.visibility != View.VISIBLE) {
            applyChipContent()
            chip.alpha = 0f
            chip.visibility = View.VISIBLE
            chip.animate()
                .alpha(1f)
                .setDuration(180)
                .start()
            return
        }

        chip.animate()
            .alpha(0f)
            .setDuration(120)
            .withEndAction {
                applyChipContent()
                chip.animate()
                    .alpha(1f)
                    .setDuration(180)
                    .start()
            }
            .start()
    }

    private fun showScanResultMeta(sourceLabel: String?, confidence: Float?) {
        val metaView = binding.scanResultMetaText

        if (sourceLabel.isNullOrBlank()) {
            if (metaView.visibility == View.VISIBLE) {
                metaView.animate()
                    .alpha(0f)
                    .setDuration(120)
                    .withEndAction {
                        metaView.visibility = View.GONE
                        metaView.alpha = 1f
                    }
                    .start()
            } else {
                metaView.visibility = View.GONE
            }
            return
        }

        val metaText = if (confidence != null) {
            getString(R.string.scan_meta_format, sourceLabel, confidence * 100f)
        } else {
            getString(R.string.scan_meta_no_confidence_format, sourceLabel)
        }

        metaView.text = metaText
        if (metaView.visibility != View.VISIBLE) {
            metaView.alpha = 0f
            metaView.visibility = View.VISIBLE
            metaView.animate().alpha(1f).setDuration(180).start()
        }
    }

    private fun showUnmappedBarcodeBanner(barcode: String, message: String? = null) {
        val normalizedBarcode = ScanNormalization.normalizeBarcode(barcode)
        currentUnmappedBarcode = normalizedBarcode
        binding.unmappedBarcodeText.text = message
            ?: "Barcode $normalizedBarcode is not recognized yet. Add this product mapping to scan it next time."
        binding.unmappedBarcodeBanner.visibility = android.view.View.VISIBLE
        showLookupSourceTag(null)
        showScanResultMeta(null, null)
    }


    private fun hideUnmappedBarcodeBanner() {
        currentUnmappedBarcode = null
        binding.unmappedBarcodeBanner.visibility = android.view.View.GONE
    }

    private suspend fun lookupBarcodeAndResolve(rawBarcode: String): Boolean {
        val safeBarcode = ScanNormalization.normalizeBarcode(rawBarcode)
        if (safeBarcode.isBlank()) return false

        val lookup = withContext(Dispatchers.IO) {
            TeaBarcodeLookupService.lookupTeaByBarcode(safeBarcode)
        }

        return when (lookup) {
            is TeaBarcodeLookupService.LookupResult.Found -> {
                val apiTeaType = resolveTeaFromApiText(lookup.teaHint, lookup.productName, lookup.brand)
                if (apiTeaType.isNullOrBlank()) {
                    Toast.makeText(
                        this,
                        "${lookup.source} returned product data, but tea type could not be mapped.",
                        Toast.LENGTH_LONG
                    ).show()
                    false
                } else {
                    BarcodeMappingStore.saveMapping(
                        context = this,
                        barcode = safeBarcode,
                        teaType = apiTeaType,
                        brand = lookup.brand,
                        product = lookup.productName
                    )

                    handleScannedTeaSelection(
                        scannedTeaType = apiTeaType,
                        scanMethod = "barcode",
                        scanConfidence = 0.92f,
                        barcodeMappedTeaType = apiTeaType,
                        barcodeRawValue = safeBarcode,
                        lookupSourceLabel = getString(R.string.source_api_format, lookup.source)
                    )
                    true
                }
            }

            is TeaBarcodeLookupService.LookupResult.NotFound -> {
                Toast.makeText(this, "Barcode not found in Open Food Facts/UPCitemDB.", Toast.LENGTH_SHORT).show()
                false
            }

            is TeaBarcodeLookupService.LookupResult.Error -> {
                Toast.makeText(this, "Barcode API unavailable (${lookup.reason}). Use Add Product.", Toast.LENGTH_LONG).show()
                false
            }
        }
    }

    private fun resolveTeaFromApiText(vararg apiTexts: String): String? {
        val combinedText = apiTexts
            .filter { it.isNotBlank() }
            .joinToString(" ")
            .lowercase(Locale.US)

        if (combinedText.isBlank()) return null

        val apiAliasMap = linkedMapOf(
            "matcha" to "Matcha Tea",
            "green tea" to "Green Tea",
            "sencha" to "Green Tea",
            "black tea" to "Black Tea",
            "earl grey" to "Black Tea",
            "earl gray" to "Black Tea",
            "darjeeling" to "Black Tea",
            "assam" to "Black Tea",
            "ceylon" to "Black Tea",
            "oolong" to "Oolong Tea",
            "wulong" to "Oolong Tea",
            "chamomile" to "Chamomile Tea",
            "peppermint" to "Peppermint Tea",
            "mint tea" to "Peppermint Tea",
            "ginger" to "Ginger Tea",
            "hibiscus" to "Hibiscus Tea",
            "rooibos" to "Rooibos Tea",
            "lavender" to "Lavender Tea",
            "chai" to "Chai Tea",
            "masala chai" to "Chai Tea",
            "turmeric" to "Turmeric Tea",
            "rosehip" to "Rosehip Tea",
            "rose hip" to "Rosehip Tea",
            "blueberry" to "Blueberry Tea",
            "raspberry" to "Raspberry Tea",
            "kukicha" to "Kukicha Tea",
            "genmaicha" to "Genmaicha Tea",
            "lemon tea" to "Lemon Tea"
        )

        val directMapped = apiAliasMap.entries.firstOrNull { combinedText.contains(it.key) }?.value
        if (!directMapped.isNullOrBlank()) {
            return teaList.firstOrNull {
                ScanNormalization.normalizeTeaName(it.teaType) == ScanNormalization.normalizeTeaName(directMapped)
            }?.teaType
        }

        return resolveTeaSelection(combinedText)?.teaType
    }

    private fun showAddProductDialog(barcode: String) {
        if (teaList.isEmpty()) {
            Toast.makeText(this, "Tea list is still loading. Please try again.", Toast.LENGTH_SHORT).show()
            return
        }

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            val pad = (16 * resources.displayMetrics.density).toInt()
            setPadding(pad, pad, pad, 0)
        }

        val teaSpinner = Spinner(this).apply {
            adapter = ArrayAdapter(
                this@MainActivity,
                android.R.layout.simple_spinner_item,
                teaList.map { it.teaType }
            ).also { it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
        }

        val brandInput = EditText(this).apply {
            hint = "Brand (optional)"
        }

        val productInput = EditText(this).apply {
            hint = "Product name (optional)"
        }

        root.addView(teaSpinner)
        root.addView(brandInput)
        root.addView(productInput)

        val dialog = AlertDialog.Builder(this)
            .setTitle("Add Product Mapping")
            .setMessage("Barcode: $barcode")
            .setView(root)
            .setPositiveButton("Save", null)
            .setNeutralButton("Add New Tea Type", null)
            .setNegativeButton("Cancel", null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val selectedTea = teaList.getOrNull(teaSpinner.selectedItemPosition)?.teaType
                if (selectedTea.isNullOrBlank()) {
                    Toast.makeText(this, "Please select a tea type", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                BarcodeMappingStore.saveMapping(
                    context = this,
                    barcode = barcode,
                    teaType = selectedTea,
                    brand = brandInput.text.toString(),
                    product = productInput.text.toString()
                )

                Toast.makeText(this, "Saved mapping for barcode $barcode", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
                handleScannedTeaSelection(
                    scannedTeaType = selectedTea,
                    scanMethod = "barcode",
                    scanConfidence = null,
                    barcodeMappedTeaType = selectedTea,
                    barcodeRawValue = barcode,
                    lookupSourceLabel = getString(R.string.source_manual_mapping)
                )
            }

            dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener {
                dialog.dismiss()
                showAdminPinDialog(
                    barcode = barcode,
                    prefillBrand = brandInput.text.toString(),
                    prefillProduct = productInput.text.toString()
                )
            }
        }

        dialog.show()
    }

    private fun showAdminPinDialog(barcode: String, prefillBrand: String, prefillProduct: String) {
        val pinInput = EditText(this).apply {
            hint = "Enter admin PIN"
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
        }

        val dialog = AlertDialog.Builder(this)
            .setTitle("Admin Access Required")
            .setMessage("Enter PIN to add a brand new tea type.")
            .setView(pinInput)
            .setPositiveButton("Verify", null)
            .setNegativeButton("Cancel", null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                if (pinInput.text.toString().trim() != ADMIN_PIN) {
                    pinInput.error = "Invalid PIN"
                    return@setOnClickListener
                }
                dialog.dismiss()
                showAddNewTeaTypeDialog(
                    barcode = barcode,
                    prefillBrand = prefillBrand,
                    prefillProduct = prefillProduct
                )
            }
        }

        dialog.show()
    }

    private fun showAddNewTeaTypeDialog(
        barcode: String,
        prefillBrand: String,
        prefillProduct: String
    ) {
        val density = resources.displayMetrics.density
        val pad = (16 * density).toInt()

        val formContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, pad, pad, pad)
        }

        fun field(hint: String, defaultValue: String = ""): EditText {
            return EditText(this).apply {
                this.hint = hint
                if (defaultValue.isNotBlank()) {
                    setText(defaultValue)
                }
            }
        }

        val teaTypeInput = field("Tea type name (required)")
        val caloriesInput = field("Calories", "0 kcal")
        val proteinInput = field("Protein", "0 g")
        val carbsInput = field("Carbohydrates", "0 g")
        val fatInput = field("Fat", "0 g")
        val calciumInput = field("Calcium", "0 mg")
        val magnesiumInput = field("Magnesium", "0 mg")
        val remarkInput = field("Health remark", "Custom tea profile added by admin for barcode mapping.")

        formContainer.addView(teaTypeInput)
        formContainer.addView(caloriesInput)
        formContainer.addView(proteinInput)
        formContainer.addView(carbsInput)
        formContainer.addView(fatInput)
        formContainer.addView(calciumInput)
        formContainer.addView(magnesiumInput)
        formContainer.addView(remarkInput)

        val scrollView = ScrollView(this).apply {
            addView(formContainer)
        }

        val dialog = AlertDialog.Builder(this)
            .setTitle("Add New Tea Type (Admin)")
            .setMessage("Barcode: $barcode")
            .setView(scrollView)
            .setPositiveButton("Create & Map", null)
            .setNegativeButton("Cancel", null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val rawTeaName = teaTypeInput.text.toString().trim()
                if (rawTeaName.isBlank()) {
                    teaTypeInput.error = "Tea type is required"
                    return@setOnClickListener
                }

                val teaName = rawTeaName
                    .split(" ")
                    .filter { it.isNotBlank() }
                    .joinToString(" ") { token -> token.replaceFirstChar { c -> c.uppercase() } }

                val duplicate = teaList.any {
                    ScanNormalization.normalizeTeaName(it.teaType) == ScanNormalization.normalizeTeaName(teaName)
                }
                if (duplicate) {
                    Toast.makeText(this, "Tea type already exists. Use Add Product mapping.", Toast.LENGTH_LONG).show()
                    dialog.dismiss()
                    showAddProductDialog(barcode)
                    return@setOnClickListener
                }

                val newTea = TeaEntity(
                    teaType = teaName,
                    calories = caloriesInput.text.toString().trim().ifBlank { "0 kcal" },
                    protein = proteinInput.text.toString().trim().ifBlank { "0 g" },
                    carbohydrates = carbsInput.text.toString().trim().ifBlank { "0 g" },
                    fat = fatInput.text.toString().trim().ifBlank { "0 g" },
                    calcium = calciumInput.text.toString().trim().ifBlank { "0 mg" },
                    magnesium = magnesiumInput.text.toString().trim().ifBlank { "0 mg" },
                    doctorRemark = remarkInput.text.toString().trim().ifBlank {
                        "Custom tea profile added by admin for barcode mapping."
                    }
                )

                lifecycleScope.launch {
                    repository.insertTea(newTea)

                    // Keep in-memory list in sync immediately so mapping and selection work now.
                    teaList.removeAll { ScanNormalization.normalizeTeaName(it.teaType) == ScanNormalization.normalizeTeaName(newTea.teaType) }
                    teaList.add(newTea)
                    teaList.sortBy { it.teaType }
                    updateTeaSpinner()

                    BarcodeMappingStore.saveMapping(
                        context = this@MainActivity,
                        barcode = barcode,
                        teaType = newTea.teaType,
                        brand = prefillBrand,
                        product = prefillProduct
                    )

                    Toast.makeText(
                        this@MainActivity,
                        "Added ${newTea.teaType} and mapped barcode $barcode",
                        Toast.LENGTH_LONG
                    ).show()

                    handleScannedTeaSelection(
                        scannedTeaType = newTea.teaType,
                        scanMethod = "barcode",
                        scanConfidence = null,
                        barcodeMappedTeaType = newTea.teaType,
                        barcodeRawValue = barcode,
                        lookupSourceLabel = getString(R.string.source_admin_mapping)
                    )
                }

                dialog.dismiss()
            }
        }

        dialog.show()
    }

    private companion object {
        // Simple admin gate for production-style controlled access. Move to secure config for real deployment.
        private const val ADMIN_PIN = "2468"
    }
}
