package com.example.memostodowidget.data

import android.util.Log
import com.example.memostodowidget.domain.TodoItem
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.time.Instant

class MemosApiClient(private val client: OkHttpClient) {
    fun signIn(serverUrl: String, username: String, password: String): String {
        val attempts = listOf(
            JSONObject().put(
                "passwordCredentials",
                JSONObject()
                    .put("username", username)
                    .put("password", password)
            ),
            JSONObject()
                .put("username", username)
                .put("password", password)
        )

        var lastError: Throwable? = null
        for (payload in attempts) {
            val result = runCatching {
                logDebug("signIn attempt path=/api/v1/auth/signin payloadKeys=${payload.keys().asSequence().toList()}")
                postForJson(
                    serverUrl = serverUrl,
                    path = "/api/v1/auth/signin",
                    accessToken = "",
                    payload = payload
                )
            }
            val response = result.getOrNull()
            val token = response?.optString("accessToken").orEmpty()
            if (token.isNotBlank()) return token
            lastError = result.exceptionOrNull()
            logWarning("signIn attempt failed", lastError)
        }

        throw lastError ?: IllegalStateException("登录失败，请检查账号或密码")
    }

    fun fetchMemos(settings: MemosSettings): List<TodoItem> {
        val v1Result = runCatching {
            requestAndParse(
                settings = settings,
                path = "/api/v1/memos",
                limitKey = "pageSize",
                limitValue = "100",
                archiveFilter = MemosJsonParser.ArchiveFilter.ActiveOnly
            )
        }
        return v1Result.getOrElse {
            requestAndParse(
                settings = settings,
                path = "/api/memo",
                limitKey = "limit",
                limitValue = "100",
                archiveFilter = MemosJsonParser.ArchiveFilter.ActiveOnly
            )
        }
    }

    fun fetchArchivedMemos(settings: MemosSettings): List<TodoItem> {
        val v1Result = runCatching {
            requestAndParse(
                settings = settings,
                path = "/api/v1/memos",
                limitKey = "pageSize",
                limitValue = "100",
                archiveFilter = MemosJsonParser.ArchiveFilter.ArchivedOnly,
                queryParameters = mapOf("state" to "ARCHIVED")
            )
        }
        return v1Result.getOrElse {
            requestAndParse(
                settings = settings,
                path = "/api/memo",
                limitKey = "limit",
                limitValue = "100",
                archiveFilter = MemosJsonParser.ArchiveFilter.ArchivedOnly,
                queryParameters = mapOf("rowStatus" to "ARCHIVED")
            )
        }
    }

    fun createMemo(settings: MemosSettings, content: String): TodoItem {
        val attempts = listOf(
            CreateAttempt(
                path = "/api/v1/memos",
                payload = JSONObject()
                    .put("content", content)
                    .put("visibility", "PRIVATE")
            ),
            CreateAttempt(
                path = "/api/memo",
                payload = JSONObject()
                    .put("content", content)
            )
        )

        var lastError: Throwable? = null
        for (attempt in attempts) {
            val result = runCatching {
                val response = postForJson(
                    serverUrl = settings.serverUrl,
                    path = attempt.path,
                    accessToken = settings.accessToken,
                    payload = attempt.payload
                )
                MemosJsonParser.parseMemo(response, settings.serverUrl)
            }
            result.getOrNull()?.let { return it }
            lastError = result.exceptionOrNull()
        }

        throw lastError ?: IllegalStateException("创建 memo 失败")
    }

    fun archiveMemo(settings: MemosSettings, memo: TodoItem) {
        val archiveTime = Instant.now().toString()
        val attempts = listOf(
            ArchiveAttempt(
                path = "/api/v1/memos/${memo.memoId}",
                payload = JSONObject()
                    .put("rowStatus", "ARCHIVED")
                    .put("updateTime", archiveTime),
                updateMask = null
            ),
            ArchiveAttempt(
                path = "/api/v1/memos/${memo.memoId}",
                payload = JSONObject()
                    .put("id", memo.memoId.toLongOrNull() ?: memo.memoId)
                    .put("rowStatus", "ARCHIVED")
                    .put("updateTime", archiveTime),
                updateMask = null
            ),
            ArchiveAttempt(
                path = "/api/v1/memos/${memo.memoId}",
                payload = JSONObject()
                    .put("state", "ARCHIVED")
                    .put("updateTime", archiveTime),
                updateMask = null
            ),
            ArchiveAttempt(
                path = "/api/v1/memos/${memo.memoId}",
                payload = JSONObject()
                    .put("name", "memos/${memo.memoId}")
                    .put("state", "ARCHIVED")
                    .put("updateTime", archiveTime),
                updateMask = "state,update_time"
            ),
            ArchiveAttempt(
                path = "/api/v1/memos/${memo.memoId}",
                payload = JSONObject()
                    .put("name", "memos/${memo.memoId}")
                    .put("state", "ARCHIVED")
                    .put("updateTime", archiveTime),
                updateMask = null
            ),
            ArchiveAttempt(
                path = "/api/v1/memo/${memo.memoId}",
                payload = JSONObject()
                    .put("id", memo.memoId.toLongOrNull() ?: memo.memoId)
                    .put("rowStatus", "ARCHIVED"),
                updateMask = null
            ),
            ArchiveAttempt(
                path = "/api/memo/${memo.memoId}",
                payload = JSONObject()
                    .put("id", memo.memoId.toLongOrNull() ?: memo.memoId)
                    .put("rowStatus", "ARCHIVED"),
                updateMask = null
            )
        )

        var lastError: Throwable? = null
        for (attempt in attempts) {
            val result = runCatching {
                patchForJson(
                    serverUrl = settings.serverUrl,
                    path = attempt.path,
                    accessToken = settings.accessToken,
                    payload = attempt.payload,
                    updateMask = attempt.updateMask
                )
            }
            if (result.isSuccess) return
            lastError = result.exceptionOrNull()
        }

        throw lastError ?: IllegalStateException("归档 memo 失败")
    }

