package io.stashapp.android.core.network

import com.sun.net.httpserver.HttpServer
import okhttp3.OkHttpClient
import okhttp3.Request
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.net.InetSocketAddress
import java.util.concurrent.atomic.AtomicReference

class StashAuthInterceptorTest {
    private val seenByStash = AtomicReference<String?>("unset")
    private val seenByOther = AtomicReference<String?>("unset")
    private lateinit var other: HttpServer
    private lateinit var stash: HttpServer
    private lateinit var base: String

    @BeforeEach
    fun setUp() {
        other =
            HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0).apply {
                createContext("/") {
                    seenByOther.set(it.requestHeaders.getFirst("ApiKey"))
                    it.sendResponseHeaders(200, -1)
                    it.close()
                }
                start()
            }
        val otherPort = other.address.port
        stash =
            HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0).apply {
                createContext("/stream") {
                    seenByStash.set(it.requestHeaders.getFirst("ApiKey"))
                    it.sendResponseHeaders(200, -1)
                    it.close()
                }
                createContext("/redirect") {
                    seenByStash.set(it.requestHeaders.getFirst("ApiKey"))
                    // Same host, different port => different origin.
                    it.responseHeaders.add("Location", "http://127.0.0.1:$otherPort/x")
                    it.sendResponseHeaders(302, -1)
                    it.close()
                }
                start()
            }
        base = "http://127.0.0.1:${stash.address.port}"
    }

    @AfterEach
    fun tearDown() {
        stash.stop(0)
        other.stop(0)
    }

    private fun client(apiKey: String?) =
        OkHttpClient
            .Builder()
            .addNetworkInterceptor(
                StashAuthInterceptor(
                    object : StashEndpointProvider {
                        override fun current() = StashEndpoint(base, apiKey)
                    },
                ),
            ).build()

    @Test
    fun `key is sent to the configured origin`() {
        client("SECRET").newCall(Request.Builder().url("$base/stream").build()).execute().close()
        assertEquals("SECRET", seenByStash.get())
    }

    @Test
    fun `key is not forwarded when a redirect leaves the origin`() {
        client("SECRET").newCall(Request.Builder().url("$base/redirect").build()).execute().close()
        assertEquals("SECRET", seenByStash.get())
        assertNull(seenByOther.get())
    }

    @Test
    fun `no header when no key is configured`() {
        client(null).newCall(Request.Builder().url("$base/stream").build()).execute().close()
        assertNull(seenByStash.get())
    }
}
