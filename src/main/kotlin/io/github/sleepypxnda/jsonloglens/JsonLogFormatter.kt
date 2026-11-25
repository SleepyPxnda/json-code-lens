package io.github.sleepypxnda.jsonloglens

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.JsonSyntaxException

/**
 * Renders a single console line that contains a JSON object through a template such as
 * `{timestamp} {level} {#logger}[{logger}] {/logger}{message}`.
 *
 * - `{fieldPath}` is replaced with the value of that field; missing or null fields become an empty string.
 *   A path like `log.level` first matches a top-level key named exactly `"log.level"`, otherwise the
 *   nested field `"log": {"level": ...}`.
 * - `{#fieldPath}...{/fieldPath}` is a section that is only shown if the field is present and not empty.
 * - `\n`, `\t` and `\\` in the template become a line break, a tab and a backslash.
 */
object JsonLogFormatter {
    private val placeholderPattern = Regex("""\{([^{}]+)}""")
    private val sectionPattern = Regex("""\{#([^{}]+)}(.*?)\{/\1}""", RegexOption.DOT_MATCHES_ALL)
    private val escapeSequencePattern = Regex("""\\([nt\\])""")
    private const val CARRIAGE_RETURN = "\r"
    private const val LINE_FEED = "\n"
    private const val FIELD_PATH_SEPARATOR = "."

    /**
     * Formats every line of a console text chunk that is a JSON object.
     * Returns null if no line was formatted, so the console can show the original text unchanged.
     * A trailing carriage return is kept: consoles that emulate a terminal (PTY) send `\r\n`,
     * and without the `\r` each following line would start at the column where the previous one ended.
     */
    fun formatTextOrNull(text: String, template: String): String? {
        var anyLineWasFormatted = false
        val outputText = text.split(LINE_FEED).joinToString(LINE_FEED) { line ->
            val formattedLine = formatLineOrNull(line, template) ?: return@joinToString line
            anyLineWasFormatted = true
            if (line.endsWith(CARRIAGE_RETURN)) {
                // Line breaks from the template need the carriage return as well.
                formattedLine.replace(LINE_FEED, CARRIAGE_RETURN + LINE_FEED) + CARRIAGE_RETURN
            } else {
                formattedLine
            }
        }
        return if (anyLineWasFormatted) outputText else null
    }

    fun isJsonObjectLine(line: String): Boolean = parseJsonObjectOrNull(line.trim()) != null

    /** Returns the formatted line, or null if the line is not a JSON object and should stay unchanged. */
    fun formatLineOrNull(line: String, template: String): String? {
        val logEntry = parseJsonObjectOrNull(line.trim()) ?: return null
        // Sections are resolved first and placeholders replaced in a single pass afterwards,
        // so field values that contain braces (e.g. nested objects) are never treated as placeholders.
        val templateWithResolvedSections = resolveSections(resolveEscapeSequences(template), logEntry)
        return placeholderPattern.replace(templateWithResolvedSections) { placeholderMatch ->
            fieldValueAsText(logEntry, placeholderMatch.groupValues[1])
        }
    }

    /**
     * Turns `\n` into a line break, `\t` into a tab and `\\` into a backslash, because the template is a
     * single-line text field. Only the template is unescaped, never field values. Other backslashes stay as they are.
     */
    private fun resolveEscapeSequences(template: String): String =
        escapeSequencePattern.replace(template) { escapeMatch ->
            when (val escapedCharacter = escapeMatch.groupValues[1]) {
                "n" -> LINE_FEED
                "t" -> "\t"
                "\\" -> "\\"
                else -> error("Escape pattern matched unsupported character: $escapedCharacter")
            }
        }

    /** Keeps the content of every section whose field is present (resolving nested sections in it) and drops the others. */
    private fun resolveSections(template: String, logEntry: JsonObject): String =
        sectionPattern.replace(template) { sectionMatch ->
            val fieldPath = sectionMatch.groupValues[1]
            val sectionContent = sectionMatch.groupValues[2]
            if (fieldValueAsText(logEntry, fieldPath).isEmpty()) "" else resolveSections(sectionContent, logEntry)
        }

    private fun fieldValueAsText(logEntry: JsonObject, fieldPath: String): String {
        val fieldValue = findFieldValueOrNull(logEntry, fieldPath)
        return when {
            fieldValue == null || fieldValue.isJsonNull -> ""
            fieldValue.isJsonPrimitive -> fieldValue.asString
            else -> fieldValue.toString()
        }
    }

    private fun findFieldValueOrNull(logEntry: JsonObject, fieldPath: String): JsonElement? {
        // A literal key wins, so flat ECS keys like "log.level" keep working.
        logEntry.get(fieldPath)?.let { literalFieldValue -> return literalFieldValue }
        var currentElement: JsonElement = logEntry
        for (pathSegment in fieldPath.split(FIELD_PATH_SEPARATOR)) {
            if (!currentElement.isJsonObject) return null
            currentElement = currentElement.asJsonObject.get(pathSegment) ?: return null
        }
        return currentElement
    }

    private fun parseJsonObjectOrNull(trimmedLine: String): JsonObject? {
        // Cheap pre-check so ordinary log lines never reach the JSON parser.
        if (!trimmedLine.startsWith("{") || !trimmedLine.endsWith("}")) return null
        val parsedElement = try {
            JsonParser.parseString(trimmedLine)
        } catch (notJson: JsonSyntaxException) {
            return null
        }
        return if (parsedElement.isJsonObject) parsedElement.asJsonObject else null
    }
}
