package com.example.memostodowidget.widget

object MarkdownParser {
    fun parse(content: String): List<MarkdownLine> =
        content.lines().mapIndexedNotNull { index, rawLine ->
            val line = rawLine.trimEnd()
            val trimmed = line.trim()
            val indentLevel = indentLevel(line)

            when {
                trimmed.equals("#todo", ignoreCase = true) -> null
                trimmed.isBlank() -> MarkdownLine("", MarkdownLine.Kind.Spacer)
                TASK_WHOLE_LINE.matches(line) -> parseTask(index, line)
                HEADING_PREFIX.containsMatchIn(trimmed) -> MarkdownLine(
                    text = cleanInline(trimmed.replace(HEADING_PREFIX, "")),
                    kind = MarkdownLine.Kind.Heading,
                    indentLevel = indentLevel
                )
                trimmed.startsWith(">") -> MarkdownLine(
                    text = cleanInline(trimmed.removePrefix(">").trim()),
                    kind = MarkdownLine.Kind.Quote,
                    indentLevel = indentLevel
                )
                BULLET_PREFIX.containsMatchIn(trimmed) -> MarkdownLine(
                    text = "• ${cleanInline(trimmed.replace(BULLET_PREFIX, ""))}",
                    kind = MarkdownLine.Kind.Bullet,
                    indentLevel = indentLevel
                )
                NUMBERED_LINE.matches(trimmed) -> MarkdownLine(
                    text = cleanInline(trimmed),
                    kind = MarkdownLine.Kind.Bullet,
                    indentLevel = indentLevel
                )
                trimmed.startsWith("```") -> null
                trimmed.startsWith("`") && trimmed.endsWith("`") -> MarkdownLine(
                    text = trimmed.trim('`'),
                    kind = MarkdownLine.Kind.Code,
                    indentLevel = indentLevel
                )
                else -> MarkdownLine(
                    text = cleanInline(trimmed),
                    kind = MarkdownLine.Kind.Paragraph,
                    indentLevel = indentLevel
                )
            }
        }.ifEmpty {
            listOf(MarkdownLine("(空 memo)", MarkdownLine.Kind.Paragraph))
        }

    private fun parseTask(index: Int, line: String): MarkdownLine {
        val match = TASK_PREFIX.find(line) ?: error("Invalid task line")
        val isChecked = match.groupValues[1].equals("x", ignoreCase = true)
        val text = line.substring(match.range.last + 1).trim().ifBlank { "(空待办)" }
        return MarkdownLine(
            text = cleanInline(text),
            kind = MarkdownLine.Kind.Task,
            lineIndex = index,
            isChecked = isChecked,
            indentLevel = indentLevel(line)
        )
    }

    private fun indentLevel(line: String): Int {
        var width = 0
        for (char in line) {
            when (char) {
                ' ' -> width += 1
                '\t' -> width += SPACES_PER_INDENT
                else -> break
            }
        }
        return (width / SPACES_PER_INDENT).coerceAtMost(MAX_INDENT_LEVEL)
    }

    private fun cleanInline(value: String): String =
        value.replace(Regex("`([^`]+)`"), "$1")
            .replace(Regex("\\*\\*([^*]+)\\*\\*"), "$1")
            .replace(Regex("__([^_]+)__"), "$1")
            .replace(Regex("\\*([^*]+)\\*"), "$1")
            .replace(Regex("_([^_]+)_"), "$1")
            .replace(Regex("\\[([^]]+)]\\([^)]+\\)"), "$1")
            .trim()

    private const val SPACES_PER_INDENT = 2
    private const val MAX_INDENT_LEVEL = 6
    private val TASK_WHOLE_LINE = Regex("^\\s*(?:[-*+]\\s+)?\\[([ xX])\\]\\s+.*")
    private val TASK_PREFIX = Regex("^\\s*(?:[-*+]\\s+)?\\[([ xX])\\]\\s+")
    private val HEADING_PREFIX = Regex("^#{1,6}\\s+")
    private val BULLET_PREFIX = Regex("^\\s*[-*+]\\s+")
    private val NUMBERED_LINE = Regex("^\\s*\\d+[.)]\\s+.*")
}
