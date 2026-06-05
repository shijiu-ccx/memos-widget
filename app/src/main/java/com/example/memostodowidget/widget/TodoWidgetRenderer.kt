package com.example.memostodowidget.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import com.example.memostodowidget.R
import com.example.memostodowidget.ui.MainActivity

object TodoWidgetRenderer {
    fun loading(context: Context, widgetId: Int): RemoteViews =
        base(context, widgetId).apply {
            setViewVisibility(R.id.message, View.VISIBLE)
            setViewVisibility(R.id.memo_list, View.GONE)
            setTextViewText(R.id.message, context.getString(R.string.loading))
        }

    fun error(context: Context, widgetId: Int, message: String): RemoteViews =
        base(context, widgetId).apply {
            setViewVisibility(R.id.message, View.VISIBLE)
            setViewVisibility(R.id.memo_list, View.GONE)
            setTextViewText(
                R.id.message,
                message.ifBlank { context.getString(R.string.load_failed) }
            )
            setOnClickPendingIntent(R.id.message, openSettingsIntent(context))
        }

    fun content(context: Context, widgetId: Int): RemoteViews =
        base(context, widgetId).apply {
            setViewVisibility(R.id.message, View.GONE)
            setViewVisibility(R.id.memo_list, View.VISIBLE)
            setTextViewText(R.id.message, context.getString(R.string.no_todos))

            val serviceIntent = Intent(context, MemoRemoteViewsService::class.java).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
                data = MemoRemoteViewsService.uriFor(widgetId)
            }
            setRemoteAdapter(R.id.memo_list, serviceIntent)
            setEmptyView(R.id.memo_list, R.id.message)
            setPendingIntentTemplate(R.id.memo_list, widgetActionTemplate(context, widgetId))
        }

    private fun base(context: Context, widgetId: Int): RemoteViews =
        RemoteViews(context.packageName, R.layout.widget_todo).apply {
            setOnClickPendingIntent(R.id.refresh_button, refreshIntent(context, widgetId))
            setOnClickPendingIntent(R.id.widget_title, openSettingsIntent(context))
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

    private fun widgetActionTemplate(context: Context, widgetId: Int): PendingIntent {
        val intent = Intent(context, TodoWidgetProvider::class.java).apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
        }
        return PendingIntent.getBroadcast(
            context,
            widgetId * 100,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
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
