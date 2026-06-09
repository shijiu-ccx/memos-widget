package com.example.memostodowidget.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.util.Log
import com.example.memostodowidget.AppContainer
import com.example.memostodowidget.domain.TodoItem

object TodoWidgetUpdater {
    suspend fun refreshAll(context: Context) {
        val manager = AppWidgetManager.getInstance(context)
        val ids = manager.getAppWidgetIds(ComponentName(context, TodoWidgetProvider::class.java))
        refresh(context, ids)
    }

    suspend fun refresh(context: Context, widgetIds: IntArray) {
        if (widgetIds.isEmpty()) return

        val appContext = context.applicationContext
        val manager = AppWidgetManager.getInstance(appContext)
        val preferences = WidgetPreferences(appContext)
        widgetIds.forEach { widgetId ->
            manager.updateAppWidget(widgetId, TodoWidgetRenderer.loading(appContext, widgetId))
        }

        val result = runCatching {
            val settings = AppContainer.settingsRepository(appContext).getSettings()
            check(settings.isConfigured) { "请先在 App 内配置 Memos 地址和 Access Token" }

            val memos = AppContainer.memosApiClient().fetchMemos(settings)
            val mode = preferences.memoMode
            val memo = selectMemo(memos, mode)
            memo?.let {
                preferences.saveSnapshot(
                    WidgetMemoSnapshot(
                        mode = mode,
                        memoId = it.memoId,
                        rawContent = it.rawMemoContent
                    )
                )
            }
            mode to memo
        }
        widgetIds.forEach { widgetId ->
            val views = result.fold(
                onSuccess = { (mode, memo) -> TodoWidgetRenderer.content(appContext, widgetId, memo, mode) },
                onFailure = {
                    Log.w(TAG, "Widget refresh failed", it)
                    val cached = preferences.cachedSnapshot()
                    if (cached != null) {
                        TodoWidgetRenderer.cachedContent(appContext, widgetId, cached)
                    } else {
                        TodoWidgetRenderer.error(appContext, widgetId, it.message.orEmpty())
                    }
                }
            )
            manager.updateAppWidget(widgetId, views)
        }
    }

    suspend fun updateCachedMemoIfVisible(context: Context, memo: TodoItem, rawContent: String) {
        val appContext = context.applicationContext
        val preferences = WidgetPreferences(appContext)
        val cached = preferences.cachedSnapshot() ?: return
        if (cached.memoId != memo.memoId) return

        val snapshot = cached.copy(rawContent = rawContent)
        preferences.saveSnapshot(snapshot)

        val manager = AppWidgetManager.getInstance(appContext)
        val ids = manager.getAppWidgetIds(ComponentName(appContext, TodoWidgetProvider::class.java))
        ids.forEach { widgetId ->
            manager.updateAppWidget(
                widgetId,
                TodoWidgetRenderer.cachedContent(appContext, widgetId, snapshot)
            )
        }
    }

    private fun selectMemo(memos: List<TodoItem>, mode: WidgetMemoMode): TodoItem? =
        when (mode) {
            WidgetMemoMode.Latest -> memos.maxByOrNull { it.createdAtEpochSeconds }
            WidgetMemoMode.Pinned -> memos
                .filter { it.pinned }
                .maxByOrNull { it.createdAtEpochSeconds }
        }

    private const val TAG = "TodoWidgetUpdater"
}
