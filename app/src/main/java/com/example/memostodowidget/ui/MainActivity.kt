package com.example.memostodowidget.ui

import android.graphics.Typeface
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.unit.dp as composeDp
import androidx.core.view.GravityCompat
import androidx.core.view.setPadding
import androidx.lifecycle.lifecycleScope
import com.example.memostodowidget.AppContainer
import com.example.memostodowidget.R
import com.example.memostodowidget.data.MemosSettings
import com.example.memostodowidget.data.SettingsRepository
import com.example.memostodowidget.databinding.ActivityMainBinding
import com.example.memostodowidget.domain.TodoItem
import com.example.memostodowidget.widget.MarkdownLine
import com.example.memostodowidget.widget.MarkdownParser
import com.example.memostodowidget.widget.TodoWidgetUpdater
import com.example.memostodowidget.widget.WidgetMemoMode
import com.example.memostodowidget.widget.WidgetPreferences
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var settingsRepository: SettingsRepository
    private var currentSettings = MemosSettings()
    private var editingMemo: TodoItem? = null
    private var displayedMemos: List<TodoItem> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        settingsRepository = AppContainer.settingsRepository(this)
        configureActions()
        configureEditorInput()
        configureWidgetMode()
        configureBackNavigation()

        lifecycleScope.launch {
            currentSettings = settingsRepository.settings.first()
            binding.serverUrlInput.setText(currentSettings.serverUrl)
            binding.accessTokenInput.setText(currentSettings.accessToken)
            if (currentSettings.isConfigured) {
                showClient()
                if (!handleLaunchIntent(intent)) {
                    refreshMemos()
                }
            } else {
                showSetup()
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (currentSettings.isConfigured) {
            showClient()
            if (!handleLaunchIntent(intent)) {
                refreshMemos()
            }
        }
    }

    private fun configureActions() {
        binding.saveButton.setOnClickListener {
            login()
        }
        binding.reloadButton.setOnClickListener {
            refreshMemos()
        }
        binding.publishButton.setOnClickListener {
            editingMemo?.let { saveMemoEdit(it) } ?: publishMemo()
        }
        binding.settingsButton.setOnClickListener {
            showSetup()
        }
        binding.newMemoButton.setOnClickListener {
            startCreatingMemo()
        }
        binding.editorCancelButton.setOnClickListener {
            closeEditor()
        }
        binding.archiveHistoryButton.setOnClickListener {
            openArchiveHistory()
        }
        binding.markdownTaskButton.setOnClickListener {
            insertMarkdownSnippet("- [ ] ", "")
        }
        binding.markdownIndentButton.setOnClickListener {
            insertMarkdownSnippet("  ", "")
        }
        binding.markdownBoldButton.setOnClickListener {
            insertMarkdownSnippet("**", "**", "加粗文字")
        }
        binding.markdownHeadingButton.setOnClickListener {
            insertMarkdownSnippet("# ", "")
        }
        binding.markdownListButton.setOnClickListener {
            insertMarkdownSnippet("- ", "")
        }
        binding.markdownQuoteButton.setOnClickListener {
            insertMarkdownSnippet("> ", "")
        }
        binding.markdownCodeButton.setOnClickListener {
            insertMarkdownSnippet("`", "`", "代码")
        }
    }

    private fun configureEditorInput() {
        binding.memoContentInput.apply {
            isVerticalScrollBarEnabled = true
            overScrollMode = View.OVER_SCROLL_IF_CONTENT_SCROLLS
            setOnTouchListener { view, event ->
                view.parent.requestDisallowInterceptTouchEvent(true)
                if (event.action == MotionEvent.ACTION_UP || event.action == MotionEvent.ACTION_CANCEL) {
                    view.parent.requestDisallowInterceptTouchEvent(false)
                }
                false
            }
        }
    }

    private fun configureWidgetMode() {
        val preferences = WidgetPreferences(this)
        val checkedId = when (preferences.memoMode) {
            WidgetMemoMode.Latest -> R.id.widget_mode_latest
            WidgetMemoMode.Pinned -> R.id.widget_mode_pinned
        }
        binding.widgetModeGroup.check(checkedId)
        binding.widgetModeGroup.setOnCheckedChangeListener { _, selectedId ->
            preferences.memoMode = when (selectedId) {
                R.id.widget_mode_pinned -> WidgetMemoMode.Pinned
                else -> WidgetMemoMode.Latest
            }
            lifecycleScope.launch {
                TodoWidgetUpdater.refreshAll(this@MainActivity)
            }
        }
    }

    private fun handleLaunchIntent(intent: Intent?): Boolean {
        return when (intent?.action) {
            ACTION_NEW_MEMO -> {
                startCreatingMemo()
                true
            }
            ACTION_OPEN_MEMO -> {
                closeEditor()
                false
            }
            else -> false
        }
    }

    private fun login() {
        val serverUrl = normalizeServerUrl(binding.serverUrlInput.text?.toString().orEmpty())
        val accessToken = binding.accessTokenInput.text?.toString().orEmpty().trim()
        Log.d(TAG, "login clicked serverUrl=$serverUrl tokenBlank=${accessToken.isBlank()}")
        if (!isValidServerUrl(serverUrl)) {
            Snackbar.make(binding.root, getString(R.string.invalid_server_url), Snackbar.LENGTH_LONG).show()
            return
        }
        if (accessToken.isBlank()) {
            Snackbar.make(binding.root, "请输入 Access Token", Snackbar.LENGTH_LONG).show()
            return
        }

        setLoading(true)
        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    val settings = MemosSettings(serverUrl = serverUrl, accessToken = accessToken)
                    val memos = AppContainer.memosApiClient()
                        .fetchMemos(settings)
                        .sortForFeed()
                    settingsRepository.save(serverUrl, accessToken)
                    settings to memos
                }
            }
            setLoading(false)
            result.fold(
                onSuccess = { (settings, memos) ->
                    currentSettings = settings
                    showClient()
                    Snackbar.make(binding.root, getString(R.string.login_success), Snackbar.LENGTH_SHORT).show()
                    renderMemos(memos)
                    TodoWidgetUpdater.refreshAll(this@MainActivity)
                },
                onFailure = { showError(it.message.orEmpty()) }
            )
        }
    }

    private fun refreshMemos() {
        if (!currentSettings.isConfigured) {
            showSetup()
            return
        }

        setLoading(true)
        showFeedMessage(getString(R.string.loading))
        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    AppContainer.memosApiClient()
                        .fetchMemos(currentSettings)
                        .sortForFeed()
                }
            }
            setLoading(false)
            result.fold(
                onSuccess = ::renderMemos,
                onFailure = { showError(it.message.orEmpty()) }
            )
        }
    }

    private fun publishMemo() {
        val content = binding.memoContentInput.text?.toString().orEmpty().trim()
        if (content.isBlank()) {
            Snackbar.make(binding.root, getString(R.string.empty_content), Snackbar.LENGTH_SHORT).show()
            return
        }
        if (!currentSettings.isConfigured) {
            showSetup()
            return
        }

        setLoading(true)
        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    AppContainer.memosApiClient().createMemo(currentSettings, content)
                }
            }
            setLoading(false)
            result.fold(
                onSuccess = {
                    closeEditor()
                    Snackbar.make(binding.root, getString(R.string.publish_success), Snackbar.LENGTH_SHORT).show()
                    refreshMemos()
                    TodoWidgetUpdater.refreshAll(this@MainActivity)
                },
                onFailure = { showError(it.message.orEmpty()) }
            )
        }
    }

    private fun saveMemoEdit(memo: TodoItem) {
        val content = binding.memoContentInput.text?.toString().orEmpty().trim()
        if (content.isBlank()) {
            Snackbar.make(binding.root, getString(R.string.empty_content), Snackbar.LENGTH_SHORT).show()
            return
        }
        if (!currentSettings.isConfigured) {
            showSetup()
            return
        }

        setLoading(true)
        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    AppContainer.memosApiClient().updateMemoContent(
                        settings = currentSettings,
                        memoId = memo.memoId,
                        content = content,
                        state = memo.state,
                        visibility = memo.visibility
                    )
                }
            }
            setLoading(false)
            result.fold(
                onSuccess = {
                    closeEditor()
                    Snackbar.make(binding.root, getString(R.string.edit_success), Snackbar.LENGTH_SHORT).show()
                    TodoWidgetUpdater.updateCachedMemoIfVisible(this@MainActivity, memo, content)
                    refreshMemos()
                    TodoWidgetUpdater.refreshAll(this@MainActivity)
                },
                onFailure = { showError(it.message.orEmpty()) }
            )
        }
    }

    private fun renderMemos(memos: List<TodoItem>) {
        displayedMemos = memos
        binding.memoList.removeAllViews()
        if (memos.isEmpty()) {
            showFeedMessage(getString(R.string.no_todos))
            return
        }

        binding.feedMessage.visibility = View.GONE
        memos.forEach { memo ->
            binding.memoList.addView(createMemoCard(memo))
        }
    }

    private fun createMemoCard(memo: TodoItem): View {
        val card = MaterialCardView(this).apply {
            radius = dp(8).toFloat()
            cardElevation = dp(1).toFloat()
            useCompatPadding = true
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dp(10)
            }
        }

        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14))
        }
        content.addView(createMemoHeader(memo))

        content.addView(createMarkdownView(memo))
        card.addView(content)
        return card
    }

    private fun createMemoHeader(memo: TodoItem): View {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL

            addView(TextView(this@MainActivity).apply {
                text = formatTime(memo.createdAtEpochSeconds)
                textSize = 12f
                alpha = 0.65f
                layoutParams = LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1f
                )
            })

            if (memo.pinned) {
                addView(TextView(this@MainActivity).apply {
                    text = getString(R.string.pin)
                    textSize = 12f
                    alpha = 0.75f
                    typeface = Typeface.DEFAULT_BOLD
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        marginEnd = dp(8)
                    }
                })
            }

            addView(TextView(this@MainActivity).apply {
                contentDescription = getString(R.string.open_settings)
                text = "☰"
                textSize = 20f
                alpha = 0.65f
                gravity = android.view.Gravity.CENTER
                isClickable = true
                isFocusable = true
                setOnClickListener {
                    showMemoOptions(this, memo)
                }
                layoutParams = LinearLayout.LayoutParams(dp(32), dp(32))
            })
        }
    }

    private fun createMarkdownView(memo: TodoItem): View {
        return FrameLayout(this).apply {
            addView(ComposeView(this@MainActivity).apply {
                setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
                setContent {
                    MaterialTheme {
                        MemoMarkdown(
                            content = memo.rawMemoContent,
                            onTextClick = {},
                            onTaskCheckedChange = { lineIndex, checked ->
                                toggleTaskLine(memo, lineIndex, checked)
                            }
                        )
                    }
                }
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT
                )
            })
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dp(8)
            }
        }
    }

    private fun showMemoOptions(anchor: View, memo: TodoItem) {
        PopupMenu(this, anchor).apply {
            menu.add(if (memo.pinned) getString(R.string.unpin) else getString(R.string.pin))
            menu.add(getString(R.string.delete))
            menu.add(getString(R.string.archive))
            menu.add(getString(R.string.edit))
            setOnMenuItemClickListener { item ->
                when (item.title.toString()) {
                    getString(R.string.pin),
                    getString(R.string.unpin) -> setMemoPinned(memo, !memo.pinned)
                    getString(R.string.edit) -> startEditingMemo(memo)
                    getString(R.string.delete) -> deleteMemo(memo)
                    getString(R.string.archive) -> archiveMemo(memo)
                }
                true
            }
            show()
        }
    }

    private fun createMarkdownLine(memo: TodoItem, line: MarkdownLine): TextView {
        return TextView(this).apply {
            text = when (line.kind) {
                MarkdownLine.Kind.Task -> "${if (line.isChecked) "☑" else "☐"} ${line.text}"
                else -> line.text
            }
            textSize = when (line.kind) {
                MarkdownLine.Kind.Heading -> 19f
                MarkdownLine.Kind.Code -> 13f
                else -> 15f
            }
            typeface = when (line.kind) {
                MarkdownLine.Kind.Heading -> Typeface.DEFAULT_BOLD
                MarkdownLine.Kind.Code -> Typeface.MONOSPACE
                else -> Typeface.DEFAULT
            }
            alpha = when (line.kind) {
                MarkdownLine.Kind.Quote -> 0.75f
                MarkdownLine.Kind.Spacer -> 0f
                else -> 1f
            }
            setTextIsSelectable(line.kind != MarkdownLine.Kind.Task)
            if (line.kind == MarkdownLine.Kind.Task && line.lineIndex != null) {
                isClickable = true
                isFocusable = true
                setOnClickListener {
                    toggleTaskLine(memo, line)
                }
            }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                if (line.kind == MarkdownLine.Kind.Spacer) dp(8) else LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                marginStart = dp(line.indentLevel * MARKDOWN_INDENT_WIDTH_DP)
                topMargin = when (line.kind) {
                    MarkdownLine.Kind.Heading -> dp(10)
                    MarkdownLine.Kind.Spacer -> 0
                    else -> dp(6)
                }
            }
        }
    }

    private fun toggleTaskLine(memo: TodoItem, lineIndex: Int, checked: Boolean) {
        val previousMemos = displayedMemos
        replaceDisplayedMemo(memo.memoId, memo.withTaskLineChecked(lineIndex, checked))
        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                AppContainer.todoRepository(this@MainActivity)
                    .setTodoChecked(memo, lineIndex, checked)
            }
            result.fold(
                onSuccess = {
                    TodoWidgetUpdater.refreshAll(this@MainActivity)
                },
                onFailure = {
                    renderMemosPreservingScroll(previousMemos)
                    showTransientError(it.message.orEmpty())
                }
            )
        }
    }

    private fun toggleTaskLine(memo: TodoItem, line: MarkdownLine) {
        val lineIndex = line.lineIndex ?: return
        val checked = !line.isChecked
        val previousMemos = displayedMemos
        replaceDisplayedMemo(memo.memoId, memo.withTaskLineChecked(lineIndex, checked))
        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                AppContainer.todoRepository(this@MainActivity)
                    .setTodoChecked(memo, lineIndex, checked)
            }
            result.fold(
                onSuccess = {
                    TodoWidgetUpdater.refreshAll(this@MainActivity)
                },
                onFailure = {
                    renderMemosPreservingScroll(previousMemos)
                    showTransientError(it.message.orEmpty())
                }
            )
        }
    }

    private fun replaceDisplayedMemo(memoId: String, updatedMemo: TodoItem) {
        val updated = displayedMemos.map { memo ->
            if (memo.memoId == memoId) updatedMemo else memo
        }
        renderMemosPreservingScroll(updated)
    }

    private fun renderMemosPreservingScroll(memos: List<TodoItem>) {
        val scrollY = binding.feedScroll.scrollY
        renderMemos(memos)
        binding.feedScroll.post {
            binding.feedScroll.scrollTo(0, scrollY)
        }
    }

    private fun archiveMemo(memo: TodoItem) {
        if (!currentSettings.isConfigured) {
            showSetup()
            return
        }

        setLoading(true)
        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    AppContainer.memosApiClient().archiveMemo(currentSettings, memo)
                }
            }
            setLoading(false)
            result.fold(
                onSuccess = {
                    Snackbar.make(binding.root, getString(R.string.archive_success), Snackbar.LENGTH_SHORT).show()
                    refreshMemos()
                    TodoWidgetUpdater.refreshAll(this@MainActivity)
                },
                onFailure = { showError(it.message.orEmpty()) }
            )
        }
    }

    private fun deleteMemo(memo: TodoItem) {
        if (!currentSettings.isConfigured) {
            showSetup()
            return
        }

        setLoading(true)
        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    AppContainer.memosApiClient().deleteMemo(currentSettings, memo)
                }
            }
            setLoading(false)
            result.fold(
                onSuccess = {
                    Snackbar.make(binding.root, getString(R.string.delete_success), Snackbar.LENGTH_SHORT).show()
                    refreshMemos()
                    TodoWidgetUpdater.refreshAll(this@MainActivity)
                },
                onFailure = { showError(it.message.orEmpty()) }
            )
        }
    }

    private fun openArchiveHistory() {
        if (!currentSettings.isConfigured) {
            showSetup()
            return
        }
        binding.drawerLayout.openDrawer(GravityCompat.START)
        loadArchiveHistory()
    }

    private fun loadArchiveHistory() {
        binding.archiveMessage.text = getString(R.string.loading)
        binding.archiveMessage.visibility = View.VISIBLE
        binding.archiveList.removeAllViews()

        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    AppContainer.memosApiClient()
                        .fetchArchivedMemos(currentSettings)
                        .sortedByDescending(TodoItem::createdAtEpochSeconds)
                }
            }
            result.fold(
                onSuccess = { archived ->
                    renderArchiveHistory(archived)
                },
                onFailure = {
                    binding.archiveMessage.text = it.message.orEmpty().ifBlank { getString(R.string.load_failed) }
                    binding.archiveMessage.visibility = View.VISIBLE
                }
            )
        }
    }

    private fun renderArchiveHistory(memos: List<TodoItem>) {
        binding.archiveList.removeAllViews()
        if (memos.isEmpty()) {
            binding.archiveMessage.text = getString(R.string.empty_archive)
            binding.archiveMessage.visibility = View.VISIBLE
            return
        }

        binding.archiveMessage.visibility = View.GONE
        memos.forEach { memo ->
            binding.archiveList.addView(createArchiveCard(memo))
        }
    }

    private fun createArchiveCard(memo: TodoItem): View {
        return MaterialCardView(this).apply {
            radius = dp(8).toFloat()
            cardElevation = dp(1).toFloat()
            useCompatPadding = true
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dp(10)
            }
            addView(LinearLayout(this@MainActivity).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(12))
                addView(TextView(this@MainActivity).apply {
                    text = formatTime(memo.createdAtEpochSeconds)
                    textSize = 12f
                    alpha = 0.65f
                })
                addView(TextView(this@MainActivity).apply {
                    text = memo.content
                    textSize = 15f
                    maxLines = 4
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        topMargin = dp(6)
                    }
                })
            })
        }
    }

    private fun startEditingMemo(memo: TodoItem) {
        editingMemo = memo
        binding.editorTitle.text = getString(R.string.edit_memo)
        binding.memoContentInput.setText(memo.rawMemoContent)
        binding.memoContentInput.setSelection(binding.memoContentInput.text?.length ?: 0)
        binding.publishButton.text = getString(R.string.save)
        binding.editorContainer.visibility = View.VISIBLE
        binding.newMemoButton.visibility = View.GONE
        binding.memoContentInput.requestFocus()
    }

    private fun startCreatingMemo() {
        editingMemo = null
        binding.memoContentInput.setText("")
        binding.publishButton.text = getString(R.string.publish)
        binding.editorTitle.text = getString(R.string.new_memo)
        binding.editorContainer.visibility = View.VISIBLE
        binding.newMemoButton.visibility = View.GONE
        binding.memoContentInput.requestFocus()
    }

    private fun closeEditor() {
        editingMemo = null
        binding.memoContentInput.setText("")
        binding.publishButton.text = getString(R.string.publish)
        binding.editorTitle.text = getString(R.string.new_memo)
        binding.editorContainer.visibility = View.GONE
        binding.newMemoButton.visibility = View.VISIBLE
    }

    private fun insertMarkdownSnippet(prefix: String, suffix: String, placeholder: String = "") {
        val editText = binding.memoContentInput
        val text = editText.text ?: return
        val start = editText.selectionStart.coerceAtLeast(0)
        val end = editText.selectionEnd.coerceAtLeast(start)
        val selected = text.substring(start, end)
        val body = selected.ifBlank { placeholder }
        val replacement = "$prefix$body$suffix"
        text.replace(start, end, replacement)
        val cursor = if (body.isBlank()) {
            start + replacement.length
        } else {
            start + prefix.length + body.length
        }
        editText.requestFocus()
        editText.setSelection(cursor.coerceIn(0, text.length))
    }

    private fun setMemoPinned(memo: TodoItem, pinned: Boolean) {
        if (!currentSettings.isConfigured) {
            showSetup()
            return
        }

        setLoading(true)
        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    AppContainer.memosApiClient().setMemoPinned(currentSettings, memo, pinned)
                }
            }
            setLoading(false)
            result.fold(
                onSuccess = {
                    Snackbar.make(
                        binding.root,
                        getString(if (pinned) R.string.pin_success else R.string.unpin_success),
                        Snackbar.LENGTH_SHORT
                    ).show()
                    refreshMemos()
                    TodoWidgetUpdater.refreshAll(this@MainActivity)
                },
                onFailure = { showError(it.message.orEmpty()) }
            )
        }
    }

    private fun showSetup() {
        binding.setupContainer.visibility = View.VISIBLE
        binding.clientContainer.visibility = View.GONE
    }

    private fun showClient() {
        binding.setupContainer.visibility = View.GONE
        binding.clientContainer.visibility = View.VISIBLE
        binding.clientTitle.text = getString(R.string.app_name)
    }

    private fun showFeedMessage(message: String) {
        binding.feedMessage.text = message
        binding.feedMessage.visibility = View.VISIBLE
        binding.memoList.removeAllViews()
    }

    private fun showError(message: String) {
        val fallback = getString(R.string.load_failed)
        Snackbar.make(binding.root, message.ifBlank { fallback }, Snackbar.LENGTH_LONG).show()
        showFeedMessage(message.ifBlank { fallback })
    }

    private fun showTransientError(message: String) {
        val fallback = getString(R.string.load_failed)
        Snackbar.make(binding.root, message.ifBlank { fallback }, Snackbar.LENGTH_LONG).show()
    }

    private fun setLoading(loading: Boolean) {
        binding.pageProgress.visibility = if (loading) View.VISIBLE else View.GONE
        binding.saveButton.isEnabled = !loading
        binding.reloadButton.isEnabled = !loading
        binding.publishButton.isEnabled = !loading
        binding.archiveHistoryButton.isEnabled = !loading
        binding.newMemoButton.isEnabled = !loading
        binding.editorCancelButton.isEnabled = !loading
    }

    private fun configureBackNavigation() {
        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
                        binding.drawerLayout.closeDrawer(GravityCompat.START)
                    } else if (binding.editorContainer.visibility == View.VISIBLE) {
                        closeEditor()
                    } else if (binding.setupContainer.visibility == View.VISIBLE && currentSettings.isConfigured) {
                        showClient()
                    } else {
                        isEnabled = false
                        onBackPressedDispatcher.onBackPressed()
                    }
                }
            }
        )
    }

    private fun isValidServerUrl(value: String): Boolean {
        return value.startsWith("https://") || value.startsWith("http://")
    }

    private fun normalizeServerUrl(value: String): String = value.trim().trimEnd('/')

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private fun formatTime(epochSeconds: Long): String {
        if (epochSeconds <= 0L) return ""
        val zoneId = ZoneId.systemDefault()
        val memoTime = Instant.ofEpochSecond(epochSeconds).atZone(zoneId)
        val now = Instant.now().atZone(zoneId)
        val hours = ChronoUnit.HOURS.between(memoTime, now)
        if (hours < 1) return "现在"
        if (hours < 24) return "${hours}小时前"

        val memoDate = memoTime.toLocalDate()
        val today = LocalDate.now(zoneId)
        val days = ChronoUnit.DAYS.between(memoDate, today)
        return when (days) {
            1L -> "昨天"
            2L -> "前天"
            else -> memoTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        }
    }

    private fun List<TodoItem>.sortForFeed(): List<TodoItem> =
        sortedWith(compareByDescending<TodoItem> { it.pinned }.thenByDescending { it.createdAtEpochSeconds })

    companion object {
        const val TAG = "MainActivity"
        const val ACTION_NEW_MEMO = "com.example.memostodowidget.action.NEW_MEMO"
        const val ACTION_OPEN_MEMO = "com.example.memostodowidget.action.OPEN_MEMO_IN_APP"
        const val EXTRA_MEMO_ID = "com.example.memostodowidget.extra.MEMO_ID"
    }
}

