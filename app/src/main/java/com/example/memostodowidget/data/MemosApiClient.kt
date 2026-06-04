package com.example.memostodowidget.data

import com.example.memostodowidget.domain.TodoItem
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
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
                val content = memo.optString("content")
                if (!TODO_TAG.containsMatchIn(content)) continue

                val name = memo.optString("name")
                val uid = memo.optString("uid")
                val id = when {
                    uid.isNotBlank() -> uid
                    name.isNotBlank() -> name.substringAfterLast('/')
                    memo.has("id") -> memo.opt("id").toString()
                    else -> continue
                }
                add(
                    TodoItem(
                        id = id,
                        content = cleanTodoContent(content),
                        createdAtEpochSeconds = parseCreatedAt(memo),
                        webUrl = "$serverUrl/m/$id"
                    )
                )
            }
        }
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

    private fun cleanTodoContent(content: String): String =
        content.replace(TODO_TAG, "")
            .lineSequence()
            .joinToString(" ") { it.trim() }
            .replace(Regex("\\s+"), " ")
            .trim()
            .ifBlank { "#todo" }

    private companion object {
        val TODO_TAG = Regex("(?i)(?<![\\p{L}\\p{N}_])#todo(?![\\p{L}\\p{N}_])")
    }
}

