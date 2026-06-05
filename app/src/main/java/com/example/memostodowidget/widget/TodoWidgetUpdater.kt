package com.example.memostodowidget.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import com.example.memostodowidget.AppContainer

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
        widgetIds.forEach { widgetId ->
            manager.updateAppWidget(widgetId, TodoWidgetRenderer.loading(appContext, widgetId))
        }

        val result = AppContainer.todoRepository(appContext).getRecentTodos(5)
        widgetIds.forEach { widgetId ->
            val views = result.fold(
                onSuccess = { TodoWidgetRenderer.content(appContext, widgetId) },
                onFailure = { TodoWidgetRenderer.error(appContext, widgetId, it.message.orEmpty()) }
            )
            manager.updateAppWidget(widgetId, views)
            manager.notifyAppWidgetViewDataChanged(widgetId, com.example.memostodowidget.R.id.memo_list)
        }
    }

}
