package com.example.memostodowidget.data

import com.example.memostodowidget.domain.TodoItem
import com.example.memostodowidget.domain.TodoTaskLine
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant

class MemosApiClient(private val client: OkHttpClient) {
    fun fetchMemos(settings: MemosSettings): List<TodoItem> {
        val v1Result = runCatching {
            requestAndParse(settings, "/api/v1/memos", "pageSize", "100")
        }
        return v1Result.getOrElse {
            requestAndParse(settings, "/api/memo", "limit", "100")
        }
    }

    fun updateMemoContent(
        settings: MemosSettings,
        memoId: String,
        content: String,
        state: String,
        visibility: String
    ) {
        val attempts = listOf(
            UpdateAttempt("/api/v1/memos/$memoId", true, true),
            UpdateAttempt("/api/v1/memo/$memoId", false, false),
            UpdateAttempt("/api/memo/$memoId", false, false)
        )

        var lastError: Throwable? = null
        for (attempt in attempts) {
            val result = runCatching {
                patchMemoContent(
                    settings = settings,
                    path = attempt.path,
                    content = content,
                    state = state,
                    visibility = visibility,
                    updateMask = attempt.updateMask,
                    includeResourceName = attempt.includeResourceName
                )
            }
            if (result.isSuccess) return
            lastError = result.exceptionOrNull()
        }
        throw lastError ?: IllegalStateException("Memos API 更新失败")
    }

    private fun requestAndParse(
        settings: MemosSettings,
        path: String,
        limitKey: String,
        limitValue: String
    ): List<TodoItem> {
        val baseUrl = settings.serverUrl.toHttpUrl()
        val url = baseUrl.newBuilder()
            .addPathSegments(path.removePrefix("/"))
            .addQueryParameter(limitKey, limitValue)
            .build()

        val request = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer ${settings.accessToken}")
            .header("Accept", "application/json")
            .get()
            .build()

        client.newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()
            check(response.isSuccessful) {
                "Memos API 请求失败：HTTP ${response.code}"
            }
            return parseMemos(body, settings.serverUrl)
        }
    }

    private fun patchMemoContent(
        settings: MemosSettings,
        path: String,
        content: String,
        state: String,
        visibility: String,
        updateMask: Boolean,
        includeResourceName: Boolean
    ) {
        val baseUrl = settings.serverUrl.toHttpUrl()
        val urlBuilder = baseUrl.newBuilder()
            .addPathSegments(path.removePrefix("/"))
        if (updateMask) {
            urlBuilder.addQueryParameter("updateMask", "content")
        }

        val json = JSONObject().apply {
            if (includeResourceName) {
                put("name", "memos/${path.substringAfterLast('/')}")
                put("state", state.ifBlank { "NORMAL" })
                put("visibility", visibility.ifBlank { "PRIVATE" })
            }
            put("content", content)
        }
        val body = json
            .toString()
            .toRequestBody(JSON_MEDIA_TYPE)

        val request = Request.Builder()
            .url(urlBuilder.build())
            .header("Authorization", "Bearer ${settings.accessToken}")
            .header("Accept", "application/json")
            .header("Content-Type", "application/json")
            .patch(body)
            .build()

        client.newCall(request).execute().use { response ->
            check(response.isSuccessful) {
                "Memos API 更新失败：HTTP ${response.code}"
            }
        }
    }

    private fun parseMemos(json: String, serverUrl: String): List<TodoItem> {
        val trimmed = json.trim()
        val memos = when {
            trimmed.startsWith("[") -> JSONArray(trimmed)
            trimmed.startsWith("{") -> JSONObject(trimmed).optJSONArray("memos") ?: JSONArray()
            else -> JSONArray()
        }

        return buildList {
            for (index in 0 until memos.length()) {
                val memo = memos.optJSONObject(index) ?: continue
                if (memo.isArchived()) continue

                val content = memo.optString("content")

                val name = memo.optString("name")
                val uid = memo.optString("uid")
                val id = when {
                    uid.isNotBlank() -> uid
                    name.isNotBlank() -> name.substringAfterLast('/')
                    memo.has("id") -> memo.opt("id")?.toString().orEmpty()
                    else -> continue
                }
                val createdAt = parseCreatedAt(memo)
                val webUrl = "$serverUrl/m/$id"
                add(
                    TodoItem(
                        id = id,
                        memoId = id,
                        content = cleanMarkdownPreview(content),
                        rawMemoContent = content,
                        taskLines = parseCheckboxItems(content),
                        state = memo.optString("state").ifBlank { memo.optString("rowStatus") },
                        visibility = memo.optString("visibility"),
                        createdAtEpochSeconds = createdAt,
                        webUrl = webUrl
                    )
                )
            }
        }
    }

    private fun parseCheckboxItems(content: String): List<TodoTaskLine> =
        content.lines().mapIndexedNotNull { lineIndex, line ->
            val match = CHECKBOX_LINE.find(line) ?: return@mapIndexedNotNull null
            val checked = match.groupValues[1].equals("x", ignoreCase = true)
            val text = line.substring(match.range.last + 1).trim().ifBlank { "(空待办)" }
            TodoTaskLine(
                lineIndex = lineIndex,
                content = cleanInlineMarkdown(text),
                isChecked = checked
            )
        }

    private fun JSONObject.isArchived(): Boolean {
        val statusValues = listOf(
            optString("rowStatus"),
            optString("state"),
            optString("status"),
            optString("visibility")
        )
        return statusValues.any { it.equals("ARCHIVED", ignoreCase = true) }
            || optBoolean("archived", false)
    }

    private fun parseCreatedAt(memo: JSONObject): Long {
        val timestamp = memo.opt("createTime") ?: memo.opt("createdTs") ?: return 0L
        return when (timestamp) {
            is Number -> timestamp.toLong()
            is String -> timestamp.toLongOrNull()
                ?: runCatching { Instant.parse(timestamp).epochSecond }.getOrDefault(0L)
            else -> 0L
        }
    }

    private fun cleanMarkdownPreview(content: String): String =
        content
            .lineSequence()
            .joinToString(" ") { it.trim() }
            .let(::cleanInlineMarkdown)
            .replace(Regex("\\s+"), " ")
            .trim()
            .ifBlank { "(空 memo)" }

    private fun cleanInlineMarkdown(content: String): String =
        content.replace(Regex("`([^`]+)`"), "$1")
            .replace(Regex("\\*\\*([^*]+)\\*\\*"), "$1")
            .replace(Regex("__([^_]+)__"), "$1")
            .replace(Regex("\\[([^]]+)]\\([^)]+\\)"), "$1")
            .trim()

    private companion object {
        val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
        val CHECKBOX_LINE = Regex("^\\s*(?:[-*+]\\s+)?\\[([ xX])\\]\\s+")
    }

    private data class UpdateAttempt(
        val path: String,
        val updateMask: Boolean,
        val includeResourceName: Boolean
    )
}
