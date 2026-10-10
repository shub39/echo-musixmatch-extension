package dev.brahmkshatriya.echo.extension

import dev.brahmkshatriya.echo.common.models.Artist
import dev.brahmkshatriya.echo.common.models.Feed
import dev.brahmkshatriya.echo.common.models.Lyrics
import dev.brahmkshatriya.echo.common.models.Track
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(DelicateCoroutinesApi::class)
@ExperimentalCoroutinesApi
class ExtensionUnitTest {
    private val extension = MusixmatchExtension()

    private val mainThreadSurrogate = newSingleThreadContext("UI thread")

    @Before
    fun setUp() {
        Dispatchers.setMain(mainThreadSurrogate)
        extension.setSettings(MockedSettings())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        mainThreadSurrogate.close()
    }

    private fun testIn(title: String, block: suspend CoroutineScope.() -> Unit) = runBlocking {
        println("\n-- $title --")
        block.invoke(this)
        println("\n")
    }

    private suspend fun <T : Any> Feed<T>.loadAllItems(): List<T> {
        val data = getPagedData(null)
        return data.pagedData.loadAll()
    }

    @Test
    fun searchTest() = testIn("Search Test") {
        val results = extension.searchLyrics("Bohemian Rhapsody").loadAllItems()
        assertTrue("Expected search results for Bohemian Rhapsody", results.isNotEmpty())
        println("Found ${results.size} results:")
        results.take(3).forEach {
            println("- ${it.title} by ${it.subtitle} (ID: ${it.id})")
        }

        val first = results.first()
        val loaded = extension.loadLyrics(first)
        assertNotNull(loaded.lyrics)
        println("Loaded lyrics type: ${loaded.lyrics?.javaClass?.simpleName}")
        when (val l = loaded.lyrics) {
            is Lyrics.Timed -> {
                println("Timed lines count: ${l.list.size}")
                println("First line: ${l.list.firstOrNull()?.text} at ${l.list.firstOrNull()?.startTime}ms")
            }
            is Lyrics.WordByWord -> {
                println("WordByWord lines count: ${l.list.size}")
            }
            is Lyrics.Simple -> {
                println("Simple lyrics: ${l.text.take(100)}...")
            }
            else -> {}
        }
    }

    @Test
    fun searchTrackLyricsTest() = testIn("Search Track Lyrics Test") {
        val track = Track(
            id = "test_1",
            title = "Shape of You",
            artists = listOf(Artist("test_a1", "Ed Sheeran"))
        )
        val results = extension.searchTrackLyrics("test_client", track).loadAllItems()
        assertTrue("Expected search results for Ed Sheeran - Shape of You", results.isNotEmpty())
        val first = results.first()
        println("First match: ${first.title} by ${first.subtitle} (ID: ${first.id})")
        val loaded = extension.loadLyrics(first)
        assertNotNull(loaded.lyrics)
    }

    @Test
    fun noDojaCatHoneypotTest() = testIn("No Doja Cat Honeypot Test") {
        val results = extension.searchLyrics("Never Gonna Give You Up").loadAllItems()
        println("noDojaCatHoneypotTest results count: ${results.size}")
        results.forEach { println("   Result: ${it.title} by ${it.subtitle}") }
        assertTrue("Results should not be empty", results.isNotEmpty())
        for (item in results) {
            assertFalse(
                "Should not contain honeypot 'Casual by Doja Cat'",
                item.title.equals("Casual", ignoreCase = true) && (item.subtitle?.equals("Doja Cat", ignoreCase = true) == true)
            )
        }
    }
}
