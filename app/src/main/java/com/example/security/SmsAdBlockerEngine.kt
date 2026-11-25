package com.example.security

import com.example.service.ContactsLoader
import java.util.Locale

data class AdBlockDecision(
    val shouldBlock: Boolean,
    val isProtectedTransaction: Boolean,
    val reasonSw: String,
    val reasonEn: String
)

/**
 * High-precision Offline SMS Ad Blocker Engine.
 *
 * Automatically blocks promotional/commercial/betting/spam SMS — especially:
 * - Shortcodes & bulk promo numbers starting with 15... (e.g., 15588, 15456, 15000)
 * - Premium/promo numbers starting with 09... (e.g., 0900..., 0901...)
 * - Bulk alphanumeric & non-transaction commercial senders
 *
 * Strictly PROTECTS real financial transactions, mobile money receipts (M-Pesa, Mixx/Tigo Pesa,
 * Airtel Money, HaloPesa), bank debit/credit alerts, LUKU/utility tokens, and OTP/2FA codes.
 */
object SmsAdBlockerEngine {

    private val currencyAmountRegex = Regex(
        """(?i)(tzs|tsh|kes|ksh|ugx|usd|\$)\s?[\d,]+(\.\d{1,2})?|[\d,]+(\.\d{1,2})?\s?(tzs|tsh|shilingi|kes|usd)"""
    )

    private val ussdPromoRegex = Regex("""\*\d{3}[\d*#]*#""")
    private val urlRegex = Regex("""(?i)(https?://|www\.|bit\.ly/|tinyurl\.com/|t\.co/|wa\.me/)""")
    private val otpDigitRegex = Regex("""\b\d{4,8}\b""")
    private val transactionRefRegex = Regex("""(?i)\b([A-Z]{2,4}[0-9A-Z]{6,12}|ref[:#\s]+[A-Z0-9-]{5,}|kumbukumbu[:#\s]+[A-Z0-9-]{5,}|control\s*(no|number|namba)[:#\s]*\d{6,})\b""")

    // Strong transaction/financial confirmation markers (Swahili + English)
    private val strongTransactionKeywords = listOf(
        "umepokea", "umetuma", "umelipa", "umetoa", "umeweka",
        "imeingia", "imetolewa", "salio lako", "salio jipya", "salio ni",
        "muamala", "miamala", "malipo yako", "malipo yamefanikiwa",
        "imethibitishwa", "kumbukumbu", "risiti", "control number", "namba ya malipo",
        "token yako", "luku", "units", "kwh", "gepg", "tra ",
        "confirmed.", "confirmed ", "you have received", "you have sent",
        "have paid", "paid to", "transferred to", "received from",
        "new balance", "available balance", "current balance", "your balance is",
        "account credited", "account debited", "debited with", "credited with",
        "transaction id", "txn id", "trx id", "receipt no", "payment received",
        "payment successful", "withdrawal of", "deposit of", "cash out", "cash in",
        "mpesa balance", "m-pesa balance", "airtel money", "tigo pesa", "mixx by yas",
        "halopesa", "azam pesa", "t-pesa", "selcom"
    )

    // OTP / security verification markers (must never be blocked)
    private val otpKeywords = listOf(
        "otp", "verification code", "security code", "login code", "passcode",
        "one-time", "one time password", "2fa", "auth code", "confirm code",
        "namba ya uthibitisho", "namba ya siri", "msimbo", "code yako", "kodi yako",
        "usishirikishe", "do not share", "valid for", "expires in"
    )

    // Promotional / Ad / Betting / Spam markers (Swahili + English)
    private val strongPromoKeywords = listOf(
        "shinda", "jishindie", "zawadi", "bonasi", "bonus", "jackpot",
        "bashiri", "kasino", "casino", "betting", "odds", "sportpesa",
        "betpawa", "meridianbet", "parimatch", "sokabet", "wasafibet", "gsb",
        "gal sport", "1xbet", "22bet", "premierbet", "pmbet", "betika",
        "biko", "tatumzuka", "bahati nasibu", "droo", "tiketi ya bahati",
        "ofa maalum", "ofa ya", "ofa!", "ofa ", "punguzo", "bure!", "jiunge sasa",
        "jiunge na", "tuma neno", "piga *", "bofya hapa", "click here",
        "tangazo", "matangazo", "usipitwe", "furahia ofa", "furahia ", "vifurushi",
        "kifurushi", "bando la", "bando bure", "royal bando", "cheka", "saizi yako",
        "jaza ujazwe", "pata gb", "pata mb", "pata dakika", "dakika bure", "sms bure",
        "mkopo wa haraka", "kopa sasa", "pata mkopo", "instant loan", "loan offer",
        "special offer", "limited time", "flash sale", "discount", "promo code",
        "promotional", "promosheni", "win big", "win up to", "lucky draw",
        "subscribe", "unsubscribe", "opt out", "opt-out", "reply stop", "tuma stop",
        "kujitoa", "download app", "pakua app", "nunua sasa", "shop now",
        "free spins", "free bet", "cashback offer", "claim now", "dai zawadi"
    )

