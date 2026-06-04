package com.example.memostodowidget.domain

data class TodoItem(
    val id: String,
    val content: String,
    val createdAtEpochSeconds: Long,
    val webUrl: String
)

