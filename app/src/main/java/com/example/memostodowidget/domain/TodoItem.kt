package com.example.memostodowidget.domain

data class TodoItem(
    val id: String,
    val memoId: String,
    val content: String,
    val rawMemoContent: String,
    val taskLines: List<TodoTaskLine>,
    val state: String,
    val visibility: String,
    val pinned: Boolean,
    val createdAtEpochSeconds: Long,
    val webUrl: String
)

data class TodoTaskLine(
    val lineIndex: Int,
    val content: String,
    val isChecked: Boolean
)
