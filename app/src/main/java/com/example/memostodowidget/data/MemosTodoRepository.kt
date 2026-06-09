package com.example.memostodowidget.data

import com.example.memostodowidget.domain.TodoItem
import com.example.memostodowidget.domain.TodoRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MemosTodoRepository(
    private val settingsRepository: SettingsRepository,
    private val apiClient: MemosApiClient
) : TodoRepository {
    override suspend fun getRecentTodos(limit: Int): Result<List<TodoItem>> = withContext(Dispatchers.IO) {
        runCatching {
            val settings = settingsRepository.getSettings()
            check(settings.isConfigured) { "请先在 App 内配置 Memos 地址和 Access Token" }

            apiClient.fetchMemos(settings)
                .sortedWith(compareByDescending<TodoItem> { it.pinned }.thenByDescending { it.createdAtEpochSeconds })
                .take(limit)
        }
    }

    override suspend fun setTodoChecked(todo: TodoItem, lineIndex: Int, checked: Boolean): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val settings = settingsRepository.getSettings()
            check(settings.isConfigured) { "请先在 App 内配置 Memos 地址和 Access Token" }

            val updatedContent = replaceCheckboxState(todo.rawMemoContent, lineIndex, checked)
            apiClient.updateMemoContent(
                settings = settings,
                memoId = todo.memoId,
                content = updatedContent,
                state = todo.state,
                visibility = todo.visibility
            )
        }
    }

    private fun replaceCheckboxState(content: String, lineIndex: Int, checked: Boolean): String {
        val lines = content.split('\n').toMutableList()
        check(lineIndex in lines.indices) { "待办位置已变化，请刷新后重试" }

        val line = lines[lineIndex]
        check(CHECKBOX_LINE.containsMatchIn(line)) { "这条内容不是 Markdown 勾选项" }

        val match = CHECKBOX_LINE.find(line) ?: error("这条内容不是 Markdown 勾选项")
        lines[lineIndex] = line.replaceRange(
            match.range,
            "${match.groupValues[1]}[${if (checked) "x" else " "}]"
        )
        return lines.joinToString("\n")
    }

    private companion object {
        val CHECKBOX_LINE = Regex("^(\\s*(?:[-*+]\\s+)?)(?:\\[[ xX]\\])")
    }
}