    // Commercial / Promo alphanumeric sender ID fragments
    private val adSenderTokens = listOf(
        "bet", "sport", "casino", "lotto", "biko", "mzuka", "promo", "offer",
        "ofa", "deal", "ads", "advert", "bonus", "win", "game", "club", "loan",
        "mkopo", "kopa", "shop", "mall", "market", "info", "news", "media",
        "tv", "radio", "music", "bulk", "sms", "alert", "notify", "reward",
        "jackpot", "spin", "play", "lucky", "vip", "cash", "vodacom", "tigo",
        "airtel", "halotel", "zantel", "ttcl", "yas", "dstv", "azam", "startimes", "zuku"
    )

    // Known financial / utility institutions that send real transactions (still checked for promo content!)
    private val financialSenderTokens = listOf(
        "mpesa", "m-pesa", "tigopesa", "mixx", "airtelmoney", "halopesa", "azampesa",
        "tpesa", "selcom", "nmb", "crdb", "nbc", "equity", "kcb", "stanbic", "dtb",
        "absa", "exim", "pbz", "azania", "akiba", "tanesco", "luku", "dawasa",
        "tra", "gepg", "nssf", "nhif", "visa", "mastercard", "paypal", "bank", "pesa"
    )

    private val explicitAdPrefixes = listOf("15", "16", "17", "18", "19", "09", "55")

    /**
     * Returns true for normal human phone numbers:
     * - Starts with +255 (or 00255 / 255)
     * - Starts with 07... or 06...
     * These are ALWAYS treated as normal people's numbers and NEVER mixed into Blocked Ads.
     */
    fun isPersonalHumanPhoneNumber(rawSender: String): Boolean {
        val trimmed = rawSender.trim()
        if (trimmed.isEmpty()) return false
        if (ContactsLoader.isAlphanumericSender(trimmed)) return false
        val compact = trimmed.replace(Regex("[\\s\\-()]"), "")
        val digits = compact.filter { it.isDigit() }
        if (digits.length < 7) return false
        return compact.startsWith("+255") ||
            compact.startsWith("00255") ||
            digits.startsWith("255") ||
            digits.startsWith("07") ||
            digits.startsWith("06")
    }

    /**
     * Returns the matched ad prefix if the numeric sender starts with one of the explicit ad prefixes:
     * 15, 16, 17, 18, 19, 09, 55.
     */
    fun matchedAdNumberPrefix(rawSender: String): String? {
        val trimmed = rawSender.trim()
        if (trimmed.isEmpty()) return null
        if (ContactsLoader.isAlphanumericSender(trimmed)) return null
        if (isPersonalHumanPhoneNumber(trimmed)) return null
        val digits = trimmed.filter { it.isDigit() }
        if (digits.length < 2) return null
        return explicitAdPrefixes.firstOrNull { prefix -> digits.startsWith(prefix) }
    }

    fun evaluate(
        senderAddress: String,
        body: String,
        isSavedContact: Boolean,
        whitelistedSenders: Set<String> = emptySet(),
        autoBlockAdsEnabled: Boolean = true,
        senderName: String = ""
    ): AdBlockDecision {
        if (!autoBlockAdsEnabled) {
            return AdBlockDecision(false, false, "", "")
        }

        val cleanSender = ContactsLoader.cleanSenderIdentifier(senderAddress)
            .ifEmpty { ContactsLoader.cleanSenderIdentifier(senderName) }
        val cleanName = ContactsLoader.cleanSenderIdentifier(senderName)
            .ifEmpty { cleanSender }

        val normalizedKey = canonicalSenderKey(cleanSender)
        val normalizedNameKey = canonicalSenderKey(cleanName)
        if (whitelistedSenders.isNotEmpty()) {
            val isWhitelisted = whitelistedSenders.any { w ->
                val wk = canonicalSenderKey(w)
                wk.isNotEmpty() && (wk == normalizedKey || wk == normalizedNameKey)
            }
            if (isWhitelisted) {
                return AdBlockDecision(false, false, "", "")
            }
        }

        val lowerBody = body.lowercase(Locale.ROOT)
        val lowerSender = cleanSender.lowercase(Locale.ROOT)
        val lowerName = cleanName.lowercase(Locale.ROOT)
        val combinedSenderIdentity = "$lowerSender $lowerName"

        // 1. Check if this message is an authentic Transaction / Mobile Money / Bank / Utility / OTP SMS
        val isOtp = otpDigitRegex.containsMatchIn(body) && otpKeywords.any { lowerBody.contains(it) }
        val isRealTransaction = isOtp || isGenuineTransactionMessage(combinedSenderIdentity, lowerBody, body)

        // 2. STRICT RULE: Numbers starting with 07..., 06..., or +255... are ALWAYS human numbers — NEVER Ads!
        if (isPersonalHumanPhoneNumber(cleanSender)) {
            return AdBlockDecision(
                shouldBlock = false,
                isProtectedTransaction = isRealTransaction,
                reasonSw = if (isRealTransaction) "Muamala/OTP umelindwa" else "",
                reasonEn = if (isRealTransaction) "Transaction/OTP protected" else ""
            )
        }

        // 3. Protect genuine financial transactions & OTPs (M-Pesa, Mixx/Tigo Pesa, Airtel Money, Banks, LUKU, OTP)
        if (isRealTransaction) {
            return AdBlockDecision(
                shouldBlock = false,
                isProtectedTransaction = true,
                reasonSw = "Muamala/OTP umelindwa",
                reasonEn = "Transaction/OTP protected"
            )
        }

        // 4. Explicit Ad Number Prefixes: 15, 16, 17, 18, 19, 09, 55
        val matchedPrefix = matchedAdNumberPrefix(cleanSender)
        if (matchedPrefix != null) {
            return AdBlockDecision(
                shouldBlock = true,
                isProtectedTransaction = false,
                reasonSw = "Tangazo la namba $matchedPrefix... limewekwa kwenye Ads ($cleanSender)",
                reasonEn = "Auto-blocked $matchedPrefix... ad number ($cleanSender)"
            )
        }

        // 5. Name-Only / Alphanumeric Sender (arrives as a name without a 07/06/+255 phone number) -> Ads
        if (ContactsLoader.isAlphanumericSender(cleanSender)) {
            val displayLabel = cleanSender.ifEmpty { cleanName }
            return AdBlockDecision(
                shouldBlock = true,
                isProtectedTransaction = false,
                reasonSw = "Tangazo la jina bila namba limewekwa kwenye Ads ($displayLabel)",
                reasonEn = "Auto-blocked name-only ad sender ($displayLabel)"
            )
        }

        // Any other regular phone number (e.g., other international personal numbers) stays safely in Inbox
        return AdBlockDecision(false, false, "", "")
    }

