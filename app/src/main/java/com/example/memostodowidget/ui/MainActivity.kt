package com.example.memostodowidget.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.memostodowidget.AppContainer
import com.example.memostodowidget.databinding.ActivityMainBinding
import com.example.memostodowidget.widget.TodoWidgetUpdater
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val settingsRepository = AppContainer.settingsRepository(this)
        lifecycleScope.launch {
            val settings = settingsRepository.settings.first()
            binding.serverUrlInput.setText(settings.serverUrl)
            binding.accessTokenInput.setText(settings.accessToken)
        }

        binding.saveButton.setOnClickListener {
            val serverUrl = binding.serverUrlInput.text?.toString().orEmpty()
            val accessToken = binding.accessTokenInput.text?.toString().orEmpty()
            if (!isValidServerUrl(serverUrl) || accessToken.isBlank()) {
                Snackbar.make(binding.root, "请输入完整的 http(s) 地址和 Access Token", Snackbar.LENGTH_LONG).show()
                return@setOnClickListener
            }

            lifecycleScope.launch {
                settingsRepository.save(serverUrl, accessToken)
                TodoWidgetUpdater.refreshAll(this@MainActivity)
                Snackbar.make(binding.root, "配置已保存，正在刷新小组件", Snackbar.LENGTH_LONG).show()
            }
        }
    }

    private fun isValidServerUrl(value: String): Boolean {
        val normalized = value.trim()
        return normalized.startsWith("https://") || normalized.startsWith("http://")
    }
}
