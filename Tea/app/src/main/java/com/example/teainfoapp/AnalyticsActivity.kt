package com.example.teainfoapp

import android.graphics.Color
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.teainfoapp.data.TeaDatabase
import com.example.teainfoapp.data.TeaRepository
import com.example.teainfoapp.databinding.ActivityAnalyticsBinding
import com.github.mikephil.charting.animation.Easing
import com.github.mikephil.charting.utils.MPPointF
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.PercentFormatter
import com.github.mikephil.charting.utils.ColorTemplate
import kotlinx.coroutines.launch
import java.util.*

/**
 * Analytics Activity - Interactive Dashboard
 * Displays tea consumption patterns with interactive charts and graphs
 */
class AnalyticsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAnalyticsBinding
    private lateinit var repository: TeaRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAnalyticsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initializeDatabase()
        setupCharts()
        setupMicroAnimations()
        animateCardsOnEntry()
        loadAnalyticsData()
    }

    private fun initializeDatabase() {
        val db = TeaDatabase.getDatabase(this)
        repository = TeaRepository(db.teaDao(), db.logDao())
    }

    private fun setupCharts() {
        setupPieChart()
        setupBarChart()
        setupLineChart()
    }

    private fun setupPieChart() {
        binding.teaDistributionPieChart.apply {
            description.isEnabled = false
            setUsePercentValues(true)
            setEntryLabelColor(Color.WHITE)
            setEntryLabelTextSize(12f)
            holeRadius = 40f
            transparentCircleRadius = 45f
            isRotationEnabled = true
            setExtraOffsets(5f, 10f, 5f, 5f)
            legend.apply {
                isEnabled = true
                textSize = 12f
            }
            animateY(1400, Easing.EaseInOutQuad)
        }
    }

    private fun setupBarChart() {
        binding.weeklyConsumptionBarChart.apply {
            description.isEnabled = false
            setDrawGridBackground(false)
            setTouchEnabled(true)
            isDragEnabled = true
            setScaleEnabled(true)
            legend.apply {
                isEnabled = true
                textSize = 12f
            }
            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                setDrawGridLines(false)
                textSize = 12f
            }
            axisLeft.apply {
                textSize = 12f
                setDrawGridLines(true)
            }
            axisRight.isEnabled = false
            animateY(1400, Easing.EaseInOutQuad)
        }
    }

    private fun setupLineChart() {
        binding.frequencyTrendLineChart.apply {
            description.isEnabled = false
            setDrawGridBackground(false)
            setTouchEnabled(true)
            isDragEnabled = true
            setScaleEnabled(true)
            legend.apply {
                isEnabled = true
                textSize = 12f
            }
            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                setDrawGridLines(false)
                textSize = 12f
            }
            axisLeft.apply {
                textSize = 12f
                setDrawGridLines(true)
            }
            axisRight.isEnabled = false
            animateY(1400, Easing.EaseInOutQuad)
        }
    }

    private fun setupMicroAnimations() {
        binding.analyticsScrollView.setOnScrollChangeListener { _, _, scrollY, _, _ ->
            binding.analyticsHeroHeader.translationY = scrollY * 0.2f
        }
    }

    private fun animateCardsOnEntry() {
        val cards = listOf(
            binding.distributionCard,
            binding.weeklyCard,
            binding.trendCard,
            binding.statisticsCard
        )

        cards.forEachIndexed { index, card ->
            card.alpha = 0f
            card.translationY = 36f
            card.animate()
                .alpha(1f)
                .translationY(0f)
                .setStartDelay((index * 90).toLong())
                .setDuration(320)
                .start()
        }
    }

    private fun loadAnalyticsData() {
        lifecycleScope.launch {
            val logs = repository.getAllLogs()

            if (logs.isEmpty()) {
                binding.apply {
                    totalScansValue.text = getString(R.string.zero)
                    confirmedScansValue.text = getString(R.string.zero)
                    uniqueTypesValue.text = getString(R.string.zero)
                    favoriteTeaValue.text = getString(R.string.dash)
                }
                return@launch
            }

            // Calculate statistics
            val totalScans = logs.size
            val confirmedScans = logs.count { it.userConfirmed }
            val uniqueTypes = logs.map { it.teaType }.distinct().size
            val favoriteType = logs.groupingBy { it.teaType }
                .eachCount()
                .maxByOrNull { it.value }?.key ?: "—"

            // Update statistics display with animation
            animateStatValue(binding.totalScansValue, totalScans)
            animateStatValue(binding.confirmedScansValue, confirmedScans)
            animateStatValue(binding.uniqueTypesValue, uniqueTypes)
            binding.favoriteTeaValue.text = favoriteType

            // Prepare data for charts
            val teaDistribution = logs.groupingBy { it.teaType }
                .eachCount()

            val daysOfWeek = arrayOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
            val calendar = Calendar.getInstance()
            val weekData = daysOfWeek.mapIndexed { index, _ ->
                val dayOfWeek = when (index) {
                    0 -> Calendar.MONDAY
                    1 -> Calendar.TUESDAY
                    2 -> Calendar.WEDNESDAY
                    3 -> Calendar.THURSDAY
                    4 -> Calendar.FRIDAY
                    5 -> Calendar.SATURDAY
                    else -> Calendar.SUNDAY
                }
                val count = logs.count { log ->
                    val logCal = Calendar.getInstance().apply { timeInMillis = log.timestamp }
                    logCal.get(Calendar.DAY_OF_WEEK) == dayOfWeek
                }
                count.toFloat()
            }

            // Load pie chart with tea distribution
            loadPieChart(teaDistribution)

            // Load bar chart with weekly data
            loadBarChart(daysOfWeek, weekData)

            // Load line chart with frequency trend
            loadLineChart(daysOfWeek, weekData)
        }
    }

    private fun loadPieChart(teaDistribution: Map<String, Int>) {
        val entries = teaDistribution.entries.mapIndexed { index, (teaType, count) ->
            PieEntry(count.toFloat(), teaType)
        }

        val colors = mutableListOf<Int>()
        colors.addAll(ColorTemplate.MATERIAL_COLORS.toList())
        colors.addAll(ColorTemplate.PASTEL_COLORS.toList())

        val dataSet = PieDataSet(entries, "Tea Distribution").apply {
            setColors(colors)
            sliceSpace = 3f
            iconsOffset = MPPointF(0f, 20f)
            selectionShift = 5f
            valueLineColor = Color.WHITE
            valueTextSize = 11f
            valueFormatter = PercentFormatter()
        }

        val data = PieData(dataSet).apply {
            setValueTextColor(Color.WHITE)
            setValueTextSize(11f)
        }

        binding.teaDistributionPieChart.apply {
            this.data = data
            highlightValue(null)
            invalidate()
        }
    }

    private fun loadBarChart(labels: Array<String>, values: List<Float>) {
        val entries = values.mapIndexed { index, value ->
            BarEntry(index.toFloat(), value)
        }

        val dataSet = BarDataSet(entries, "Weekly Scans").apply {
            color = resources.getColor(R.color.primary_teal, theme)
            valueTextColor = Color.WHITE
            valueTextSize = 12f
            setDrawValues(true)
        }

        val data = BarData(dataSet).apply {
            barWidth = 0.6f
        }

        binding.weeklyConsumptionBarChart.apply {
            xAxis.valueFormatter = IndexAxisValueFormatter(labels)
            this.data = data
            invalidate()
        }
    }

    private fun loadLineChart(labels: Array<String>, values: List<Float>) {
        val entries = values.mapIndexed { index, value ->
            Entry(index.toFloat(), value)
        }

        val dataSet = LineDataSet(entries, "Scan Frequency").apply {
            color = resources.getColor(R.color.success_green, theme)
            setCircleColor(resources.getColor(R.color.success_green, theme))
            lineWidth = 3f
            circleRadius = 5f
            setDrawCircleHole(false)
            valueTextColor = Color.BLACK
            valueTextSize = 11f
            setDrawValues(true)
            mode = LineDataSet.Mode.CUBIC_BEZIER
            cubicIntensity = 0.2f
        }

        val data = LineData(dataSet)

        binding.frequencyTrendLineChart.apply {
            xAxis.valueFormatter = IndexAxisValueFormatter(labels)
            this.data = data
            invalidate()
        }
    }

    private fun animateStatValue(textView: android.widget.TextView, finalValue: Int) {
        val duration = 1000L
        val startValue = 0
        val startTime = System.currentTimeMillis()

        val updateRunnable = object : Runnable {
            override fun run() {
                val elapsed = System.currentTimeMillis() - startTime
                val progress = (elapsed.toFloat() / duration).coerceIn(0f, 1f)
                val currentValue = (startValue + (finalValue - startValue) * progress).toInt()

                textView.text = currentValue.toString()

                if (progress < 1f) {
                    textView.postDelayed(this, 16)
                }
            }
        }

        textView.post(updateRunnable)
    }
}
