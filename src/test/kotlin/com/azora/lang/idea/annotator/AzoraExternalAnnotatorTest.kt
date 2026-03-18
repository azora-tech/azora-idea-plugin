/*
 * Copyright 2026 AzoraTech
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.azora.lang.idea.annotator

import com.intellij.lang.annotation.HighlightSeverity
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

/**
 * Unit tests for [AzoraExternalAnnotator] diagnostics.
 *
 * Tests the bracket balance and unterminated string detection logic
 * by calling [AzoraExternalAnnotator.doAnnotate] directly.
 */
class AzoraExternalAnnotatorTest {

    private val annotator = AzoraExternalAnnotator()

    private fun annotate(source: String): List<AzoraDiagnostic> {
        val info = AzoraAnnotationInfo(source, "test.az", "/test.az")
        return annotator.doAnnotate(info).diagnostics
    }

    // ── Balanced brackets ──────────────────────────────────────────────

    @Test
    fun `balanced brackets produce no diagnostics`() {
        val diags = annotate("func main() { println(\"hello\") }")
        assertTrue(diags.isEmpty(), "Expected no diagnostics, got: $diags")
    }

    @Test
    fun `nested balanced brackets produce no diagnostics`() {
        val diags = annotate("func test() { if true { [1, 2, 3] } }")
        assertTrue(diags.isEmpty())
    }

    // ── Unmatched brackets ─────────────────────────────────────────────

    @Test
    fun `unmatched opening brace reports error`() {
        val diags = annotate("func main() {")
        assertTrue(diags.isNotEmpty())
        assertEquals(HighlightSeverity.ERROR, diags[0].severity)
    }

    @Test
    fun `unmatched closing paren reports error`() {
        val diags = annotate("func main )")
        assertTrue(diags.isNotEmpty())
        assertEquals(HighlightSeverity.ERROR, diags[0].severity)
    }

    // ── Brackets in strings and comments are ignored ───────────────────

    @Test
    fun `brackets inside strings are ignored`() {
        val diags = annotate("var s = \"({[\"")
        assertTrue(diags.isEmpty(), "Brackets in strings should be ignored")
    }

    @Test
    fun `brackets inside line comments are ignored`() {
        val diags = annotate("// { ( [")
        assertTrue(diags.isEmpty())
    }

    @Test
    fun `brackets inside block comments are ignored`() {
        val source = "/* { ( [ */ func main() {}"
        val diags = annotate(source)
        assertTrue(diags.isEmpty())
    }

    // ── Unterminated strings ───────────────────────────────────────────

    @Test
    fun `unterminated string reports error`() {
        val diags = annotate("var s = \"hello")
        assertTrue(diags.any { it.message.contains("Unterminated") })
    }

    @Test
    fun `properly terminated string produces no error`() {
        val diags = annotate("var s = \"hello world\"")
        assertTrue(diags.none { it.message.contains("Unterminated") })
    }

    @Test
    fun `string with escapes is not false positive`() {
        val diags = annotate("var s = \"hello \\\"world\\\"\"")
        assertTrue(diags.none { it.message.contains("Unterminated") })
    }

    @Test
    fun `string inside comment does not trigger unterminated`() {
        val diags = annotate("// var s = \"unterminated")
        assertTrue(diags.none { it.message.contains("Unterminated") })
    }

    // ── Complex cases ──────────────────────────────────────────────────

    @Test
    fun `complete program produces no diagnostics`() {
        val source = """
            package example

            func main() {
                var x = 42
                if x > 0 {
                    println("positive")
                }
            }
        """.trimIndent()
        val diags = annotate(source)
        assertTrue(diags.isEmpty(), "Expected no diagnostics, got: $diags")
    }
}
