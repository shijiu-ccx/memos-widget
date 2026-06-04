package com.example.memostodowidget

import android.content.Context
import com.example.memostodowidget.data.MemosApiClient
import com.example.memostodowidget.data.MemosTodoRepository
import com.example.memostodowidget.data.SettingsRepository
import com.example.memostodowidget.domain.TodoRepository
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

object AppContainer {
    @Volatile
    private var settingsRepository: SettingsRepository? = null

    @Volatile
    private var todoRepository: TodoRepository? = null

    fun settingsRepository(context: Context): SettingsRepository =
        settingsRepository ?: synchronized(this) {
            settingsRepository ?: SettingsRepository(context.applicationContext).also {
                settingsRepository = it
            }
        }

    fun todoRepository(context: Context): TodoRepository =
        todoRepository ?: synchronized(this) {
            todoRepository ?: MemosTodoRepository(
                settingsRepository = settingsRepository(context),
                apiClient = MemosApiClient(
                    OkHttpClient.Builder()
                        .connectTimeout(15, TimeUnit.SECONDS)
                        .readTimeout(20, TimeUnit.SECONDS)
                        .build()
                )
            ).also {
                todoRepository = it
            }
        }
}