    fun setMemoPinned(settings: MemosSettings, memo: TodoItem, pinned: Boolean) {
        val updateTime = Instant.now().toString()
        val attempts = listOf(
            PinAttempt(
                method = HttpMethod.Patch,
                path = "/api/v1/memos/${memo.memoId}",
                payload = JSONObject()
                    .put("pinned", pinned)
                    .put("updateTime", updateTime),
                updateMask = null
            ),
            PinAttempt(
                method = HttpMethod.Patch,
                path = "/api/v1/memos/${memo.memoId}",
                payload = JSONObject()
                    .put("name", "memos/${memo.memoId}")
                    .put("pinned", pinned)
                    .put("updateTime", updateTime),
                updateMask = "pinned,update_time"
            ),
            PinAttempt(
                method = HttpMethod.Post,
                path = "/api/v1/memo/${memo.memoId}/organizer",
                payload = JSONObject().put("pinned", pinned),
                updateMask = null
            ),
            PinAttempt(
                method = HttpMethod.Post,
                path = "/api/memo/${memo.memoId}/organizer",
                payload = JSONObject().put("pinned", pinned),
                updateMask = null
            )
        )

        var lastError: Throwable? = null
        for (attempt in attempts) {
            val result = runCatching {
                when (attempt.method) {
                    HttpMethod.Patch -> patchForJson(
                        serverUrl = settings.serverUrl,
                        path = attempt.path,
                        accessToken = settings.accessToken,
                        payload = attempt.payload,
                        updateMask = attempt.updateMask
                    )
                    HttpMethod.Post -> postForJson(
                        serverUrl = settings.serverUrl,
                        path = attempt.path,
                        accessToken = settings.accessToken,
                        payload = attempt.payload
                    )
                }
            }
            if (result.isSuccess) return
            lastError = result.exceptionOrNull()
        }

        throw lastError ?: IllegalStateException("置顶 memo 失败")
    }

    fun deleteMemo(settings: MemosSettings, memo: TodoItem) {
        val attempts = listOf(
            "/api/v1/memos/${memo.memoId}",
            "/api/v1/memo/${memo.memoId}",
            "/api/memo/${memo.memoId}"
        )

        var lastError: Throwable? = null
        for (path in attempts) {
            val result = runCatching {
                deleteForJson(
                    serverUrl = settings.serverUrl,
                    path = path,
                    accessToken = settings.accessToken
                )
            }
            if (result.isSuccess) return
            lastError = result.exceptionOrNull()
        }

        throw lastError ?: IllegalStateException("删除 memo 失败")
    }

    fun updateMemoContent(
        settings: MemosSettings,
        memoId: String,
        content: String,
        state: String,
        visibility: String
    ) {
        val attempts = listOf(
            UpdateAttempt("/api/v1/memos/$memoId", false, false),
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
        limitValue: String,
        archiveFilter: MemosJsonParser.ArchiveFilter,
        queryParameters: Map<String, String> = emptyMap()
    ): List<TodoItem> {
        val baseUrl = settings.serverUrl.toHttpUrl()
        val urlBuilder = baseUrl.newBuilder()
            .addPathSegments(path.removePrefix("/"))
            .addQueryParameter(limitKey, limitValue)
        queryParameters.forEach { (key, value) ->
            urlBuilder.addQueryParameter(key, value)
        }
        val url = urlBuilder.build()

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
            return MemosJsonParser.parseMemos(body, settings.serverUrl, archiveFilter)
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
            val responseBody = response.body?.string().orEmpty()
            logDebug("PATCH $path updateMask=$updateMask -> HTTP ${response.code}, bodyPrefix=${responseBody.take(80)}")
            check(response.isSuccessful) {
                "Memos API 更新失败：HTTP ${response.code}"
            }
            checkLooksLikeJson(responseBody, path)
        }
    }

