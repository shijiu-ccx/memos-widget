package com.example.memostodowidget.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.example.memostodowidget.AppContainer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class TodoWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        refreshInBroadcastLifetime {
            TodoWidgetUpdater.refresh(context, appWidgetIds)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        when (intent.action) {
            ACTION_REFRESH -> {
                val widgetId = intent.getIntExtra(
                    AppWidgetManager.EXTRA_APPWIDGET_ID,
                    AppWidgetManager.INVALID_APPWIDGET_ID
                )
                refreshInBroadcastLifetime {
                    if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
                        TodoWidgetUpdater.refreshAll(context)
                    } else {
                        TodoWidgetUpdater.refresh(context, intArrayOf(widgetId))
                    }
                }
            }
            ACTION_OPEN_MEMO -> {
                val url = intent.getStringExtra(EXTRA_MEMO_URL).orEmpty()
                if (url.isNotBlank()) {
                    context.startActivity(
                        Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    )
                }
            }
            ACTION_TOGGLE_TASK -> {
                val widgetId = intent.getIntExtra(
                    AppWidgetManager.EXTRA_APPWIDGET_ID,
                    AppWidgetManager.INVALID_APPWIDGET_ID
                )
                val lineIndex = intent.getIntExtra(EXTRA_LINE_INDEX, -1)
                val checked = intent.getBooleanExtra(EXTRA_CHECKED, false)
                if (lineIndex >= 0) {
                    refreshInBroadcastLifetime {
                        val repository = AppContainer.todoRepository(context)
                        val memo = repository.getRecentTodos(1).getOrNull()?.firstOrNull()
                        if (memo != null) {
                            repository.setTodoChecked(memo, lineIndex, checked)
                        }
                        if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
                            TodoWidgetUpdater.refreshAll(context)
                        } else {
                            TodoWidgetUpdater.refresh(context, intArrayOf(widgetId))
                        }
                    }
                }
            }
        }
    }

    private fun refreshInBroadcastLifetime(block: suspend () -> Unit) {
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                block()
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val ACTION_REFRESH = "com.example.memostodowidget.action.REFRESH_WIDGET"
        const val ACTION_OPEN_MEMO = "com.example.memostodowidget.action.OPEN_MEMO"
        const val ACTION_TOGGLE_TASK = "com.example.memostodowidget.action.TOGGLE_TASK"
        const val EXTRA_MEMO_URL = "com.example.memostodowidget.extra.MEMO_URL"
        const val EXTRA_LINE_INDEX = "com.example.memostodowidget.extra.LINE_INDEX"
        const val EXTRA_CHECKED = "com.example.memostodowidget.extra.CHECKED"
    }
}
