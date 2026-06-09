package com.example.memostodowidget.data

import com.example.memostodowidget.domain.TodoItem
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class MemosApiClientTest {
    private lateinit var server: MockWebServer
    private lateinit var client: MemosApiClient

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        client = MemosApiClient(OkHttpClient())
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun archiveMemoUsesV1MemoIdAndRowStatusBody() {
        server.enqueue(jsonResponse("""{"name":"memos/123","rowStatus":"ARCHIVED"}"""))

        client.archiveMemo(settings(), todo("123"))

        val request = server.takeRequest()
        assertEquals("PATCH", request.method)
        assertEquals("/api/v1/memos/123", request.path)
        assertTrue(request.body.readUtf8().contains(""""rowStatus":"ARCHIVED""""))
    }

    @Test
    fun archiveMemoFallsBackWhenV1ReturnsHtml() {
        server.enqueue(htmlResponse())
        server.enqueue(jsonResponse("""{"id":123,"rowStatus":"ARCHIVED"}"""))

        client.archiveMemo(settings(), todo("123"))

        assertEquals("/api/v1/memos/123", server.takeRequest().path)
        assertEquals("/api/v1/memos/123", server.takeRequest().path)
    }

    @Test
    fun updateMemoContentUsesPatchBodyWithoutUpdateMaskFirst() {
        server.enqueue(jsonResponse("""{"name":"memos/123","content":"- [x] done"}"""))

        client.updateMemoContent(
            settings = settings(),
            memoId = "123",
            content = "- [x] done",
            state = "NORMAL",
            visibility = "PRIVATE"
        )

        val request = server.takeRequest()
        assertEquals("PATCH", request.method)
        assertEquals("/api/v1/memos/123", request.path)
        assertFalse(request.requestUrl?.queryParameterNames.orEmpty().contains("updateMask"))
        assertTrue(request.body.readUtf8().contains(""""content":"- [x] done""""))
    }

    @Test
    fun setMemoPinnedUsesV1PinnedPatchFirst() {
        server.enqueue(jsonResponse("""{"name":"memos/123","pinned":true}"""))

        client.setMemoPinned(settings(), todo("123"), true)

        val request = server.takeRequest()
        assertEquals("PATCH", request.method)
        assertEquals("/api/v1/memos/123", request.path)
        assertTrue(request.body.readUtf8().contains(""""pinned":true"""))
    }

    @Test
    fun setMemoPinnedFallsBackToLegacyOrganizer() {
        server.enqueue(htmlResponse())
        server.enqueue(htmlResponse())
        server.enqueue(jsonResponse("""{"id":123,"pinned":true}"""))

        client.setMemoPinned(settings(), todo("123"), true)

        assertEquals("/api/v1/memos/123", server.takeRequest().path)
        assertEquals("/api/v1/memos/123?updateMask=pinned%2Cupdate_time", server.takeRequest().path)
        val legacyRequest = server.takeRequest()
        assertEquals("POST", legacyRequest.method)
        assertEquals("/api/v1/memo/123/organizer", legacyRequest.path)
        assertTrue(legacyRequest.body.readUtf8().contains(""""pinned":true"""))
    }

    @Test
    fun fetchMemosPrefersNameAsApiIdOverUid() {
        server.enqueue(
            jsonResponse(
                """
                {
                  "memos": [
                    {
                      "name": "memos/123",
                      "uid": "public-uid",
                      "content": "- [ ] task",
                      "state": "NORMAL",
                      "createTime": "2026-06-08T00:00:00Z"
                    }
                  ]
                }
                """.trimIndent()
            )
        )

        val memos = client.fetchMemos(settings())

        assertEquals("123", memos.single().memoId)
        assertEquals("${baseUrl()}/m/public-uid", memos.single().webUrl)
    }

    @Test
    fun fetchArchivedMemosRequestsArchivedState() {
        server.enqueue(
            jsonResponse(
                """
                {
                  "memos": [
                    {
                      "name": "memos/archived",
                      "content": "done",
                      "state": "ARCHIVED"
                    }
                  ]
                }
                """.trimIndent()
            )
        )

        val memos = client.fetchArchivedMemos(settings())

        val request = server.takeRequest()
        assertEquals("/api/v1/memos?pageSize=100&state=ARCHIVED", request.path)
        assertEquals("archived", memos.single().memoId)
    }

    private fun settings(): MemosSettings =
        MemosSettings(serverUrl = baseUrl(), accessToken = "token")

    private fun baseUrl(): String = server.url("/").toString().trimEnd('/')

    private fun todo(id: String): TodoItem =
        TodoItem(
            id = id,
            memoId = id,
            content = "- [ ] task",
            rawMemoContent = "- [ ] task",
            taskLines = emptyList(),
            state = "NORMAL",
            visibility = "PRIVATE",
            pinned = false,
            createdAtEpochSeconds = 0L,
            webUrl = "${baseUrl()}/m/$id"
        )

    private fun jsonResponse(body: String): MockResponse =
        MockResponse()
            .setResponseCode(200)
            .setHeader("Content-Type", "application/json")
            .setBody(body)

    private fun htmlResponse(): MockResponse =
        MockResponse()
            .setResponseCode(200)
            .setHeader("Content-Type", "text/html")
            .setBody("<!doctype html><html></html>")
}
