package com.example.memostodowidget.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.View
import android.widget.RemoteViews
import com.example.memostodowidget.R
import com.example.memostodowidget.domain.TodoItem
import com.example.memostodowidget.ui.MainActivity

object TodoWidgetRenderer {
    private val rowIds = intArrayOf(
        R.id.todo_row_1,
        R.id.todo_row_2,
        R.id.todo_row_3,
        R.id.todo_row_4,
        R.id.todo_row_5
    )
    private val textIds = intArrayOf(
        R.id.todo_text_1,
        R.id.todo_text_2,
        R.id.todo_text_3,
        R.id.todo_text_4,
        R.id.todo_text_5
    )

    fun loading(context: Context, widgetId: Int): RemoteViews =
        base(context, widgetId).apply {
            setViewVisibility(R.id.message, View.VISIBLE)
            setTextViewText(R.id.message, context.getString(R.string.loading))
            hideRows(this)
        }

    fun error(context: Context, widgetId: Int, message: String): RemoteViews =
        base(context, widgetId).apply {
            setViewVisibility(R.id.message, View.VISIBLE)
            setTextViewText(
                R.id.message,
                message.ifBlank { context.getString(R.string.load_failed) }
            )
            setOnClickPendingIntent(R.id.message, openSettingsIntent(context))
            hideRows(this)
        }

    fun content(context: Context, widgetId: Int, todos: List<TodoItem>): RemoteViews =
        base(context, widgetId).apply {
            setViewVisibility(R.id.message, if (todos.isEmpty()) View.VISIBLE else View.GONE)
            setTextViewText(R.id.message, context.getString(R.string.no_todos))
            rowIds.forEachIndexed { index, rowId ->
                val todo = todos.getOrNull(index)
                setViewVisibility(rowId, if (todo == null) View.GONE else View.VISIBLE)
                if (todo != null) {
                    setTextViewText(textIds[index], todo.content)
                    setOnClickPendingIntent(rowId, openMemoIntent(context, widgetId, index, todo.webUrl))
                }
            }
        }

    private fun base(context: Context, widgetId: Int): RemoteViews =
        RemoteViews(context.packageName, R.layout.widget_todo).apply {
            setOnClickPendingIntent(R.id.refresh_button, refreshIntent(context, widgetId))
            setOnClickPendingIntent(R.id.widget_title, openSettingsIntent(context))
        }

    private fun hideRows(views: RemoteViews) {
        rowIds.forEach { views.setViewVisibility(it, View.GONE) }
    }

    private fun refreshIntent(context: Context, widgetId: Int): PendingIntent {
        val intent = Intent(context, TodoWidgetProvider::class.java).apply {
            action = TodoWidgetProvider.ACTION_REFRESH
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
        }
        return PendingIntent.getBroadcast(
            context,
            widgetId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun openMemoIntent(
        context: Context,
        widgetId: Int,
        rowIndex: Int,
        url: String
    ): PendingIntent {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        return PendingIntent.getActivity(
            context,
            widgetId * 10 + rowIndex + 1,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun openSettingsIntent(context: Context): PendingIntent =
        PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
}

