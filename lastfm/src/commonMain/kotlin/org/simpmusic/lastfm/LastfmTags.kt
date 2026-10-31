package org.simpmusic.lastfm

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/** One tag of a track. [count] is Last.fm's own 0..100 ranking, 100 being the top tag. */
data class LastfmTag(
    val name: String,
    val count: Int?,
)

/** What `track.getTopTags` can come back as. Read-only: no session and no secret involved. */
sealed interface LastfmTagsOutcome {
    data class Tags(
        val tags: List<LastfmTag>,
    ) : LastfmTagsOutcome

    /** Last.fm answered and does not know the track, or the track has no tags. Final. */
    data object NotFound : LastfmTagsOutcome

    /** Not worth asking again right now: offline, rate limited (29), temporary failure (16), 5xx. */
    data class Temporary(
        val message: String,
    ) : LastfmTagsOutcome

    /** The API key is missing or refused (10, 26). Retrying cannot help. */
    data class Rejected(
        val message: String,
    ) : LastfmTagsOutcome
}

private val lenientJson = Json { ignoreUnknownKeys = true }

/**
 * Reads a `track.getTopTags` response.
 *
 * Kept free of any HTTP type so it can be tested with plain strings. Last.fm's JSON is a
 * translation of XML: `tag` is an array with several tags, a bare object with one, and an empty
 * string or absent with none, and `count` arrives as a string.
 */
internal fun parseTopTags(
    httpStatus: Int,
    body: String,
): LastfmTagsOutcome {
    if (httpStatus == 429 || httpStatus >= 500) return LastfmTagsOutcome.Temporary("HTTP $httpStatus")
    val root: JsonObject =
        runCatching { lenientJson.parseToJsonElement(body).jsonObject }.getOrNull()
            ?: return LastfmTagsOutcome.Temporary("Unreadable response (HTTP $httpStatus)")

    val code = root["error"]?.jsonPrimitive?.content?.toIntOrNull()
    if (code != null) {
        val message = root["message"]?.jsonPrimitive?.content ?: "Last.fm error $code"
        return when (code) {
            6 -> LastfmTagsOutcome.NotFound
            10, 26 -> LastfmTagsOutcome.Rejected(message)
            else -> LastfmTagsOutcome.Temporary("Last.fm error $code: $message")
        }
    }
    if (httpStatus >= 400) return LastfmTagsOutcome.Temporary("HTTP $httpStatus")

    val tagElement: JsonElement? = (root["toptags"] as? JsonObject)?.get("tag")
    val entries: List<JsonObject> =
        when (tagElement) {
            is JsonArray -> tagElement.mapNotNull { it as? JsonObject }
            is JsonObject -> listOf(tagElement)
            else -> emptyList()
        }
    val tags =
        entries.mapNotNull { entry ->
            val name = entry["name"]?.jsonPrimitive?.content?.trim().orEmpty()
            if (name.isEmpty()) {
                null
            } else {
                LastfmTag(name, entry["count"]?.jsonPrimitive?.content?.toIntOrNull())
            }
        }
    return if (tags.isEmpty()) LastfmTagsOutcome.NotFound else LastfmTagsOutcome.Tags(tags)
}
