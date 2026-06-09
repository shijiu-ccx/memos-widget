package com.example.memostodowidget.data

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MemosJsonParserTest {
    @Test
    fun parseMemosFiltersArchivedRows() {
        val memos = MemosJsonParser.parseMemos(
            """
            {
              "memos": [
                {"name":"memos/1","content":"active","state":"NORMAL"},
                {"name":"memos/2","content":"archived","state":"ARCHIVED"},
                {"id":3,"content":"legacy archived","rowStatus":"ARCHIVED"},
                {"uid":"uid-4","content":"visible","archived":false}
              ]
            }
            """.trimIndent(),
            SERVER_URL
        )

        assertEquals(listOf("1", "uid-4"), memos.map { it.memoId })
    }

    @Test
    fun parseMemoPrefersResourceNameForApiIdAndUidForWebUrl() {
        val memo = MemosJsonParser.parseMemo(
            JSONObject(
                """
                {
                  "name": "memos/123",
                  "uid": "public-uid",
                  "content": "- [ ] **task**",
                  "state": "NORMAL",
                  "visibility": "PRIVATE",
                  "pinned": true,
                  "createTime": "2026-06-08T00:00:00Z"
                }
                """.trimIndent()
            ),
            SERVER_URL
        )!!

        assertEquals("123", memo.memoId)
        assertEquals("$SERVER_URL/m/public-uid", memo.webUrl)
        assertEquals(true, memo.pinned)
        assertEquals(1, memo.taskLines.size)
        assertEquals("task", memo.taskLines.single().content)
        assertEquals(1_780_876_800L, memo.createdAtEpochSeconds)
    }

    @Test
    fun parseMemoReturnsNullWithoutAnyIdentifier() {
        val memo = MemosJsonParser.parseMemo(
            JSONObject("""{"content":"no id"}"""),
            SERVER_URL
        )

        assertNull(memo)
    }

    private companion object {
        const val SERVER_URL = "https://memos.example.com"
    }
}
