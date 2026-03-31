package com.example.fbuddy.ui.analytics

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.fbuddy.data.model.Category
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter

@Composable
fun AnalyticsScreen(
    modifier: Modifier = Modifier,
    viewModel: AnalyticsViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()

    if (state.isLoading) {
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        // Month comparison card
        MonthComparisonCard(
            thisMonth = state.thisMonthTotal,
            lastMonth = state.lastMonthTotal
        )

        // Pie chart – spending by category this month
        if (state.categoryTotalsMonth.isNotEmpty()) {
            CategoryPieChartCard(categoryTotals = state.categoryTotalsMonth)
        }

        // Bar chart – daily spend last 30 days
        if (state.dailyTotals30Days.isNotEmpty()) {
            DailyBarChartCard(dailyTotals = state.dailyTotals30Days)
        }

        // Line/Bar chart – monthly totals last 6 months
        if (state.monthlyTotals6Months.isNotEmpty()) {
            MonthlyBarChartCard(monthlyBars = state.monthlyTotals6Months)
        }

        // Top merchants
        if (state.topMerchants.isNotEmpty()) {
            TopMerchantsCard(merchants = state.topMerchants)
        }
    }
}

@Composable
private fun MonthComparisonCard(thisMonth: Double, lastMonth: Double) {
    val diff = thisMonth - lastMonth
    val trend = when {
        diff > 0 -> "↑ ₹%.2f more than last month".format(diff)
        diff < 0 -> "↓ ₹%.2f less than last month".format(-diff)
        else -> "Same as last month"
    }
    val trendColor = if (diff > 0) Color(0xFFD32F2F) else Color(0xFF388E3C)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("This Month", style = MaterialTheme.typography.titleMedium)
            Text(
                "₹%.2f".format(thisMonth),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 4.dp)
            )
            Text(trend, style = MaterialTheme.typography.bodySmall, color = trendColor,
                modifier = Modifier.padding(top = 4.dp))
            Text(
                "Last month: ₹%.2f".format(lastMonth),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}

@Composable
private fun CategoryPieChartCard(categoryTotals: Map<Category, Double>) {
    val colors = listOf(
        Color(0xFF4CAF50), Color(0xFF2196F3), Color(0xFFFF9800), Color(0xFF9C27B0),
        Color(0xFFF44336), Color(0xFF00BCD4), Color(0xFF8BC34A), Color(0xFFFF5722),
        Color(0xFF607D8B), Color(0xFF795548)
    )

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Spending by Category", style = MaterialTheme.typography.titleMedium)
            Text("This month", style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline)
            Spacer(Modifier.height(8.dp))

            AndroidView(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp),
                factory = { ctx ->
                    PieChart(ctx).apply {
                        description.isEnabled = false
                        isDrawHoleEnabled = true
                        holeRadius = 45f
                        setDrawEntryLabels(false)
                        legend.isEnabled = true
                        legend.textSize = 10f
                    }
                },
                update = { chart ->
                    val entries = categoryTotals.entries.mapIndexed { i, (cat, amount) ->
                        PieEntry(amount.toFloat(), cat.displayName)
                    }
                    val dataSet = PieDataSet(entries, "").apply {
                        this.colors = colors.take(entries.size).map { it.toArgb() }
                        sliceSpace = 2f
                        valueTextSize = 0f
                    }
                    chart.data = PieData(dataSet)
                    chart.invalidate()
                }
            )

            // Legend list below chart
            Spacer(Modifier.height(8.dp))
            categoryTotals.entries.sortedByDescending { it.value }.forEach { (cat, amount) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(cat.displayName, style = MaterialTheme.typography.bodySmall)
                    Text("₹%.2f".format(amount), style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun DailyBarChartCard(dailyTotals: List<Double>) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Daily Spending", style = MaterialTheme.typography.titleMedium)
            Text("Last 30 days", style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline)
            Spacer(Modifier.height(8.dp))

            AndroidView(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                factory = { ctx ->
                    BarChart(ctx).apply {
                        description.isEnabled = false
                        legend.isEnabled = false
                        setPinchZoom(false)
                        setScaleEnabled(false)
                        axisRight.isEnabled = false
                        axisLeft.axisMinimum = 0f
                        axisLeft.textSize = 9f
                        xAxis.position = XAxis.XAxisPosition.BOTTOM
                        xAxis.setDrawGridLines(false)
                        xAxis.granularity = 5f
                        xAxis.textSize = 9f
                        xAxis.labelRotationAngle = -45f
                        setDrawValueAboveBar(false)
                    }
                },
                update = { chart ->
                    val entries = dailyTotals.mapIndexed { i, v ->
                        BarEntry(i.toFloat(), v.toFloat())
                    }
                    // Label every 5th day
                    val labels = (0 until dailyTotals.size).map { i ->
                        if (i % 5 == 0) "D-${dailyTotals.size - 1 - i}" else ""
                    }
                    chart.xAxis.valueFormatter = IndexAxisValueFormatter(labels)

                    val dataSet = BarDataSet(entries, "").apply {
                        color = android.graphics.Color.parseColor("#2196F3")
                        setDrawValues(false)
                    }
                    chart.data = BarData(dataSet).apply { barWidth = 0.8f }
                    chart.invalidate()
                }
            )
        }
    }
}

@Composable
private fun MonthlyBarChartCard(monthlyBars: List<MonthlyBar>) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Monthly Trend", style = MaterialTheme.typography.titleMedium)
            Text("Last 6 months", style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline)
            Spacer(Modifier.height(8.dp))

            AndroidView(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                factory = { ctx ->
                    BarChart(ctx).apply {
                        description.isEnabled = false
                        legend.isEnabled = false
                        setPinchZoom(false)
                        setScaleEnabled(false)
                        axisRight.isEnabled = false
                        axisLeft.axisMinimum = 0f
                        xAxis.position = XAxis.XAxisPosition.BOTTOM
                        xAxis.setDrawGridLines(false)
                        xAxis.granularity = 1f
                        setDrawValueAboveBar(true)
                    }
                },
                update = { chart ->
                    val entries = monthlyBars.mapIndexed { i, bar ->
                        BarEntry(i.toFloat(), bar.total.toFloat())
                    }
                    val labels = monthlyBars.map { it.label }
                    chart.xAxis.valueFormatter = IndexAxisValueFormatter(labels)

                    val dataSet = BarDataSet(entries, "").apply {
                        color = android.graphics.Color.parseColor("#9C27B0")
                        valueTextSize = 9f
                    }
                    chart.data = BarData(dataSet).apply { barWidth = 0.6f }
                    chart.invalidate()
                }
            )
        }
    }
}

@Composable
private fun TopMerchantsCard(merchants: List<Pair<String, Double>>) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Top Merchants", style = MaterialTheme.typography.titleMedium)
            Text("Last 30 days", style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline)
            Spacer(Modifier.height(8.dp))

            merchants.forEachIndexed { index, (merchant, amount) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "${index + 1}",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.width(24.dp)
                        )
                        Text(merchant, style = MaterialTheme.typography.bodyMedium)
                    }
                    Text(
                        "₹%.2f".format(amount),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFD32F2F)
                    )
                }
                if (index < merchants.size - 1) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))
                }
            }
        }
    }
}
