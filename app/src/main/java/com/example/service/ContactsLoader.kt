package com.example.service

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.ContactsContract
import androidx.core.content.ContextCompat
import com.example.data.ContactItem

/**
 * Loads real contacts from Android ContactsProvider and checks whether an incoming SMS or call
 * number belongs to a saved contact (used for silent unknown-number filtering & call blocking).
 */
object ContactsLoader {

    fun hasContactsPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun loadDeviceContacts(context: Context): List<ContactItem> {
        if (!hasContactsPermission(context)) return emptyList()
        val seenNumbers = LinkedHashSet<String>()
        val list = mutableListOf<ContactItem>()

        try {
            val projection = arrayOf(
                ContactsContract.CommonDataKinds.Phone._ID,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER
            )
            context.contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                projection,
                null,
                null,
                "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} ASC"
            )?.use { cursor ->
                val idIdx = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone._ID)
                val nameIdx = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val numIdx = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER)

                while (cursor.moveToNext()) {
                    val rawNumber = cursor.getString(numIdx)?.trim().orEmpty()
                    val normalized = normalizePhone(rawNumber)
                    if (normalized.isEmpty() || !seenNumbers.add(normalized)) continue
                    val name = cursor.getString(nameIdx)?.trim().orEmpty().ifEmpty { rawNumber }
                    list.add(
                        ContactItem(
                            id = "sys_${cursor.getLong(idIdx)}",
                            name = name,
                            phoneNumber = rawNumber,
                            isLocalSaved = false
                        )
                    )
                }
            }
        } catch (_: Exception) {
            // Return whatever was collected safely
        }
        return list
    }

    private val normalizedPhoneCache = java.util.concurrent.ConcurrentHashMap<String, String>(4096)

    private val GENERIC_PLACEHOLDERS = setOf(
        "null",
        "(unknown)",
        "unknown",
        "none",
        "n/a",
        "-1",
        "insert-address-token",
        "namba geni (haijahifadhiwa)",
        "unknown number (unsaved)"
    )

    fun isGenericPlaceholder(raw: String?): Boolean {
        if (raw.isNullOrBlank()) return true
        return raw.trim().lowercase() in GENERIC_PLACEHOLDERS
    }

    fun cleanSenderIdentifier(raw: String?): String {
        if (raw.isNullOrBlank()) return ""
        var s = raw.trim()
        if (s.startsWith("tel:", ignoreCase = true)) {
            s = s.substring(4).trim()
        } else if (s.startsWith("sms:", ignoreCase = true)) {
            s = s.substring(4).trim()
        }
        return if (s.lowercase() in GENERIC_PLACEHOLDERS) "" else s
    }

    /**
     * Resolves a canonical grouping key for any SMS message whether it arrived by phone number ("namba")
     * or by sender name ("jina").
     */
    fun resolveCanonicalThreadKey(phoneNumber: String, contactName: String): String {
        val cleanPhone = cleanSenderIdentifier(phoneNumber)
        val cleanName = cleanSenderIdentifier(contactName)
        val primary = cleanPhone.ifEmpty { cleanName }
        if (primary.isEmpty()) return "UNKNOWN"
        return normalizePhone(primary).ifEmpty { primary.uppercase() }
    }

    /**
     * Returns true for alphanumeric sender IDs (e.g., "M-PESA", "VODACOM", "CRDB", "TIGO", ads/promos)
     * that contain letters so their exact sender name is always preserved and never collapsed.
     */
    fun isAlphanumericSender(raw: String): Boolean {
        if (raw.isEmpty()) return false
        val cleaned = cleanSenderIdentifier(raw)
        if (cleaned.isEmpty()) return false
        for (i in 0 until cleaned.length) {
            if (cleaned[i].isLetter()) return true
        }
        return false
    }

    /**
     * Resolves contact display name if the number is saved in device contacts, or null if unknown / unnamed.
     * Alphanumeric sender IDs (ads, banks, carriers) keep their original sender name directly.
     */
    fun lookupContactName(context: Context, phoneNumber: String): String? {
        val cleanedPhone = cleanSenderIdentifier(phoneNumber)
        if (cleanedPhone.isBlank() || isAlphanumericSender(cleanedPhone) || !hasContactsPermission(context)) {
            return null
        }
        return try {
            val uri = Uri.withAppendedPath(
                ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                Uri.encode(cleanedPhone)
            )
            context.contentResolver.query(
                uri,
                arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME),
                null,
                null,
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val name = cursor.getString(0)?.trim()
                    if (!name.isNullOrBlank() && !isGenericPlaceholder(name) && normalizePhone(name) != normalizePhone(cleanedPhone)) {
                        name
                    } else null
                } else null
            }
        } catch (_: Exception) {
            null
        }
    }

    fun normalizePhone(raw: String): String {
        if (raw.isEmpty()) return ""
        normalizedPhoneCache[raw]?.let { return it }
        val trimmed = cleanSenderIdentifier(raw)
        if (trimmed.isEmpty()) return ""
        val computed = if (isAlphanumericSender(trimmed)) {
            "alpha_${trimmed.uppercase()}"
        } else {
            val sb = StringBuilder(trimmed.length)
            for (i in 0 until trimmed.length) {
                val c = trimmed[i]
                if (c.isDigit()) sb.append(c)
            }
            val cleaned = sb.toString()
            if (cleaned.length >= 9) cleaned.substring(cleaned.length - 9) else cleaned.ifEmpty { trimmed }
        }
        if (normalizedPhoneCache.size < 25_000) {
            normalizedPhoneCache[raw] = computed
        }
        return computed
    }

    data class DetectedPhoneSpan(
        val rawMatch: String,
        val dialableNumber: String,
        val startIndex: Int,
        val endIndex: Int
    )

    // Matches phone numbers inside sent or received SMS bodies:
    // +255 712 345 678, 0712345678, 0655-123-456, (0754) 123 456, 0712 34 56 78, 255754123456, 712345678, +254..., +1..., 0800...
    private val EMBEDDED_PHONE_REGEX = Regex(
        """(?<![A-Za-z0-9/.,])(?:\+\d{1,3}[\s.-]?\(?\d{2,4}\)?[\s.-]?\d{2,4}[\s.-]?\d{2,4}(?:[\s.-]?\d{1,4})?|\(?0[126789]\d{1,2}\)?[\s.-]?\d{2,4}[\s.-]?\d{2,4}(?:[\s.-]?\d{1,4})?|(?:25[456][16789]\d)[\s.-]?\d{3}[\s.-]?\d{3,4}|\b[67]\d{2}[\s.-]?\d{3}[\s.-]?\d{3}\b|\b0800[\s.-]?\d{3}[\s.-]?\d{3}\b)(?![A-Za-z0-9/])"""
    )

    /**
     * Finds all phone number spans embedded inside a sent or received chat message body so they can
     * be highlighted, tapped directly in text, and acted on (Copy Number, Call Number, Send SMS, Save Contact).
     */
    fun extractPhoneNumberSpans(text: String): List<DetectedPhoneSpan> {
        if (text.length < 7) return emptyList()
        val results = ArrayList<DetectedPhoneSpan>(4)
        for (match in EMBEDDED_PHONE_REGEX.findAll(text)) {
            val raw = match.value.trim()
            val dialable = buildString(raw.length) {
                for (i in 0 until raw.length) {
                    val c = raw[i]
                    if (c.isDigit() || (c == '+' && isEmpty())) {
                        append(c)
                    }
                }
            }
            val digitCount = dialable.count { it.isDigit() }
            if (digitCount in 7..15) {
                val normalizedDialable = if (digitCount == 9 && (dialable.startsWith("6") || dialable.startsWith("7"))) {
                    "0$dialable"
                } else {
                    dialable
                }
                results.add(
                    DetectedPhoneSpan(
                        rawMatch = raw,
                        dialableNumber = normalizedDialable,
                        startIndex = match.range.first,
                        endIndex = match.range.last + 1
                    )
                )
            }
        }
        return results
    }

    /**
     * Returns distinct dialable phone numbers found inside an SMS message body.
     */
    fun extractPhoneNumbersFromText(text: String): List<String> {
        val spans = extractPhoneNumberSpans(text)
        if (spans.isEmpty()) return emptyList()
        val seen = LinkedHashSet<String>(spans.size)
        val out = ArrayList<String>(spans.size)
        for (span in spans) {
            val key = normalizePhone(span.dialableNumber).ifEmpty { span.dialableNumber }
            if (seen.add(key)) {
                out.add(span.dialableNumber)
            }
        }
        return out
    }

    /**
     * Loads complete Android ContactsContract details (all phone numbers, emails, company/job title,
     * postal address, notes, photo URI, starred state, and system lookup URI) for a contact.
     */
    fun loadFullContactDetails(
        context: Context,
        fallbackName: String,
        phoneNumber: String
    ): FullContactDetails {
        val cleanPhone = phoneNumber.trim()
        val cleanFallbackName = fallbackName.trim().ifBlank { cleanPhone }
        val isAlphanumeric = isAlphanumericSender(cleanPhone)

        if (cleanPhone.isBlank() || isAlphanumeric || !hasContactsPermission(context)) {
            return FullContactDetails(
                displayName = cleanFallbackName,
                primaryPhone = cleanPhone,
                contactLookupUri = null,
                photoUri = null,
                phones = if (cleanPhone.isNotBlank()) listOf(ContactPhoneEntry(cleanPhone, "")) else emptyList(),
                emails = emptyList(),
                organization = null,
                jobTitle = null,
                address = null,
                notes = null,
                isStarred = false,
                isSavedInSystem = false
            )
        }

        var contactId: Long? = null
        var lookupKey: String? = null
        var resolvedName: String = cleanFallbackName
        var photoUri: String? = null
        var isStarred = false

        runCatching {
            val lookupUri = Uri.withAppendedPath(
                ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                Uri.encode(cleanPhone)
            )
            val projection = arrayOf(
                ContactsContract.PhoneLookup.CONTACT_ID,
                ContactsContract.PhoneLookup.LOOKUP_KEY,
                ContactsContract.PhoneLookup.DISPLAY_NAME,
                ContactsContract.PhoneLookup.PHOTO_URI,
                ContactsContract.PhoneLookup.STARRED
            )
            context.contentResolver.query(lookupUri, projection, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    contactId = cursor.getLong(0)
                    lookupKey = cursor.getString(1)
                    val sysName = cursor.getString(2)?.trim()
                    if (!sysName.isNullOrBlank()) resolvedName = sysName
                    photoUri = cursor.getString(3)
                    isStarred = cursor.getInt(4) == 1
                }
            }
        }

        val resolvedContactId = contactId
        if (resolvedContactId == null) {
            return FullContactDetails(
                displayName = cleanFallbackName,
                primaryPhone = cleanPhone,
                contactLookupUri = null,
                photoUri = null,
                phones = listOf(ContactPhoneEntry(cleanPhone, "")),
                emails = emptyList(),
                organization = null,
                jobTitle = null,
                address = null,
                notes = null,
                isStarred = false,
                isSavedInSystem = false
            )
        }

        val phones = mutableListOf<ContactPhoneEntry>()
        val seenPhoneNorms = LinkedHashSet<String>()
        val emails = mutableListOf<ContactEmailEntry>()
        val seenEmails = LinkedHashSet<String>()
        var organization: String? = null
        var jobTitle: String? = null
        var address: String? = null
        var notes: String? = null

        runCatching {
            val dataProjection = arrayOf(
                ContactsContract.Data.MIMETYPE,
                ContactsContract.Data.DATA1,
                ContactsContract.Data.DATA2,
                ContactsContract.Data.DATA3,
                ContactsContract.Data.DATA4
            )
            context.contentResolver.query(
                ContactsContract.Data.CONTENT_URI,
                dataProjection,
                "${ContactsContract.Data.CONTACT_ID} = ?",
                arrayOf(resolvedContactId.toString()),
                null
            )?.use { cursor ->
                while (cursor.moveToNext()) {
                    val mime = cursor.getString(0) ?: continue
                    val data1 = cursor.getString(1)?.trim().orEmpty()
                    val data2Int = cursor.getInt(2)
                    val data3 = cursor.getString(3)?.trim().orEmpty()
                    val data4 = cursor.getString(4)?.trim().orEmpty()

                    when (mime) {
                        ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE -> {
                            if (data1.isNotBlank()) {
                                val norm = normalizePhone(data1)
                                if (seenPhoneNorms.add(norm.ifEmpty { data1 })) {
                                    val label = ContactsContract.CommonDataKinds.Phone.getTypeLabel(
                                        context.resources,
                                        data2Int,
                                        data3
                                    ).toString()
                                    phones.add(ContactPhoneEntry(data1, label))
                                }
                            }
                        }

                        ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE -> {
                            if (data1.isNotBlank() && seenEmails.add(data1.lowercase())) {
                                val label = ContactsContract.CommonDataKinds.Email.getTypeLabel(
                                    context.resources,
                                    data2Int,
                                    data3
                                ).toString()
                                emails.add(ContactEmailEntry(data1, label))
                            }
                        }

                        ContactsContract.CommonDataKinds.Organization.CONTENT_ITEM_TYPE -> {
                            if (organization.isNullOrBlank() && data1.isNotBlank()) organization = data1
                            if (jobTitle.isNullOrBlank() && data4.isNotBlank()) jobTitle = data4
                        }

                        ContactsContract.CommonDataKinds.StructuredPostal.CONTENT_ITEM_TYPE -> {
                            if (address.isNullOrBlank() && data1.isNotBlank()) address = data1
                        }

                        ContactsContract.CommonDataKinds.Note.CONTENT_ITEM_TYPE -> {
                            if (notes.isNullOrBlank() && data1.isNotBlank()) notes = data1
                        }
                    }
                }
            }
        }

        if (phones.isEmpty() && cleanPhone.isNotBlank()) {
            phones.add(ContactPhoneEntry(cleanPhone, ""))
        }

        val systemLookupUri = if (!lookupKey.isNullOrBlank()) {
            ContactsContract.Contacts.getLookupUri(resolvedContactId, lookupKey)
        } else {
            null
        }

        return FullContactDetails(
            displayName = resolvedName,
            primaryPhone = phones.firstOrNull()?.number ?: cleanPhone,
            contactLookupUri = systemLookupUri,
            photoUri = photoUri,
            phones = phones,
            emails = emails,
            organization = organization,
            jobTitle = jobTitle,
            address = address,
            notes = notes,
            isStarred = isStarred,
            isSavedInSystem = true
        )
    }
}

data class ContactPhoneEntry(
    val number: String,
    val typeLabel: String
)

data class ContactEmailEntry(
    val email: String,
    val typeLabel: String
)

data class FullContactDetails(
    val displayName: String,
    val primaryPhone: String,
    val contactLookupUri: Uri?,
    val photoUri: String?,
    val phones: List<ContactPhoneEntry>,
    val emails: List<ContactEmailEntry>,
    val organization: String?,
    val jobTitle: String?,
    val address: String?,
    val notes: String?,
    val isStarred: Boolean,
    val isSavedInSystem: Boolean
)

