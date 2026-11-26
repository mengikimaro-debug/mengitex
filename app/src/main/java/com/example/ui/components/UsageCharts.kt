package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.ui.theme.android17AvatarColors
import kotlin.math.max
import kotlin.math.roundToInt

data class DailySmsChartPoint(
    val dayLabel: String,
    val fullDateLabel: String,
    val sentCount: Int,
    val receivedCount: Int
) {
    val totalCount: Int get() = sentCount + receivedCount
}

data class ActiveContactStat(
    val displayName: String,
    val phoneNumber: String,
    val sentCount: Int,
    val receivedCount: Int
) {
    val totalCount: Int get() = sentCount + receivedCount
}

data class SavingsDonutSlice(
    val label: String,
    val subtitle: String,
    val valueText: String,
    val weight: Float,
    val color: Color
)

/**
 * Recharts-inspired interactive Area / Grouped Bar chart rendered natively in Jetpack Compose.
 * Features dashed CartesianGrid lines, smooth cubic Bézier area gradients, grouped bars,
 * and tap/drag-to-inspect floating tooltip readout.
 */
@Composable
fun DailyUsageAreaBarChart(
    points: List<DailySmsChartPoint>,
    isBarMode: Boolean,
    sentLabel: String,
    receivedLabel: String,
    totalLabel: String,
    sentColor: Color,
    receivedColor: Color,
    modifier: Modifier = Modifier
) {
    if (points.isEmpty()) return

    var selectedIndex by remember(points) {
        mutableIntStateOf(
            points.indices.maxByOrNull { points[it].totalCount } ?: (points.lastIndex)
        )
    }

    val reveal = remember(points, isBarMode) { Animatable(0f) }
    LaunchedEffect(points, isBarMode) {
        reveal.snapTo(0f)
        reveal.animateTo(1f, tween(durationMillis = 520, easing = FastOutSlowInEasing))
    }

    val maxValue = remember(points) {
        val peak = points.maxOfOrNull { max(it.sentCount, it.receivedCount) } ?: 0
        when {
            peak <= 4 -> 4
            peak <= 10 -> ((peak + 1) / 2) * 2
            else -> ((peak + 4) / 5) * 5
        }
    }

    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val selectionBandColor = MaterialTheme.colorScheme.surfaceContainerHigh
    val axisTextColor = MaterialTheme.colorScheme.onSurfaceVariant
    val activePoint = points.getOrNull(selectedIndex.coerceIn(0, points.lastIndex))

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (activePoint != null) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("dashboard_daily_tooltip")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 9.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = activePoint.fullDateLabel,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "$totalLabel: ${activePoint.totalCount}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        ChartLegendValue(color = sentColor, label = sentLabel, value = activePoint.sentCount.toString())
                        ChartLegendValue(color = receivedColor, label = receivedLabel, value = activePoint.receivedCount.toString())
                    }
                }
            }
        }

        // Main chart plot area + Y-axis labels
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(196.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Y-Axis ticks (4 steps)
            Column(
                modifier = Modifier
                    .width(28.dp)
                    .fillMaxHeight()
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.End
            ) {
                val steps = listOf(maxValue, (maxValue * 0.66f).roundToInt(), (maxValue * 0.33f).roundToInt(), 0)
                steps.forEach { tick ->
                    Text(
                        text = tick.toString(),
                        style = MaterialTheme.typography.labelSmall,
                        color = axisTextColor
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Interactive Canvas
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .pointerInput(points) {
                        detectTapGestures { offset ->
                            val count = points.size
                            if (count > 0 && size.width > 0) {
                                val ratio = (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
                                selectedIndex = (ratio * (count - 1).coerceAtLeast(1)).roundToInt()
                                    .coerceIn(0, count - 1)
                            }
                        }
                    }
                    .pointerInput(points) {
                        detectHorizontalDragGestures { change, _ ->
                            val count = points.size
                            if (count > 0 && size.width > 0) {
                                val ratio = (change.position.x / size.width.toFloat()).coerceIn(0f, 1f)
                                selectedIndex = (ratio * (count - 1).coerceAtLeast(1)).roundToInt()
                                    .coerceIn(0, count - 1)
                            }
                        }
                    }
                    .testTag("dashboard_daily_chart_canvas")
            ) {
                val progress = reveal.value
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val topPad = 12.dp.toPx()
                    val bottomPad = 10.dp.toPx()
                    val plotHeight = (size.height - topPad - bottomPad).coerceAtLeast(1f)
                    val plotWidth = size.width.coerceAtLeast(1f)

                    // Dashed horizontal CartesianGrid lines (signature Recharts style)
                    val dashEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
                    for (i in 0..3) {
                        val y = topPad + (plotHeight * i / 3f)
                        drawLine(
                            color = gridColor,
                            start = Offset(0f, y),
                            end = Offset(plotWidth, y),
                            strokeWidth = 1.dp.toPx(),
                            pathEffect = if (i == 3) null else dashEffect
                        )
                    }

                    val count = points.size
                    if (isBarMode) {
                        val slotWidth = plotWidth / count.coerceAtLeast(1)
                        val groupWidth = (slotWidth * 0.72f).coerceAtMost(28.dp.toPx())
                        val gap = 3.dp.toPx()
                        val singleBarWidth = ((groupWidth - gap) / 2f).coerceAtLeast(3.dp.toPx())

                        points.forEachIndexed { index, pt ->
                            val centerX = slotWidth * index + slotWidth / 2f
                            if (index == selectedIndex) {
                                drawRoundRect(
                                    color = selectionBandColor,
                                    topLeft = Offset(slotWidth * index + 2.dp.toPx(), topPad),
                                    size = Size((slotWidth - 4.dp.toPx()).coerceAtLeast(2f), plotHeight),
                                    cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx())
                                )
                            }

                            val sentH = (pt.sentCount.toFloat() / maxValue.toFloat()) * plotHeight * progress
                            val recvH = (pt.receivedCount.toFloat() / maxValue.toFloat()) * plotHeight * progress

                            val leftBarX = centerX - groupWidth / 2f
                            val rightBarX = leftBarX + singleBarWidth + gap
                            val baseY = topPad + plotHeight

                            if (sentH > 0.5f) {
                                drawRoundRect(
                                    color = sentColor,
                                    topLeft = Offset(leftBarX, baseY - sentH),
                                    size = Size(singleBarWidth, sentH),
                                    cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                                )
                            }
                            if (recvH > 0.5f) {
                                drawRoundRect(
                                    color = receivedColor,
                                    topLeft = Offset(rightBarX, baseY - recvH),
                                    size = Size(singleBarWidth, recvH),
                                    cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                                )
                            }
                        }
                    } else {
                        val xStep = if (count > 1) plotWidth / (count - 1) else 0f
                        val baseY = topPad + plotHeight

                        fun coordsFor(selector: (DailySmsChartPoint) -> Int): List<Offset> {
                            return points.mapIndexed { idx, pt ->
                                val x = if (count > 1) idx * xStep else plotWidth / 2f
                                val norm = (selector(pt).toFloat() / maxValue.toFloat()).coerceIn(0f, 1f)
                                val y = baseY - (norm * plotHeight * progress)
                                Offset(x, y)
                            }
                        }

                        fun drawSmoothLine(coords: List<Offset>, strokeColor: Color) {
                            if (coords.isEmpty()) return
                            val linePath = Path().apply {
                                moveTo(coords.first().x, coords.first().y)
                                for (i in 1 until coords.size) {
                                    val prev = coords[i - 1]
                                    val curr = coords[i]
                                    val ctrlX = (prev.x + curr.x) / 2f
                                    cubicTo(ctrlX, prev.y, ctrlX, curr.y, curr.x, curr.y)
                                }
                            }
                            drawPath(
                                path = linePath,
                                color = strokeColor,
                                style = Stroke(width = 2.8.dp.toPx(), cap = StrokeCap.Round)
                            )
                        }

                        val recvCoords = coordsFor { it.receivedCount }
                        val sentCoords = coordsFor { it.sentCount }

                        drawSmoothLine(recvCoords, receivedColor)
                        drawSmoothLine(sentCoords, sentColor)

                        val selIdx = selectedIndex.coerceIn(0, count - 1)
                        val selX = sentCoords[selIdx].x
                        drawLine(
                            color = gridColor,
                            start = Offset(selX, topPad),
                            end = Offset(selX, baseY),
                            strokeWidth = 1.2.dp.toPx(),
                            pathEffect = dashEffect
                        )
                        listOf(
                            recvCoords[selIdx] to receivedColor,
                            sentCoords[selIdx] to sentColor
                        ).forEach { (pt, color) ->
                            drawCircle(color = selectionBandColor, radius = 7.dp.toPx(), center = pt)
                            drawCircle(color = color, radius = 4.dp.toPx(), center = pt)
                        }
                    }
                }
            }
        }

        // X-Axis day labels (spaced cleanly so labels never overlap on 14D / 30D)
        val stride = when {
            points.size <= 7 -> 1
            points.size <= 14 -> 2
            else -> 5
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 36.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            points.forEachIndexed { idx, pt ->
                if (idx % stride == 0 || idx == points.lastIndex) {
                    Text(
                        text = pt.dayLabel,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (idx == selectedIndex) FontWeight.Bold else FontWeight.Normal,
                        color = if (idx == selectedIndex) sentColor else axisTextColor
                    )
                }
            }
        }
    }
}

