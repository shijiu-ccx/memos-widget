package com.example.memostodowidget.widget

data class MarkdownLine(
    val text: String,
    val kind: Kind,
    val lineIndex: Int? = null,
    val isChecked: Boolean = false,
    val indentLevel: Int = 0
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

