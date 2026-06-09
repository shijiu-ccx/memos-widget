package com.example.memostodowidget.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import com.example.memostodowidget.R
import com.example.memostodowidget.domain.TodoItem
import com.example.memostodowidget.ui.MainActivity

object TodoWidgetRenderer {
    fun loading(context: Context, widgetId: Int): RemoteViews =
        base(context, widgetId).apply {
            setViewVisibility(R.id.message, View.VISIBLE)
            setViewVisibility(R.id.memo_card, View.GONE)
            setTextViewText(R.id.message, context.getString(R.string.loading))
        }

    fun error(context: Context, widgetId: Int, message: String): RemoteViews =
        base(context, widgetId).apply {
            setViewVisibility(R.id.message, View.VISIBLE)
            setViewVisibility(R.id.memo_card, View.GONE)
            setTextViewText(
                R.id.message,
                message.ifBlank { context.getString(R.string.load_failed) }
            )
            setOnClickPendingIntent(R.id.message, openAppIntent(context))
        }

    fun content(
        context: Context,
        widgetId: Int,
        memo: TodoItem?,
        mode: WidgetMemoMode
    ): RemoteViews =
        base(context, widgetId).apply {
            setTextViewText(R.id.widget_title, context.getString(titleFor(mode)))

            if (memo == null) {
                setViewVisibility(R.id.message, View.VISIBLE)
                setViewVisibility(R.id.memo_card, View.GONE)
                setTextViewText(R.id.message, context.getString(emptyMessageFor(mode)))
                setOnClickPendingIntent(R.id.message, openAppIntent(context))
                return@apply
            }

            setViewVisibility(R.id.message, View.GONE)
            setViewVisibility(R.id.memo_card, View.VISIBLE)
            setTextViewText(R.id.memo_content, renderMemoPreview(memo))
            setOnClickPendingIntent(R.id.memo_card, openMemoIntent(context, memo))
            setOnClickPendingIntent(R.id.memo_content, openMemoIntent(context, memo))
        }

    fun cachedContent(
        context: Context,
        widgetId: Int,
        snapshot: WidgetMemoSnapshot
    ): RemoteViews =
        base(context, widgetId).apply {
            setTextViewText(R.id.widget_title, context.getString(titleFor(snapshot.mode)))
            setViewVisibility(R.id.message, View.GONE)
            setViewVisibility(R.id.memo_card, View.VISIBLE)
            setTextViewText(R.id.memo_content, renderRawMemoPreview(snapshot.rawContent))
            setOnClickPendingIntent(
                R.id.memo_card,
                openMemoIntent(context, snapshot.memoId, snapshot.memoId.hashCode())
            )
            setOnClickPendingIntent(
                R.id.memo_content,
                openMemoIntent(context, snapshot.memoId, snapshot.memoId.hashCode() + 1)
            )
        }

    private fun base(context: Context, widgetId: Int): RemoteViews =
        RemoteViews(context.packageName, R.layout.widget_todo).apply {
            setOnClickPendingIntent(R.id.refresh_button, refreshIntent(context, widgetId))
            setOnClickPendingIntent(R.id.add_button, addMemoIntent(context))
            setOnClickPendingIntent(R.id.widget_title, openAppIntent(context))
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

    private fun addMemoIntent(context: Context): PendingIntent =
        PendingIntent.getActivity(
            context,
            1,
            Intent(context, MainActivity::class.java).apply {
                action = MainActivity.ACTION_NEW_MEMO
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

    private fun openMemoIntent(context: Context, memo: TodoItem): PendingIntent =
        openMemoIntent(context, memo.memoId, memo.memoId.hashCode())

    private fun openMemoIntent(context: Context, memoId: String, requestCode: Int): PendingIntent =
        PendingIntent.getActivity(
            context,
            requestCode,
            Intent(context, MainActivity::class.java).apply {
                action = MainActivity.ACTION_OPEN_MEMO
                putExtra(MainActivity.EXTRA_MEMO_ID, memoId)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

    private fun openAppIntent(context: Context): PendingIntent =
        PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

    private fun titleFor(mode: WidgetMemoMode): Int =
        when (mode) {
            WidgetMemoMode.Latest -> R.string.widget_title_latest
            WidgetMemoMode.Pinned -> R.string.widget_title_pinned
        }

    private fun emptyMessageFor(mode: WidgetMemoMode): Int =
        when (mode) {
            WidgetMemoMode.Latest -> R.string.no_todos
            WidgetMemoMode.Pinned -> R.string.no_pinned_memo
        }

    private fun renderMemoPreview(memo: TodoItem): String {
        return renderRawMemoPreview(memo.rawMemoContent)
            .ifBlank { memo.content.ifBlank { "Memo" } }
    }

    private fun renderRawMemoPreview(rawContent: String): String {
        val lines = MarkdownParser.parse(rawContent)
            .filter { it.text.isNotBlank() }
            .map { line ->
                when {
                    line.kind == MarkdownLine.Kind.Task && line.isChecked -> "☑ ${line.text}"
                    line.kind == MarkdownLine.Kind.Task -> "☐ ${line.text}"
                    else -> line.text
                }
            }
        return lines.joinToString("\n").ifBlank { "Memo" }
    }
}