@Composable
private fun ChartLegendValue(
    color: Color,
    label: String,
    value: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Box(
            modifier = Modifier
                .size(9.dp)
                .clip(CircleShape)
                .background(color)
        )
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

/**
 * Recharts-inspired Horizontal Stacked Bar Chart for Most Active Contacts.
 * Clicking any contact row jumps directly into that conversation.
 */
@Composable
fun MostActiveContactsBarChart(
    contacts: List<ActiveContactStat>,
    sentColor: Color,
    receivedColor: Color,
    sentShortLabel: String,
    receivedShortLabel: String,
    onOpenContact: (name: String, phone: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val maxTotal = (contacts.maxOfOrNull { it.totalCount } ?: 1).coerceAtLeast(1)
    val reveal = remember(contacts) { Animatable(0f) }
    LaunchedEffect(contacts) {
        reveal.snapTo(0f)
        reveal.animateTo(1f, tween(durationMillis = 480, easing = FastOutSlowInEasing))
    }

    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        contacts.forEachIndexed { idx, stat ->
            val (avatarBg, avatarFg) = remember(stat.phoneNumber, isDark) {
                android17AvatarColors(stat.phoneNumber.ifBlank { stat.displayName }, isDark)
            }
            val initial = stat.displayName.trim().firstOrNull()?.uppercase() ?: "#"
            val totalRatio = (stat.totalCount.toFloat() / maxTotal.toFloat()).coerceIn(0.06f, 1f) * reveal.value
            val sentPortion = if (stat.totalCount > 0) stat.sentCount.toFloat() / stat.totalCount.toFloat() else 0f

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { onOpenContact(stat.displayName, stat.phoneNumber) }
                    .padding(vertical = 4.dp, horizontal = 2.dp)
                    .testTag("dashboard_active_contact_$idx"),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(avatarBg),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = initial,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = avatarFg
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stat.displayName,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "$sentShortLabel ${stat.sentCount} • $receivedShortLabel ${stat.receivedCount}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = stat.totalCount.toString(),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp)
                        )
                    }
                }

                // Horizontal stacked bar track
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(RoundedCornerShape(50))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val barW = size.width * totalRatio
                        val sentW = barW * sentPortion
                        val recvW = barW - sentW
                        if (sentW > 0.5f) {
                            drawRoundRect(
                                color = sentColor,
                                topLeft = Offset.Zero,
                                size = Size(sentW, size.height),
                                cornerRadius = CornerRadius(size.height / 2f, size.height / 2f)
                            )
                        }
                        if (recvW > 0.5f) {
                            drawRoundRect(
                                color = receivedColor,
                                topLeft = Offset(sentW, 0f),
                                size = Size(recvW, size.height),
                                cornerRadius = CornerRadius(size.height / 2f, size.height / 2f)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Recharts-inspired Donut / Pie Chart visualizing Cellular Data Saved (100% Offline SMS)
 * and Compact AES-256 Storage Savings.
 */
@Composable
fun DataStorageSavingsDonutChart(
    slices: List<SavingsDonutSlice>,
    centerPrimaryText: String,
    centerSecondaryText: String,
    modifier: Modifier = Modifier
) {
    val totalWeight = slices.sumOf { it.weight.toDouble() }.toFloat().coerceAtLeast(0.01f)
    val sweepAnim = remember(slices) { Animatable(0f) }
    LaunchedEffect(slices) {
        sweepAnim.snapTo(0f)
        sweepAnim.animateTo(1f, tween(durationMillis = 600, easing = FastOutSlowInEasing))
    }

    val trackColor = MaterialTheme.colorScheme.surfaceVariant

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier
                .size(134.dp)
                .testTag("dashboard_savings_donut"),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokeWidth = 18.dp.toPx()
                val diameter = size.minDimension - strokeWidth
                val topLeft = Offset(
                    (size.width - diameter) / 2f,
                    (size.height - diameter) / 2f
                )
                val arcSize = Size(diameter, diameter)

                // Background ring
                drawArc(
                    color = trackColor,
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )

                var startAngle = -90f
                slices.forEach { slice ->
                    val rawSweep = (slice.weight / totalWeight) * 360f * sweepAnim.value
                    val gap = if (slices.size > 1 && rawSweep > 8f) 4f else 0f
                    val sweep = (rawSweep - gap).coerceAtLeast(0f)
                    if (sweep > 0.5f) {
                        drawArc(
                            color = slice.color,
                            startAngle = startAngle + gap / 2f,
                            sweepAngle = sweep,
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
                        )
                    }
                    startAngle += rawSweep
                }
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = centerPrimaryText,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = centerSecondaryText,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            slices.forEach { slice ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(slice.color)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = slice.label,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = slice.subtitle,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Text(
                        text = slice.valueText,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = slice.color
                    )
                }
            }
        }
    }
}
