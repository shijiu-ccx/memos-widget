package com.example.memostodowidget.widget

data class MarkdownLine(
    val text: String,
    val kind: Kind,
    val lineIndex: Int? = null,
    val isChecked: Boolean = false
) {
    enum class Kind {
        Heading,
        Task,
        Bullet,
        Quote,
        Code,
        Paragraph,
        Spacer
    }
}

