/*
 * Copyright 2026 PinkRoccade Local Government B.V.
 *
 * Licensed under EUPL, Version 1.2 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package nl.nlportal.architecture

/**
 * Reads [ModuleLayers.production] from the source text of ModuleLayers.kt, so the PR summary can
 * compare against the base branch without building it. Understands only the shape ModuleLayers.kt
 * uses: positional `ModuleMapping(module or listOf(modules), prefix, Layer.X, description)` entries.
 * [ModuleLayersSourceTest] keeps this in step with the real file.
 */
object ModuleLayersSource {
    /** ModuleLayers.kt relative to the architecture-test project directory. */
    const val PATH = "src/test/kotlin/nl/nlportal/architecture/ModuleLayers.kt"

    fun parse(source: String): List<ModuleMapping> {
        val tokens = tokenize(source)
        val start = tokens.indexOf(Token.Word("production"))
        require(start >= 0) { "ModuleLayers.production niet gevonden in de broncode" }
        // Older versions have no exclusions; then production runs to the end of the file.
        val end = tokens.indexOf(Token.Word("excluded")).takeIf { it > start } ?: tokens.size
        val section = tokens.subList(start, end)
        return section.indices
            .filter { section[it] == Token.Word("ModuleMapping") && section.getOrNull(it + 1) == Token.Punct('(') }
            .map { Entry(section, it + 2).read() }
    }

    private sealed interface Token {
        data class Word(
            val text: String,
        ) : Token

        data class Str(
            val value: String,
        ) : Token

        data class Punct(
            val char: Char,
        ) : Token
    }

    /** Reads one entry starting just after `ModuleMapping(`. */
    private class Entry(
        private val tokens: List<Token>,
        private var pos: Int,
    ) {
        fun read(): ModuleMapping {
            val modules =
                if (peek() == Token.Word("listOf")) {
                    pos++
                    expect(Token.Punct('('))
                    buildList {
                        while (peek() != Token.Punct(')')) {
                            add(string())
                            skip(Token.Punct(','))
                        }
                        pos++
                    }
                } else {
                    listOf(string())
                }
            expect(Token.Punct(','))
            val prefix = string()
            expect(Token.Punct(','))
            val layer = (next() as? Token.Word)?.text?.removePrefix("Layer.") ?: fail("een laag (Layer.X)")
            skip(Token.Punct(','))
            val description = (peek() as? Token.Str)?.let { string() } ?: ""
            skip(Token.Punct(','))
            expect(Token.Punct(')'))
            return ModuleMapping(modules, prefix, Layer.valueOf(layer), description)
        }

        private fun peek() = tokens.getOrNull(pos)

        private fun next() = tokens.getOrNull(pos++)

        private fun string() = (next() as? Token.Str)?.value ?: fail("een tekst tussen aanhalingstekens")

        private fun skip(token: Token) {
            if (peek() == token) pos++
        }

        private fun expect(token: Token) {
            if (next() != token) fail("$token")
        }

        private fun fail(expected: String): Nothing = throw IllegalArgumentException("ModuleMapping in ModuleLayers.kt niet te lezen: verwacht $expected bij token ${pos - 1}")
    }

    /** Words (identifiers, dotted), string literals and punctuation; comments and whitespace are dropped. */
    private fun tokenize(source: String): List<Token> {
        val tokens = mutableListOf<Token>()
        var i = 0
        while (i < source.length) {
            val c = source[i]
            when {
                c.isWhitespace() -> {
                    i++
                }

                source.startsWith("//", i) -> {
                    i = source.indexOf('\n', i).let { if (it < 0) source.length else it }
                }

                source.startsWith("/*", i) -> {
                    i = source.indexOf("*/", i + 2).let { if (it < 0) source.length else it + 2 }
                }

                c == '"' -> {
                    val value = StringBuilder()
                    i++
                    while (i < source.length && source[i] != '"') {
                        if (source[i] == '\\' && i + 1 < source.length) i++
                        value.append(source[i++])
                    }
                    i++
                    tokens += Token.Str(value.toString())
                }

                c.isLetterOrDigit() || c == '_' -> {
                    val startOfWord = i
                    while (i < source.length && (source[i].isLetterOrDigit() || source[i] == '_' || source[i] == '.')) i++
                    tokens += Token.Word(source.substring(startOfWord, i))
                }

                else -> {
                    tokens += Token.Punct(source[i++])
                }
            }
        }
        return tokens
    }
}