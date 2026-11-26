package com.example.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject

data class MessageReactionEntry(
    val emoji: String,
    val reactedAtMillis: Long
)

/**
 * Persistent thread-safe Emoji Reaction store for SMS messages inside Conversation Chat.
 * Backed by a fast SharedPreferences JSON map so tapping an emoji attaches a reaction badge
 * in 0ms without altering Room database schema or migration compatibility.
 */
object MessageReactionStore {

    private const val PREFS_NAME = "mengitex_sms_reactions_v1"
    private const val KEY_REACTIONS_JSON = "reactions_map_json"
    private const val MAX_STORED_REACTIONS = 5000

    private val writeLock = Any()

    @Volatile
    private var hasLoadedFromDisk = false

    private val _reactions = MutableStateFlow<Map<Long, MessageReactionEntry>>(emptyMap())
    val reactions: StateFlow<Map<Long, MessageReactionEntry>> = _reactions.asStateFlow()

    fun ensureLoaded(context: Context) {
        if (hasLoadedFromDisk) return
        synchronized(writeLock) {
            if (hasLoadedFromDisk) return
            val appCtx = context.applicationContext ?: context
            val parsed = runCatching {
                val raw = appCtx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                    .getString(KEY_REACTIONS_JSON, null)
                if (raw.isNullOrBlank()) {
                    emptyMap()
                } else {
                    val json = JSONObject(raw)
                    val out = LinkedHashMap<Long, MessageReactionEntry>(json.length())
                    val keys = json.keys()
                    while (keys.hasNext()) {
                        val k = keys.next()
                        val msgId = k.toLongOrNull() ?: continue
                        val obj = json.optJSONObject(k) ?: continue
                        val emoji = obj.optString("e", "").trim()
                        val ts = obj.optLong("t", 0L)
                        if (emoji.isNotEmpty()) {
                            out[msgId] = MessageReactionEntry(emoji = emoji, reactedAtMillis = ts)
                        }
                    }
                    out
                }
            }.getOrDefault(emptyMap())
            _reactions.value = parsed
            hasLoadedFromDisk = true
        }
    }

    /**
     * Toggles [emoji] on [messageId]: if already reacted with the same emoji, removes it;
     * otherwise attaches/updates it immediately and returns the resulting emoji (or null if removed).
     */
    fun toggleReaction(context: Context, messageId: Long, emoji: String): String? {
        val cleanEmoji = emoji.trim()
        if (messageId <= 0L || cleanEmoji.isEmpty()) return null
        ensureLoaded(context)
        val appCtx = context.applicationContext ?: context
        synchronized(writeLock) {
            val current = _reactions.value
            val existing = current[messageId]
            val next = LinkedHashMap(current)
            val resultEmoji: String?
            if (existing != null && existing.emoji == cleanEmoji) {
                next.remove(messageId)
                resultEmoji = null
            } else {
                next[messageId] = MessageReactionEntry(
                    emoji = cleanEmoji,
                    reactedAtMillis = System.currentTimeMillis()
                )
                resultEmoji = cleanEmoji
            }
            trimIfOversized(next)
            _reactions.value = next
            persistLocked(appCtx, next)
            return resultEmoji
        }
    }

    fun setReaction(context: Context, messageId: Long, emoji: String) {
        val cleanEmoji = emoji.trim()
        if (messageId <= 0L || cleanEmoji.isEmpty()) return
        ensureLoaded(context)
        val appCtx = context.applicationContext ?: context
        synchronized(writeLock) {
            val next = LinkedHashMap(_reactions.value)
            next[messageId] = MessageReactionEntry(
                emoji = cleanEmoji,
                reactedAtMillis = System.currentTimeMillis()
            )
            trimIfOversized(next)
            _reactions.value = next
            persistLocked(appCtx, next)
        }
    }

    fun removeReaction(context: Context, messageId: Long) {
        if (messageId <= 0L) return
        ensureLoaded(context)
        val appCtx = context.applicationContext ?: context
        synchronized(writeLock) {
            val current = _reactions.value
            if (!current.containsKey(messageId)) return
            val next = LinkedHashMap(current)
            next.remove(messageId)
            _reactions.value = next
            persistLocked(appCtx, next)
        }
    }

    fun clearAll(context: Context) {
        val appCtx = context.applicationContext ?: context
        synchronized(writeLock) {
            _reactions.value = emptyMap()
            hasLoadedFromDisk = true
            runCatching {
                appCtx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                    .edit()
                    .remove(KEY_REACTIONS_JSON)
                    .apply()
            }
        }
    }

    private fun trimIfOversized(map: LinkedHashMap<Long, MessageReactionEntry>) {
        if (map.size <= MAX_STORED_REACTIONS) return
        val sortedOldestFirst = map.entries.sortedBy { it.value.reactedAtMillis }
        val toDrop = map.size - MAX_STORED_REACTIONS
        for (i in 0 until toDrop) {
            map.remove(sortedOldestFirst[i].key)
        }
    }

    private fun persistLocked(context: Context, map: Map<Long, MessageReactionEntry>) {
        runCatching {
            val json = JSONObject()
            for ((msgId, entry) in map) {
                val obj = JSONObject()
                    .put("e", entry.emoji)
                    .put("t", entry.reactedAtMillis)
                json.put(msgId.toString(), obj)
            }
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_REACTIONS_JSON, json.toString())
                .apply()
        }
    }
}
