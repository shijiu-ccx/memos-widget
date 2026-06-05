package com.example.memostodowidget.widget

object MarkdownParser {
    fun parse(content: String): List<MarkdownLine> =
        content.lines().mapIndexedNotNull { index, rawLine ->
            val line = rawLine.trimEnd()
            val trimmed = line.trim()

            when {
                trimmed.equals("#todo", ignoreCase = true) -> null
                trimmed.isBlank() -> MarkdownLine("", MarkdownLine.Kind.Spacer)
                TASK_WHOLE_LINE.matches(trimmed) -> parseTask(index, trimmed)
                HEADING_LINE.matches(trimmed) -> MarkdownLine(
                    text = cleanInline(trimmed.replace(HEADING_LINE, "")),
                    kind = MarkdownLine.Kind.Heading
                )
                trimmed.startsWith(">") -> MarkdownLine(
                    text = cleanInline(trimmed.removePrefix(">").trim()),
                    kind = MarkdownLine.Kind.Quote
                )
                BULLET_LINE.matches(trimmed) -> MarkdownLine(
                    text = "• ${cleanInline(trimmed.replace(BULLET_LINE, ""))}",
                    kind = MarkdownLine.Kind.Bullet
                )
                NUMBERED_LINE.matches(trimmed) -> MarkdownLine(
                    text = cleanInline(trimmed),
                    kind = MarkdownLine.Kind.Bullet
                )
                trimmed.startsWith("```") -> null
                trimmed.startsWith("`") && trimmed.endsWith("`") -> MarkdownLine(
                    text = trimmed.trim('`'),
                    kind = MarkdownLine.Kind.Code
                )
                else -> MarkdownLine(
                    text = cleanInline(trimmed),
                    kind = MarkdownLine.Kind.Paragraph
                )
            }
        }.ifEmpty {
            listOf(MarkdownLine("(空 memo)", MarkdownLine.Kind.Paragraph))
        }

    private fun parseTask(index: Int, trimmed: String): MarkdownLine {
        val match = TASK_PREFIX.find(trimmed) ?: error("Invalid task line")
        val isChecked = match.groupValues[1].equals("x", ignoreCase = true)
        val text = trimmed.substring(match.range.last + 1).trim().ifBlank { "(空待办)" }
        return MarkdownLine(
            text = cleanInline(text),
            kind = MarkdownLine.Kind.Task,
            lineIndex = index,
            isChecked = isChecked
        )
    }

    private fun cleanInline(value: String): String =
        value.replace(Regex("`([^`]+)`"), "$1")
            .replace(Regex("\\*\\*([^*]+)\\*\\*"), "$1")
            .replace(Regex("__([^_]+)__"), "$1")
            .replace(Regex("\\*([^*]+)\\*"), "$1")
            .replace(Regex("_([^_]+)_"), "$1")
            .replace(Regex("\\[([^]]+)]\\([^)]+\\)"), "$1")
            .trim()

    private val TASK_WHOLE_LINE = Regex("^\\s*(?:[-*+]\\s+)?\\[([ xX])\\]\\s+.*")
    private val TASK_PREFIX = Regex("^\\s*(?:[-*+]\\s+)?\\[([ xX])\\]\\s+")
    private val HEADING_LINE = Regex("^#{1,6}\\s+")
    private val BULLET_LINE = Regex("^\\s*[-*+]\\s+")
    private val NUMBERED_LINE = Regex("^\\s*\\d+[.)]\\s+.*")
}