    private fun isGenuineTransactionMessage(
        lowerSender: String,
        lowerBody: String,
        rawBody: String
    ): Boolean {
        // Explicit betting/promo override: messages pitching "shinda", "jackpot", "ofa", "piga *", "bashiri"
        // without completed transaction verbs are PROMOS, not transactions.
        val hasPromoPitch = listOf(
            "shinda", "jishindie", "jackpot", "bashiri", "kasino", "casino",
            "piga *", "dial *", "tuma neno", "jiunge", "ofa ya", "punguzo",
            "free bet", "free spins", "win up to", "lucky draw", "mkopo wa haraka",
            "kopa sasa", "apply now", "unsubscribe", "reply stop", "kujitoa"
        ).any { lowerBody.contains(it) }

        val completedTxnVerbs = listOf(
            "umepokea", "umetuma", "umelipa", "umetoa", "umeweka",
            "salio lako", "salio jipya", "imethibitishwa", "muamala wako",
            "token yako", "control number", "namba ya malipo",
            "confirmed", "you have received", "you have sent",
            "paid to", "transferred", "debited", "credited",
            "new balance", "available balance", "payment received",
            "payment successful", "withdrawal of", "deposit of", "receipt"
        )
        val hasCompletedVerb = completedTxnVerbs.any { lowerBody.contains(it) }
        val hasCurrency = currencyAmountRegex.containsMatchIn(rawBody)
        val hasTxnRef = transactionRefRegex.containsMatchIn(rawBody)

        // If it clearly has a completed transaction verb + money/reference (and isn't a pure promo pitch)
        if (hasCompletedVerb && (hasCurrency || hasTxnRef) && !hasPromoPitch) {
            return true
        }

        // LUKU / utility token or GePG control number receipt
        if ((lowerBody.contains("luku") || lowerBody.contains("token") || lowerBody.contains("control number")) &&
            Regex("""\b\d{4}[\s-]?\d{4}[\s-]?\d{4}\b""").containsMatchIn(rawBody)
        ) {
            return true
        }

        // Financial institution sender with a transaction confirmation keyword and no promo pitch
        val isFinancialSender = financialSenderTokens.any { lowerSender.contains(it) }
        if (isFinancialSender && !hasPromoPitch && (hasCompletedVerb || (hasCurrency && hasTxnRef))) {
            return true
        }

        // Multiple strong transaction signals with zero promo pitch
        val txnHits = strongTransactionKeywords.count { lowerBody.contains(it) }
        if (txnHits >= 2 && (hasCurrency || hasTxnRef) && !hasPromoPitch) {
            return true
        }

        return false
    }

    fun canonicalSenderKey(sender: String): String {
        val trimmed = sender.trim()
        if (trimmed.isEmpty()) return ""
        return if (ContactsLoader.isAlphanumericSender(trimmed)) {
            trimmed.uppercase(Locale.ROOT)
        } else {
            ContactsLoader.normalizePhone(trimmed).ifEmpty { trimmed.uppercase(Locale.ROOT) }
        }
    }
}