    private fun patchForJson(
        serverUrl: String,
        path: String,
        accessToken: String,
        payload: JSONObject,
        updateMask: String?
    ): JSONObject {
        val baseUrl = serverUrl.toHttpUrl()
        val urlBuilder = baseUrl.newBuilder()
            .addPathSegments(path.removePrefix("/"))
        if (!updateMask.isNullOrBlank()) {
            urlBuilder.addQueryParameter("updateMask", updateMask)
        }
        val body = payload
            .toString()
            .toRequestBody(JSON_MEDIA_TYPE)
        val request = Request.Builder()
            .url(urlBuilder.build())
            .header("Authorization", "Bearer $accessToken")
            .header("Accept", "application/json")
            .header("Content-Type", "application/json")
            .patch(body)
            .build()

        client.newCall(request).execute().use { response ->
            val responseBody = response.body?.string().orEmpty()
            logDebug("PATCH $path updateMask=$updateMask -> HTTP ${response.code}, bodyPrefix=${responseBody.take(80)}")
            check(response.isSuccessful) {
                responseBody.ifBlank { "Memos API 请求失败：HTTP ${response.code}" }
            }
            checkLooksLikeJson(responseBody, path)
            return JSONObject(responseBody.ifBlank { "{}" })
        }
    }

    private fun postForJson(
        serverUrl: String,
        path: String,
        accessToken: String,
        payload: JSONObject
    ): JSONObject {
        val baseUrl = serverUrl.toHttpUrl()
        val url = baseUrl.newBuilder()
            .addPathSegments(path.removePrefix("/"))
            .build()
        val body = payload
            .toString()
            .toRequestBody(JSON_MEDIA_TYPE)
        val requestBuilder = Request.Builder()
            .url(url)
            .header("Accept", "application/json")
            .header("Content-Type", "application/json")
            .post(body)
        if (accessToken.isNotBlank()) {
            requestBuilder.header("Authorization", "Bearer $accessToken")
        }

        client.newCall(requestBuilder.build()).execute().use { response ->
            val responseBody = response.body?.string().orEmpty()
            logDebug("POST $path -> HTTP ${response.code}, bodyPrefix=${responseBody.take(80)}")
            check(response.isSuccessful) {
                responseBody.ifBlank { "Memos API 请求失败：HTTP ${response.code}" }
            }
            checkLooksLikeJson(responseBody, path)
            return JSONObject(responseBody.ifBlank { "{}" })
        }
    }

    private fun deleteForJson(
        serverUrl: String,
        path: String,
        accessToken: String
    ): JSONObject {
        val baseUrl = serverUrl.toHttpUrl()
        val url = baseUrl.newBuilder()
            .addPathSegments(path.removePrefix("/"))
            .build()
        val request = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $accessToken")
            .header("Accept", "application/json")
            .delete()
            .build()

        client.newCall(request).execute().use { response ->
            val responseBody = response.body?.string().orEmpty()
            logDebug("DELETE $path -> HTTP ${response.code}, bodyPrefix=${responseBody.take(80)}")
            check(response.isSuccessful) {
                responseBody.ifBlank { "Memos API 请求失败：HTTP ${response.code}" }
            }
            checkLooksLikeJson(responseBody, path)
            return JSONObject(responseBody.ifBlank { "{}" })
        }
    }

    private fun checkLooksLikeJson(responseBody: String, path: String) {
        val trimmed = responseBody.trimStart()
        check(trimmed.isBlank() || trimmed.startsWith("{") || trimmed.startsWith("[")) {
            "接口 $path 返回了网页内容，请检查 API 路径或当前登录状态"
        }
    }

    private fun logDebug(message: String) {
        runCatching { Log.d(TAG, message) }
    }

    private fun logWarning(message: String, throwable: Throwable?) {
        runCatching { Log.w(TAG, message, throwable) }
    }

    private companion object {
        const val TAG = "MemosApiClient"
        val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }

    private data class UpdateAttempt(
        val path: String,
        val updateMask: Boolean,
        val includeResourceName: Boolean
    )

    private data class CreateAttempt(
        val path: String,
        val payload: JSONObject
    )

    private data class ArchiveAttempt(
        val path: String,
        val payload: JSONObject,
        val updateMask: String?
    )

    private data class PinAttempt(
        val method: HttpMethod,
        val path: String,
        val payload: JSONObject,
        val updateMask: String?
    )

    private enum class HttpMethod {
        Patch,
        Post
    }
}
