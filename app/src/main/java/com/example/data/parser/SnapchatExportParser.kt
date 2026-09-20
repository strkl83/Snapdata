package com.example.data.parser

import com.example.data.entity.SnapArchiveEntity
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.UUID

class SnapchatExportParser {

    fun parseDateToEpochMillis(dateStr: String?): Long {
        if (dateStr == null || dateStr.trim().isEmpty()) return System.currentTimeMillis()
        val clean = dateStr.replace(" UTC", "").trim()

        val formats = listOf(
            "yyyy-MM-dd HH:mm:ss",
            "yyyy-MM-dd'T'HH:mm:ss'Z'",
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
            "yyyy-MM-dd HH:mm:ss z",
            "yyyy-MM-dd"
        )

        for (fmt in formats) {
            try {
                val sdf = SimpleDateFormat(fmt, Locale.US)
                sdf.timeZone = TimeZone.getTimeZone("UTC")
                val parsed = sdf.parse(clean)
                if (parsed != null) return parsed.time
            } catch (_: Exception) { }
        }
        return System.currentTimeMillis()
    }

    private fun String?.isNull_or_blank(): Boolean {
        return this == null || this.trim().isEmpty()
    }

    fun parseMemoriesJson(jsonString: String): List<SnapArchiveEntity> {
        val result = mutableListOf<SnapArchiveEntity>()
        try {
            val root = JSONObject(jsonString)
            val memoriesArray = root.optJSONArray("Saved Media") ?: root.optJSONArray("Memories")
            if (memoriesArray != null) {
                for (i in 0 until memoriesArray.length()) {
                    val item = memoriesArray.getJSONObject(i)
                    val dateStr = item.optString("Date")
                    val ts = parseDateToEpochMillis(dateStr)
                    val mediaTypeStr = item.optString("Media Type", "PHOTO").uppercase(Locale.ROOT)
                    val mediaType = if (mediaTypeStr.contains("VIDEO")) "VIDEO" else "PHOTO"
                    val location = item.optString("Location", "Memories Vault")
                    val downloadLink = item.optString("Download Link", "memories/memory_$ts.jpg")

                    result.add(
                        SnapArchiveEntity(
                            id = "mem_${UUID.randomUUID().toString().take(8)}_$ts",
                            mediaType = mediaType,
                            source = "MEMORIES",
                            sender = "Memories",
                            content = "Saved Memory ($location)",
                            timestamp = ts,
                            mediaUrl = downloadLink,
                            localPath = null,
                            isBrokenLink = true, // Initial un-linked state
                            location = location,
                            conversationId = "memories"
                        )
                    )
                }
            }
        } catch (_: Exception) { }
        return result
    }

    fun parseChatHistoryJson(jsonString: String): List<SnapArchiveEntity> {
        val result = mutableListOf<SnapArchiveEntity>()
        try {
            val root = JSONObject(jsonString)

            // Received Chat
            val received = root.optJSONArray("Received Saved Chat History") ?: root.optJSONArray("Received Chat History")
            if (received != null) {
                for (i in 0 until received.length()) {
                    val item = received.getJSONObject(i)
                    val sender = item.optString("From", item.optString("Sender", "Friend"))
                    val dateStr = item.optString("Created", item.optString("Date", ""))
                    val ts = parseDateToEpochMillis(dateStr)
                    val text = item.optString("Text", item.optString("Media Type", "Saved Chat"))
                    val mediaType = if (item.has("Media Type") && item.getString("Media Type") != "TEXT") "PHOTO" else "TEXT_CHAT"

                    result.add(
                        SnapArchiveEntity(
                            id = "chat_recv_${UUID.randomUUID().toString().take(8)}_$ts",
                            mediaType = mediaType,
                            source = "CHAT_RECEIVED",
                            sender = sender,
                            content = text,
                            timestamp = ts,
                            mediaUrl = "chat_media/${sender.lowercase(Locale.ROOT)}/msg_$ts.jpg",
                            localPath = null,
                            isBrokenLink = mediaType != "TEXT_CHAT",
                            conversationId = sender
                        )
                    )
                }
            }

            // Sent Chat
            val sent = root.optJSONArray("Sent Saved Chat History") ?: root.optJSONArray("Sent Chat History")
            if (sent != null) {
                for (i in 0 until sent.length()) {
                    val item = sent.getJSONObject(i)
                    val recipient = item.optString("To", "Friend")
                    val dateStr = item.optString("Created", item.optString("Date", ""))
                    val ts = parseDateToEpochMillis(dateStr)
                    val text = item.optString("Text", "Sent Chat")

                    result.add(
                        SnapArchiveEntity(
                            id = "chat_sent_${UUID.randomUUID().toString().take(8)}_$ts",
                            mediaType = "TEXT_CHAT",
                            source = "CHAT_SENT",
                            sender = "Me",
                            content = text,
                            timestamp = ts,
                            mediaUrl = "",
                            localPath = null,
                            isBrokenLink = false,
                            conversationId = recipient
                        )
                    )
                }
            }
        } catch (_: Exception) { }
        return result
    }

