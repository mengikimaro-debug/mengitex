package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DataSaverOn
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.ScheduleSend
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.DecryptedSmsMessage
import com.example.data.MessageDirection
import com.example.data.MessageStatus
import com.example.security.VaultFolderStats
import com.example.service.ContactsLoader
import com.example.ui.components.ActiveContactStat
import com.example.ui.components.DailySmsChartPoint
import com.example.ui.components.DailyUsageAreaBarChart
import com.example.ui.components.DataStorageSavingsDonutChart
import com.example.ui.components.MostActiveContactsBarChart
import com.example.ui.components.SavingsDonutSlice
import com.example.ui.i18n.LocalAppStrings
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Interactive SMS Usage & Savings Analytics Dashboard.
 * Visualizes daily SMS sent/received trends, most active contacts, zero-data offline savings,
 * AES-256 compact storage efficiency, and security shield impact using live vault data.
 */
@Composable
fun UsageDashboardScreen(
    messages: List<DecryptedSmsMessage>,
    scheduledMessages: List<DecryptedSmsMessage>,
    trashedCount: Int,
    vaultStats: VaultFolderStats,
    onOpenConversation: (name: String, phone: String) -> Unit,
    onQuickCompose: () -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)
    val s = LocalAppStrings.current

    var selectedDays by remember { mutableIntStateOf(7) }
    var isBarMode by remember { mutableStateOf(false) }

    val sentColor = MaterialTheme.colorScheme.primary
    val receivedColor = MaterialTheme.colorScheme.secondary
    val savingsGreen = Color(0xFF10B981)
    val amberAccent = MaterialTheme.colorScheme.tertiary

    // Build real daily timeline buckets for the selected range (7 / 14 / 30 days)
    val dailyPoints = remember(messages, selectedDays) {
        val dayFmt = SimpleDateFormat(if (selectedDays <= 7) "EEE" else "d/M", Locale.getDefault())
        val fullFmt = SimpleDateFormat("EEE, dd MMM", Locale.getDefault())

        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val todayStart = cal.timeInMillis
        val dayMs = 24L * 60L * 60L * 1000L
        val rangeStart = todayStart - (selectedDays - 1) * dayMs

        val sentBuckets = IntArray(selectedDays)
        val recvBuckets = IntArray(selectedDays)

        messages.forEach { msg ->
            if (msg.timestamp >= rangeStart) {
                val bucket = ((msg.timestamp - rangeStart) / dayMs).toInt()
                if (bucket in 0 until selectedDays) {
                    if (msg.direction == MessageDirection.OUTGOING) {
                        sentBuckets[bucket]++
                    } else {
                        recvBuckets[bucket]++
                    }
                }
            }
        }

        (0 until selectedDays).map { i ->
            val date = Date(rangeStart + i * dayMs)
            DailySmsChartPoint(
                dayLabel = dayFmt.format(date),
                fullDateLabel = fullFmt.format(date),
                sentCount = sentBuckets[i],
                receivedCount = recvBuckets[i]
            )
        }
    }

    val rangeSentTotal = remember(dailyPoints) { dailyPoints.sumOf { it.sentCount } }
    val rangeReceivedTotal = remember(dailyPoints) { dailyPoints.sumOf { it.receivedCount } }

    // Overall real counts & delivery rate
    val totalSentAll = remember(messages) { messages.count { it.direction == MessageDirection.OUTGOING } }
    val totalDeliveredOrSent = remember(messages) {
        messages.count {
            it.direction == MessageDirection.OUTGOING &&
                (it.status == MessageStatus.DELIVERED || it.status == MessageStatus.SENT)
        }
    }
    val deliveryRatePct = if (totalSentAll > 0) {
        ((totalDeliveredOrSent.toFloat() / totalSentAll.toFloat()) * 100f).roundToInt()
    } else {
        100
    }

    // Most Active Contacts ranked by real message volume (single-pass O(N) for 100k+ SMS)
    val topContacts = remember(messages) {
        val genericLabels = setOf("Namba Geni (Haijahifadhiwa)", "Unknown Number (Unsaved)")
        class ContactAcc(
            var phone: String,
            var bestName: String?,
            var sent: Int = 0,
            var received: Int = 0
        )
        val map = HashMap<String, ContactAcc>(256)
        for (i in 0 until messages.size) {
            val msg = messages[i]
            val norm = ContactsLoader.normalizePhone(msg.phoneNumber).ifEmpty { msg.phoneNumber.trim() }
            var acc = map[norm]
            if (acc == null) {
                acc = ContactAcc(phone = msg.phoneNumber, bestName = null)
                map[norm] = acc
            }
            if (acc.bestName == null) {
                val cand = msg.contactName.trim()
                if (cand.isNotEmpty() && cand !in genericLabels && cand != msg.phoneNumber) {
                    acc.bestName = cand
                }
            }
            if (msg.direction == MessageDirection.OUTGOING) acc.sent++ else acc.received++
        }
        map.values.map { acc ->
            ActiveContactStat(
                displayName = (acc.bestName ?: acc.phone).ifBlank { acc.phone },
                phoneNumber = acc.phone,
                sentCount = acc.sent,
                receivedCount = acc.received
            )
        }.sortedByDescending { it.totalCount }.take(6)
    }

    // Data & Storage Savings metrics computed from real message/vault counts
    val savingsMetrics = remember(messages, vaultStats, topContacts) {
        val totalMessages = messages.size
        val totalParts = messages.sumOf { it.partCount.coerceAtLeast(1) }
        val mobileDataSavedKb = totalParts * 48L
        val actualTextBytes = messages.sumOf { it.body.length.toLong() + 96L } + vaultStats.totalSizeBytes
        val actualStorageKb = (actualTextBytes / 1024L).coerceAtLeast(if (totalMessages > 0) 1L else 0L)
        val estimatedMediaAppKb = (topContacts.size.coerceAtLeast(1) * 320L) + (totalMessages * 14L)
        val storageSavedKb = (estimatedMediaAppKb - actualStorageKb).coerceAtLeast(0L)
        val totalSavedKb = mobileDataSavedKb + storageSavedKb
        Triple(mobileDataSavedKb, storageSavedKb, actualStorageKb to totalSavedKb)
    }
    val (mobileDataSavedKb, storageSavedKb, storagePair) = savingsMetrics
    val (actualStorageKb, totalSavedKb) = storagePair

    val secretBurnCount = remember(messages) { messages.count { it.isSecretBurnAfterRead } }
    val silentBlockedCount = remember(messages) { messages.count { it.isSilentIntercepted || it.isUnknownSender } }
    val deliveredConfirmedCount = remember(messages) { messages.count { it.status == MessageStatus.DELIVERED } }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 720.dp)
        ) {
            // Samsung One UI 8.5 Floating Header Bar
            com.example.ui.components.OneUi85SubScreenHeader(
                title = s.dashboardTitle,
                subtitle = s.dashboardSubtitle,
                backIcon = Icons.AutoMirrored.Filled.ArrowBack,
                backContentDescription = s.cdBack,
                backTestTag = "dashboard_back_button",
                onBack = onBack
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Time Range Selector Pills
                item(key = "range_selector") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        listOf(7 to s.dashboardRange7d, 14 to s.dashboardRange14d, 30 to s.dashboardRange30d).forEach { (days, label) ->
                            FilterChip(
                                selected = selectedDays == days,
                                onClick = { selectedDays = days },
                                label = {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = if (selectedDays == days) FontWeight.Bold else FontWeight.Medium
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                ),
                                modifier = Modifier.testTag("dashboard_range_${days}d")
                            )
                        }
                    }
                }

                // 2x2 KPI Summary Cards
                item(key = "kpi_grid") {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            DashboardKpiCard(
                                icon = Icons.Filled.Send,
                                title = s.dashboardKpiSent,
                                value = rangeSentTotal.toString(),
                                detail = "${s.dashboardTotalAll}: $totalSentAll",
                                accent = sentColor,
                                modifier = Modifier.weight(1f)
                            )
                            DashboardKpiCard(
                                icon = Icons.Filled.MarkEmailRead,
                                title = s.dashboardKpiReceived,
                                value = rangeReceivedTotal.toString(),
                                detail = "${s.dashboardBlockedShort}: $silentBlockedCount",
                                accent = receivedColor,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            DashboardKpiCard(
                                icon = Icons.Filled.CheckCircle,
                                title = s.dashboardKpiDeliveryRate,
                                value = "$deliveryRatePct%",
                                detail = "${s.dashboardConfirmedShort}: $deliveredConfirmedCount",
                                accent = savingsGreen,
                                modifier = Modifier.weight(1f)
                            )
                            DashboardKpiCard(
                                icon = Icons.Filled.DataSaverOn,
                                title = s.dashboardKpiDataSaved,
                                value = formatKbSmart(totalSavedKb),
                                detail = s.dashboardZeroDataBadge,
                                accent = amberAccent,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // Chart 1: Daily SMS Activity (Interactive Area / Bar Chart)
                item(key = "daily_chart") {
                    DashboardChartCard(
                        icon = if (isBarMode) Icons.Filled.BarChart else Icons.Filled.ShowChart,
                        title = s.dashboardDailyChartTitle,
                        subtitle = s.dashboardDailyChartSub(selectedDays),
                        accent = sentColor,
                        headerAction = {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                FilterChip(
                                    selected = !isBarMode,
                                    onClick = { isBarMode = false },
                                    label = { Text(s.dashboardChartArea, style = MaterialTheme.typography.labelSmall) },
                                    modifier = Modifier.testTag("dashboard_mode_area")
                                )
                                FilterChip(
                                    selected = isBarMode,
                                    onClick = { isBarMode = true },
                                    label = { Text(s.dashboardChartBar, style = MaterialTheme.typography.labelSmall) },
                                    modifier = Modifier.testTag("dashboard_mode_bar")
                                )
                            }
                        }
                    ) {
                        DailyUsageAreaBarChart(
                            points = dailyPoints,
                            isBarMode = isBarMode,
                            sentLabel = s.dashboardKpiSent,
                            receivedLabel = s.dashboardKpiReceived,
                            totalLabel = s.dashboardTotalAll,
                            sentColor = sentColor,
                            receivedColor = receivedColor
                        )
                    }
                }

                // Chart 2: Most Active Contacts Horizontal Bar Chart
                item(key = "active_contacts_chart") {
                    DashboardChartCard(
                        icon = Icons.Filled.People,
                        title = s.dashboardTopContactsTitle,
                        subtitle = s.dashboardTopContactsSub,
                        accent = receivedColor
                    ) {
                        if (topContacts.isEmpty()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 18.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = s.dashboardNoDataYet,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Button(
                                    onClick = onQuickCompose,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.testTag("dashboard_empty_compose_button")
                                ) {
                                    Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(s.drawerCompose)
                                }
                            }
                        } else {
                            MostActiveContactsBarChart(
                                contacts = topContacts,
                                sentColor = sentColor,
                                receivedColor = receivedColor,
                                sentShortLabel = s.dashboardKpiSent,
                                receivedShortLabel = s.dashboardKpiReceived,
                                onOpenContact = onOpenConversation
                            )
                        }
                    }
                }

                // Chart 3: Data & Storage Savings Donut Chart
                item(key = "savings_donut") {
                    DashboardChartCard(
                        icon = Icons.Filled.DataSaverOn,
                        title = s.dashboardSavingsTitle,
                        subtitle = s.dashboardSavingsSub,
                        accent = savingsGreen
                    ) {
                        val slices = listOf(
                            SavingsDonutSlice(
                                label = s.dashboardSavingsMobileData,
                                subtitle = s.dashboardZeroDataBadge,
                                valueText = formatKbSmart(mobileDataSavedKb),
                                weight = mobileDataSavedKb.toFloat().coerceAtLeast(1f),
                                color = savingsGreen
                            ),
                            SavingsDonutSlice(
                                label = s.dashboardSavingsStorage,
                                subtitle = s.dashboardSavingsStorageSub,
                                valueText = formatKbSmart(storageSavedKb),
                                weight = storageSavedKb.toFloat().coerceAtLeast(1f),
                                color = sentColor
                            ),
                            SavingsDonutSlice(
                                label = s.dashboardSavingsVaultUsed,
                                subtitle = "${vaultStats.totalEncryptedFiles} files • $trashedCount trash",
                                valueText = formatKbSmart(actualStorageKb),
                                weight = actualStorageKb.toFloat().coerceAtLeast(0.5f),
                                color = amberAccent
                            )
                        )
                        DataStorageSavingsDonutChart(
                            slices = slices,
                            centerPrimaryText = formatKbSmart(totalSavedKb),
                            centerSecondaryText = s.dashboardSavedShort
                        )
                    }
                }

                // Chart 4: Security & Power Breakdown
                item(key = "security_breakdown") {
                    val denominator = messages.size.coerceAtLeast(1).toFloat()
                    DashboardChartCard(
                        icon = Icons.Filled.Shield,
                        title = s.dashboardSecurityImpactTitle,
                        subtitle = s.dashboardSecurityImpactSub,
                        accent = amberAccent
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            ImpactProgressRow(
                                icon = Icons.Filled.NotificationsOff,
                                label = s.dashboardImpactSilentBlocked,
                                count = silentBlockedCount,
                                progress = (silentBlockedCount / denominator).coerceIn(0f, 1f),
                                color = receivedColor
                            )
                            ImpactProgressRow(
                                icon = Icons.Filled.LocalFireDepartment,
                                label = s.dashboardImpactSecretBurn,
                                count = secretBurnCount,
                                progress = (secretBurnCount / denominator).coerceIn(0f, 1f),
                                color = amberAccent
                            )
                            ImpactProgressRow(
                                icon = Icons.Filled.ScheduleSend,
                                label = s.dashboardImpactScheduled,
                                count = scheduledMessages.size,
                                progress = (scheduledMessages.size / (denominator + scheduledMessages.size)).coerceIn(0f, 1f),
                                color = sentColor
                            )
                            ImpactProgressRow(
                                icon = Icons.Filled.CheckCircle,
                                label = s.dashboardImpactDelivered,
                                count = deliveredConfirmedCount,
                                progress = (deliveredConfirmedCount / totalSentAll.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f),
                                color = savingsGreen
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DashboardKpiCard(
    icon: ImageVector,
    title: String,
    value: String,
    detail: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        modifier = modifier.border(
            1.dp,
            accent.copy(alpha = 0.28f),
            RoundedCornerShape(20.dp)
        )
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(accent.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = detail,
                style = MaterialTheme.typography.labelSmall,
                color = accent,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun DashboardChartCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    accent: Color,
    headerAction: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        modifier = Modifier
            .fillMaxWidth()
            .border(
                1.dp,
                MaterialTheme.colorScheme.outline.copy(alpha = 0.22f),
                RoundedCornerShape(22.dp)
            )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(11.dp))
                            .background(accent.copy(alpha = 0.14f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = accent,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                    Column {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                if (headerAction != null) {
                    headerAction()
                }
            }
            content()
        }
    }
}

@Composable
private fun ImpactProgressRow(
    icon: ImageVector,
    label: String,
    count: Int,
    progress: Float,
    color: Color
) {
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(color.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier.size(14.dp)
                    )
                }
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(7.dp)
                .clip(RoundedCornerShape(50)),
            color = color,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }
}

private fun formatKbSmart(kb: Long): String {
    return when {
        kb >= 1024L -> String.format(Locale.US, "%.1f MB", kb / 1024.0)
        kb > 0L -> "$kb KB"
        else -> "0 KB"
    }
}
