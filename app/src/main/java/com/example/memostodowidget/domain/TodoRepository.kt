package com.example.memostodowidget.domain

interface TodoRepository {
    suspend fun getRecentTodos(limit: Int = 5): Result<List<TodoItem>>
    suspend fun setTodoChecked(todo: TodoItem, lineIndex: Int, checked: Boolean): Result<Unit>
}