    fun generateRealisticSampleArchive(): List<SnapArchiveEntity> {
        val baseTime = System.currentTimeMillis() - (180L * 24 * 3600 * 1000) // 6 months ago
        val dayMillis = 24 * 3600 * 1000L
        val hourMillis = 3600 * 1000L

        val list = mutableListOf<SnapArchiveEntity>()

        // 1. Memories
        val memoryLocations = listOf(
            "Eiffel Tower, Paris" to "PHOTO",
            "Shibuya Crossing, Tokyo" to "VIDEO",
            "Malibu Beach Sunset" to "PHOTO",
            "Yosemite Valley Hike" to "VIDEO",
            "New York Times Square" to "PHOTO",
            "Grand Canyon Lookout" to "PHOTO",
            "Kyoto Bamboo Forest" to "VIDEO",
            "Santorini Coastline" to "PHOTO"
        )

        memoryLocations.forEachIndexed { index, (loc, type) ->
            val ts = baseTime + (index * 12 * dayMillis) + (index * 3 * hourMillis)
            val isFixedSample = index % 2 == 0
            list.add(
                SnapArchiveEntity(
                    id = "mem_$index",
                    mediaType = type,
                    source = "MEMORIES",
                    sender = "Memories",
                    content = "Snapchat Memory captured at $loc",
                    timestamp = ts,
                    mediaUrl = "memories/2026-03-${10 + index}_${12 + index}-30-00.jpg",
                    localPath = if (isFixedSample) "sample_cache/memory_$index.jpg" else null,
                    isBrokenLink = !isFixedSample,
                    location = loc,
                    isFavorite = index % 3 == 0,
                    conversationId = "memories",
                    fileSize = if (isFixedSample) 2_450_000L else 0L,
                    resolution = "1080x1920"
                )
            )
        }

        // 2. Chat Threads with Sarah Miller
        val sarahThread = listOf(
            Triple("Hey!! Did you get the photos from last night's party? 🎉", "Sarah Miller", false),
            Triple("Yes!! Just checking out the Snapchat archive right now!", "Me", false),
            Triple("Check out this snap video from the concert ground!", "Sarah Miller", true),
            Triple("Haha that was insane 🔥 Send me the overlay stickered version!", "Me", false),
            Triple("Here it is! Saved to chat memories.", "Sarah Miller", true),
            Triple("Thanks Sarah! Link restored perfectly.", "Me", false)
        )

        sarahThread.forEachIndexed { i, (txt, sender, isMedia) ->
            val ts = baseTime + (100 * dayMillis) + (i * 15 * 60 * 1000L)
            list.add(
                SnapArchiveEntity(
                    id = "sarah_$i",
                    mediaType = if (isMedia) "SNAP_RECEIVED" else "TEXT_CHAT",
                    source = if (sender == "Me") "CHAT_SENT" else "CHAT_RECEIVED",
                    sender = sender,
                    content = txt,
                    timestamp = ts,
                    mediaUrl = if (isMedia) "chat_media/sarah/2026-06-15_18-${10 + i}-00.jpg" else "",
                    localPath = if (isMedia && i == 2) "sample_cache/sarah_snap.jpg" else null,
                    isBrokenLink = isMedia && i != 2,
                    location = if (isMedia) "Los Angeles, CA" else null,
                    isFavorite = i == 2,
                    conversationId = "Sarah Miller",
                    fileSize = if (isMedia) 1_850_000L else 0L
                )
            )
        }

        // 3. Chat Threads with Alex Chen
        val alexThread = listOf(
            Triple("Yo man, did you optimize those database indexes yet?", "Alex Chen", false),
            Triple("Working on the Snapchat export archive Room queries now!", "Me", false),
            Triple("Awesome! Room composite indexes give sub-5ms query response times.", "Alex Chen", false),
            Triple("Sending over the snap photo snippet of the query plan execution.", "Alex Chen", true),
            Triple("Got it! Load times are blazing fast now 🚀", "Me", false)
        )

        alexThread.forEachIndexed { i, (txt, sender, isMedia) ->
            val ts = baseTime + (120 * dayMillis) + (i * 20 * 60 * 1000L)
            list.add(
                SnapArchiveEntity(
                    id = "alex_$i",
                    mediaType = if (isMedia) "SNAP_RECEIVED" else "TEXT_CHAT",
                    source = if (sender == "Me") "CHAT_SENT" else "CHAT_RECEIVED",
                    sender = sender,
                    content = txt,
                    timestamp = ts,
                    mediaUrl = if (isMedia) "chat_media/alex/2026-07-01_14-${10 + i}-00.jpg" else "",
                    localPath = null,
                    isBrokenLink = isMedia,
                    location = null,
                    isFavorite = false,
                    conversationId = "Alex Chen"
                )
            )
        }

        // 4. Snaps & Overlays from Maya Lin & Team Snapchat
        val extraSnaps = listOf(
            SnapArchiveEntity(
                id = "maya_snap_1",
                mediaType = "OVERLAY",
                source = "SNAP_HISTORY",
                sender = "Maya Lin",
                content = "Filter & Sticker Overlay on Golden Hour Photo",
                timestamp = baseTime + (140 * dayMillis),
                mediaUrl = "snaps/overlays/2026-07-20_19-00-00_Maya.png",
                localPath = null,
                isBrokenLink = true,
                location = "San Francisco, CA",
                conversationId = "Maya Lin"
            ),
            SnapArchiveEntity(
                id = "team_snap_1",
                mediaType = "SNAP_RECEIVED",
                source = "SNAP_HISTORY",
                sender = "Team Snapchat",
                content = "Welcome to Snapchat My Data Backup Archive!",
                timestamp = baseTime + (150 * dayMillis),
                mediaUrl = "snaps/official/welcome_snap.jpg",
                localPath = "sample_cache/welcome.jpg",
                isBrokenLink = false,
                location = "Snap Inc.",
                isFavorite = true,
                conversationId = "Team Snapchat",
                fileSize = 980_000L
            )
        )

        list.addAll(extraSnaps)

        return list.sortedByDescending { it.timestamp }
    }
}
