package com.example.service

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import androidx.core.content.ContextCompat

data class DetectedSimCard(
    val subscriptionId: Int,
    val slotIndex: Int,
    val displayName: String,
    val carrierName: String,
    val countryIso: String,
    val numberHint: String,
    val isActive: Boolean
)

/**
 * Automatically detects any active SIM card(s) inserted in the device (Single or Dual SIM / eSIM)
 * using SubscriptionManager and TelephonyManager.
 */
object SimCardManager {

    private val blockedCarrierRegex = Regex("(?i)\\bt[\\s\\-]*mobile\\b")

    /**
     * Strips any "T-Mobile" / "T Mobile" / "TMobile" label (e.g. default emulator carrier)
     * and cleans dangling separators while preserving other carrier names.
     */
    fun sanitizeSimText(raw: String?, fallback: String): String {
        val cleaned = raw.orEmpty()
            .replace(blockedCarrierRegex, "")
            .replace(Regex("\\s*[•\\-|]\\s*$"), "")
            .replace(Regex("^\\s*[•\\-|]\\s*"), "")
            .replace(Regex("\\s{2,}"), " ")
            .trim()
        return cleaned.ifBlank { fallback }
    }

    @SuppressLint("MissingPermission")
    fun detectSimCards(context: Context): List<DetectedSimCard> {
        val hasPhoneState = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_PHONE_STATE
        ) == PackageManager.PERMISSION_GRANTED

        val results = mutableListOf<DetectedSimCard>()

        if (hasPhoneState) {
            try {
                val subManager = context.getSystemService(SubscriptionManager::class.java)
                val activeList = subManager?.activeSubscriptionInfoList.orEmpty()
                activeList.forEach { info ->
                    val slot = info.simSlotIndex + 1
                    val rawCarrier = info.carrierName?.toString()?.takeIf { it.isNotBlank() }
                        ?: info.displayName?.toString()?.takeIf { it.isNotBlank() }
                    val carrier = sanitizeSimText(rawCarrier, "Mtandao SIM $slot")
                    val rawLabel = info.displayName?.toString()?.takeIf { it.isNotBlank() }
                    val sanitizedLabel = sanitizeSimText(rawLabel, "")
                    val label = if (sanitizedLabel.isNotBlank()) sanitizedLabel else "SIM $slot"
                    val phoneHint = try {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            subManager.getPhoneNumber(info.subscriptionId)
                        } else {
                            @Suppress("DEPRECATION")
                            info.number.orEmpty()
                        }
                    } catch (_: Exception) {
                        ""
                    }
                    results.add(
                        DetectedSimCard(
                            subscriptionId = info.subscriptionId,
                            slotIndex = slot,
                            displayName = label,
                            carrierName = carrier,
                            countryIso = info.countryIso?.uppercase().orEmpty(),
                            numberHint = phoneHint,
                            isActive = true
                        )
                    )
                }
            } catch (_: Exception) {
                // Fall back to TelephonyManager below
            }
        }

        if (results.isEmpty()) {
            try {
                val tm = context.getSystemService(TelephonyManager::class.java)
                val simState = tm?.simState ?: TelephonyManager.SIM_STATE_UNKNOWN
                val rawOperator = tm?.simOperatorName?.takeIf { it.isNotBlank() }
                    ?: tm?.networkOperatorName?.takeIf { it.isNotBlank() }
                    ?: ""
                val operator = sanitizeSimText(rawOperator, "")
                val country = tm?.simCountryIso?.uppercase().orEmpty()

                if (simState == TelephonyManager.SIM_STATE_READY || rawOperator.isNotBlank()) {
                    results.add(
                        DetectedSimCard(
                            subscriptionId = SubscriptionManager.getDefaultSmsSubscriptionId(),
                            slotIndex = 1,
                            displayName = if (operator.isNotBlank()) "SIM 1 • $operator" else "SIM 1",
                            carrierName = operator.ifBlank { "Mtandao wa Simu" },
                            countryIso = country,
                            numberHint = "",
                            isActive = true
                        )
                    )
                }
            } catch (_: Exception) {
                // Ignore
            }
        }

        return results
    }

    fun resolveEffectiveSim(context: Context, preferredSubId: Int): DetectedSimCard? {
        val detected = detectSimCards(context)
        if (preferredSubId > 0 && preferredSubId != SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
            detected.firstOrNull { it.subscriptionId == preferredSubId }?.let { return it }
        }
        val defaultSmsSubId = runCatching { SubscriptionManager.getDefaultSmsSubscriptionId() }
            .getOrDefault(SubscriptionManager.INVALID_SUBSCRIPTION_ID)
        if (defaultSmsSubId > 0 && defaultSmsSubId != SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
            detected.firstOrNull { it.subscriptionId == defaultSmsSubId }?.let { return it }
        }
        detected.firstOrNull()?.let { return it }

        if (defaultSmsSubId > 0 && defaultSmsSubId != SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
            return DetectedSimCard(
                subscriptionId = defaultSmsSubId,
                slotIndex = 1,
                displayName = "SIM 1",
                carrierName = "Mtandao wa Simu",
                countryIso = "",
                numberHint = "",
                isActive = true
            )
        }
        return null
    }

    fun getCandidateSubIds(context: Context, preferredSubId: Int): List<Int> {
        val candidates = LinkedHashSet<Int>(4)
        if (preferredSubId > 0 && preferredSubId != SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
            candidates.add(preferredSubId)
        }
        val defaultSmsSubId = runCatching { SubscriptionManager.getDefaultSmsSubscriptionId() }
            .getOrDefault(SubscriptionManager.INVALID_SUBSCRIPTION_ID)
        if (defaultSmsSubId > 0 && defaultSmsSubId != SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
            candidates.add(defaultSmsSubId)
        }
        detectSimCards(context).forEach { sim ->
            if (sim.subscriptionId > 0 && sim.subscriptionId != SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
                candidates.add(sim.subscriptionId)
            }
        }
        return candidates.toList()
    }

    fun getSimStateSummary(context: Context): String {
        val tm = context.getSystemService(TelephonyManager::class.java)
        return when (tm?.simState) {
            TelephonyManager.SIM_STATE_READY -> "SIM Iko Tayari"
            TelephonyManager.SIM_STATE_ABSENT -> "Hakuna SIM Iliyoingizwa"
            TelephonyManager.SIM_STATE_PIN_REQUIRED -> "SIM Inahitaji PIN"
            TelephonyManager.SIM_STATE_NETWORK_LOCKED -> "Mtandao Umefungwa"
            else -> "Inachunguza SIM..."
        }
    }
}
