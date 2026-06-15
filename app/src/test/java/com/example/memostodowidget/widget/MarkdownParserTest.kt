package com.example.memostodowidget.widget

import org.junit.Assert.assertEquals
import org.junit.Test

class MarkdownParserTest {
    @Test
    fun parsePreservesIndentedCheckboxTasks() {
        val lines = MarkdownParser.parse(
            """
            - [ ] parent
              [x] space child
              - [ ] list child
                - [x] nested list child
            """.trimIndent()
        )

        assertEquals(4, lines.size)
        assertEquals(listOf(0, 1, 1, 2), lines.map { it.indentLevel })
        assertEquals(listOf(false, true, false, true), lines.map { it.isChecked })
        assertEquals(listOf("parent", "space child", "list child", "nested list child"), lines.map { it.text })
        assertEquals(listOf(0, 1, 2, 3), lines.map { it.lineIndex })
        assertEquals(List(4) { MarkdownLine.Kind.Task }, lines.map { it.kind })
    }

    @Test
    fun parsePreservesIndentedPlainListItems() {
        val lines = MarkdownParser.parse(
            """
            - parent
              - child
            """.trimIndent()
        )

        assertEquals(listOf(0, 1), lines.map { it.indentLevel })
        assertEquals(listOf(MarkdownLine.Kind.Bullet, MarkdownLine.Kind.Bullet), lines.map { it.kind })
    }
}
