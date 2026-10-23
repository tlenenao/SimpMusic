package org.simpmusic.lastfm

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class LastfmTagsTest {
    @Test
    fun parsesSeveralTagsWithStringCounts() {
        val out =
            parseTopTags(
                200,
                """{"toptags":{"tag":[{"count":100,"name":"rock","url":"x"},{"count":"60","name":"alternative rock"}],"@attr":{"artist":"a"}}}""",
            )
        assertEquals(listOf(LastfmTag("rock", 100), LastfmTag("alternative rock", 60)), assertIs<LastfmTagsOutcome.Tags>(out).tags)
    }

    @Test
    fun singleTagComesAsABareObject() {
        val out = parseTopTags(200, """{"toptags":{"tag":{"count":"88","name":"jazz"}}}""")
        assertEquals(listOf(LastfmTag("jazz", 88)), assertIs<LastfmTagsOutcome.Tags>(out).tags)
    }

    @Test
    fun noTagsIsNotFoundNotAnError() {
        assertEquals(LastfmTagsOutcome.NotFound, parseTopTags(200, """{"toptags":{"tag":[],"@attr":{}}}"""))
        assertEquals(LastfmTagsOutcome.NotFound, parseTopTags(200, """{"toptags":{"tag":""}}"""))
        assertEquals(LastfmTagsOutcome.NotFound, parseTopTags(200, """{"toptags":{}}"""))
        assertEquals(LastfmTagsOutcome.NotFound, parseTopTags(200, """{"toptags":{"tag":[{"name":"  "}]}}"""))
    }

    @Test
    fun trackNotFoundIsFinal() {
        assertEquals(LastfmTagsOutcome.NotFound, parseTopTags(200, """{"error":6,"message":"Track not found"}"""))
    }

    @Test
    fun rateLimitAndTemporaryFailuresAreRetryable() {
        assertIs<LastfmTagsOutcome.Temporary>(parseTopTags(200, """{"error":29,"message":"Rate limit exceeded"}"""))
        assertIs<LastfmTagsOutcome.Temporary>(parseTopTags(200, """{"error":16,"message":"temporarily unavailable"}"""))
        assertIs<LastfmTagsOutcome.Temporary>(parseTopTags(503, "<html>down</html>"))
        assertIs<LastfmTagsOutcome.Temporary>(parseTopTags(429, ""))
        assertIs<LastfmTagsOutcome.Temporary>(parseTopTags(200, "not json"))
    }

    @Test
    fun badKeyIsRejectedNotRetried() {
        assertIs<LastfmTagsOutcome.Rejected>(parseTopTags(403, """{"error":10,"message":"Invalid API key"}"""))
        assertIs<LastfmTagsOutcome.Rejected>(parseTopTags(403, """{"error":26,"message":"Suspended API key"}"""))
    }
}
