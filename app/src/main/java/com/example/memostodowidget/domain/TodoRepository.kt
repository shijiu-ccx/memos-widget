package com.example.memostodowidget.domain

interface TodoRepository {
    suspend fun getRecentTodos(limit: Int = 5): Result<List<TodoItem>>
}

