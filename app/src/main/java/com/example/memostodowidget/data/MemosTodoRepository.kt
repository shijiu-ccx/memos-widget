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
                .sortedByDescending(TodoItem::createdAtEpochSeconds)
                .take(limit)
        }
    }
}

