package com.example.memostodowidget.widget

import android.content.Context

enum class WidgetMemoMode {
    Latest,
    Pinned;

    companion object {
        fun fromValue(value: String?): WidgetMemoMode =
            values().firstOrNull { it.name == value } ?: Latest
    }
}

data class WidgetMemoSnapshot(
    val mode: WidgetMemoMode,
    val memoId: String,
    val rawContent: String
)

class WidgetPreferences(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        "widget_preferences",
        Context.MODE_PRIVATE
    )

    var memoMode: WidgetMemoMode
        get() = WidgetMemoMode.fromValue(preferences.getString(KEY_MEMO_MODE, null))
        set(value) {
            preferences.edit().putString(KEY_MEMO_MODE, value.name).apply()
        }

    fun cachedSnapshot(): WidgetMemoSnapshot? {
        val memoId = preferences.getString(KEY_CACHE_MEMO_ID, null)?.takeIf { it.isNotBlank() }
            ?: return null
        val rawContent = preferences.getString(KEY_CACHE_RAW_CONTENT, null).orEmpty()
        val mode = WidgetMemoMode.fromValue(preferences.getString(KEY_CACHE_MODE, null))
        return WidgetMemoSnapshot(mode = mode, memoId = memoId, rawContent = rawContent)
    }

    fun saveSnapshot(snapshot: WidgetMemoSnapshot) {
        preferences.edit()
            .putString(KEY_CACHE_MODE, snapshot.mode.name)
            .putString(KEY_CACHE_MEMO_ID, snapshot.memoId)
            .putString(KEY_CACHE_RAW_CONTENT, snapshot.rawContent)
            .apply()
    }

    private companion object {
        const val KEY_MEMO_MODE = "memo_mode"
        const val KEY_CACHE_MODE = "cache_mode"
        const val KEY_CACHE_MEMO_ID = "cache_memo_id"
        const val KEY_CACHE_RAW_CONTENT = "cache_raw_content"
    }
}