@Composable
private fun MemoMarkdown(
    content: String,
    onTextClick: () -> Unit,
    onTaskCheckedChange: (lineIndex: Int, checked: Boolean) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        MarkdownParser.parse(content).forEach { line ->
            val lineModifier = Modifier
                .fillMaxWidth()
                .padding(start = (line.indentLevel * MARKDOWN_INDENT_WIDTH_DP).composeDp)

            when (line.kind) {
                MarkdownLine.Kind.Spacer -> Spacer(modifier = Modifier.height(8.composeDp))

                MarkdownLine.Kind.Task -> {
                    val lineIndex = line.lineIndex
                    Row(
                        modifier = lineModifier
                            .clickable(enabled = lineIndex != null) {
                                if (lineIndex != null) {
                                    onTaskCheckedChange(lineIndex, !line.isChecked)
                                }
                            }
                            .padding(vertical = 2.composeDp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = line.isChecked,
                            onCheckedChange = if (lineIndex == null) {
                                null
                            } else {
                                { checked -> onTaskCheckedChange(lineIndex, checked) }
                            }
                        )
                        Text(
                            text = line.text,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(start = 2.composeDp)
                        )
                    }
                }

                else -> {
                    Text(
                        text = line.text,
                        style = when (line.kind) {
                            MarkdownLine.Kind.Heading -> MaterialTheme.typography.titleMedium
                            MarkdownLine.Kind.Code -> MaterialTheme.typography.bodyMedium
                            else -> MaterialTheme.typography.bodyLarge
                        },
                        modifier = lineModifier
                            .clickable(onClick = onTextClick)
                            .padding(vertical = 3.composeDp)
                    )
                }
            }
        }
    }
}

private fun TodoItem.withTaskLineChecked(lineIndex: Int, checked: Boolean): TodoItem {
    val lines = rawMemoContent.split('\n').toMutableList()
    if (lineIndex !in lines.indices) return this

    val match = CHECKBOX_MARKDOWN_STATE.find(lines[lineIndex]) ?: return this
    lines[lineIndex] = lines[lineIndex].replaceRange(
        match.range,
        "${match.groupValues[1]}[${if (checked) "x" else " "}]"
    )
    val updatedRawContent = lines.joinToString("\n")
    return copy(
        rawMemoContent = updatedRawContent,
        taskLines = taskLines.map { taskLine ->
            if (taskLine.lineIndex == lineIndex) {
                taskLine.copy(isChecked = checked)
            } else {
                taskLine
            }
        }
    )
}

private const val MARKDOWN_INDENT_WIDTH_DP = 18
private val CHECKBOX_MARKDOWN_STATE = Regex("^(\\s*(?:[-*+]\\s+)?)(?:\\[[ xX]\\])")
