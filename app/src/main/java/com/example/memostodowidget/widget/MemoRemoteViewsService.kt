package com.example.memostodowidget.widget

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.TypedValue
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.example.memostodowidget.AppContainer
import com.example.memostodowidget.R
import com.example.memostodowidget.domain.TodoItem
import kotlinx.coroutines.runBlocking

class MemoRemoteViewsService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory =
        MemoRemoteViewsFactory(applicationContext)

    companion object {
        fun uriFor(widgetId: Int): Uri =
            Uri.parse("memos-widget://memo-lines/$widgetId/${System.currentTimeMillis()}")
    }
}

private class MemoRemoteViewsFactory(
    private val context: Context
) : RemoteViewsService.RemoteViewsFactory {
    private var memo: TodoItem? = null
    private var lines: List<MarkdownLine> = emptyList()

    override fun onCreate() = Unit

    override fun onDataSetChanged() {
        memo = runBlocking {
            AppContainer.todoRepository(context).getRecentTodos(1).getOrNull()?.firstOrNull()
        }
        lines = memo?.rawMemoContent?.let(MarkdownParser::parse).orEmpty()
    }

    override fun onDestroy() {
        memo = null
        lines = emptyList()
    }

    override fun getCount(): Int = lines.size

    override fun getViewAt(position: Int): RemoteViews {
        val line = lines.getOrNull(position) ?: MarkdownLine("", MarkdownLine.Kind.Spacer)
        val views = RemoteViews(context.packageName, R.layout.widget_markdown_line)

        views.setTextViewText(R.id.markdown_check, if (line.isChecked) "\u2611" else "\u2610")
        views.setTextViewText(R.id.markdown_text, line.text)
        views.setViewVisibility(R.id.markdown_check, if (line.kind == MarkdownLine.Kind.Task) 0 else 4)
        views.setTextViewTextSize(R.id.markdown_text, TypedValue.COMPLEX_UNIT_SP, textSize(line))

        memo?.let { currentMemo ->
            val fillInIntent = Intent().apply {
                action = TodoWidgetProvider.ACTION_OPEN_MEMO
                putExtra(TodoWidgetProvider.EXTRA_MEMO_URL, currentMemo.webUrl)
            }

            views.setOnClickFillInIntent(R.id.markdown_row, fillInIntent)
            views.setOnClickFillInIntent(R.id.markdown_check, fillInIntent)
            views.setOnClickFillInIntent(R.id.markdown_text, fillInIntent)
        }

        return views
    }

    override fun getLoadingView(): RemoteViews? = null

    override fun getViewTypeCount(): Int = 1

    override fun getItemId(position: Int): Long = position.toLong()

    override fun hasStableIds(): Boolean = true

    private fun textSize(line: MarkdownLine): Float =
        when (line.kind) {
            MarkdownLine.Kind.Heading -> 17f
            MarkdownLine.Kind.Code -> 13f
            else -> 14f
        }
}
