package com.example.memostodowidget.data

import com.example.memostodowidget.domain.TodoItem
import com.example.memostodowidget.domain.TodoTaskLine
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant

object MemosJsonParser {
    fun parseMemos(
        json: String,
        serverUrl: String,
        archiveFilter: ArchiveFilter = ArchiveFilter.ActiveOnly
    ): List<TodoItem> {
        val trimmed = json.trim()
        val memos = when {
            trimmed.startsWith("[") -> JSONArray(trimmed)
            trimmed.startsWith("{") -> JSONObject(trimmed).optJSONArray("memos") ?: JSONArray()
            else -> JSONArray()
        }

        return buildList {
            for (index in 0 until memos.length()) {
                val memo = memos.optJSONObject(index) ?: continue
                val archived = memo.isArchived()
                when (archiveFilter) {
                    ArchiveFilter.ActiveOnly -> if (archived) continue
                    ArchiveFilter.ArchivedOnly -> if (!archived) continue
                    ArchiveFilter.All -> Unit
                }

                parseMemo(memo, serverUrl)?.let(::add)
            }
        }
    }

    fun parseMemo(memo: JSONObject, serverUrl: String): TodoItem? {
        val content = memo.optString("content")
        val name = memo.optString("name")
        val uid = memo.optString("uid")
        val apiId = when {
            name.isNotBlank() -> name.substringAfterLast('/')
            memo.has("id") -> memo.opt("id")?.toString().orEmpty()
            uid.isNotBlank() -> uid
            else -> return null
        }
        val webId = uid.ifBlank { apiId }
        return TodoItem(
            id = apiId,
            memoId = apiId,
            content = cleanMarkdownPreview(content),
            rawMemoContent = content,
            taskLines = parseCheckboxItems(content),
            state = memo.optString("state").ifBlank { memo.optString("rowStatus") },
            visibility = memo.optString("visibility"),
            pinned = memo.optBoolean("pinned", false),
            createdAtEpochSeconds = parseCreatedAt(memo),
            webUrl = "$serverUrl/m/$webId"
        )
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

    private val CHECKBOX_LINE = Regex("^\\s*(?:[-*+]\\s+)?\\[([ xX])\\]\\s+")

    enum class ArchiveFilter {
        ActiveOnly,
        ArchivedOnly,
        All
    }
}
