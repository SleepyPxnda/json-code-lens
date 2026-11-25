package io.github.sleepypxnda.jsonloglens

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class JsonLogFormatterTest {
    private val template = "{timestamp} {level} [{logger}] {message}"

    @Test
    fun `formats a JSON object line with the template`() {
        val line = """{"timestamp":"12:00:01","level":"INFO","logger":"App","message":"Started"}"""
        assertEquals("12:00:01 INFO [App] Started", JsonLogFormatter.formatLineOrNull(line, template))
    }

    @Test
    fun `missing and null fields become empty`() {
        val line = """{"level":"WARN","logger":null,"message":"Hi"}"""
        assertEquals(" WARN [] Hi", JsonLogFormatter.formatLineOrNull(line, template))
    }

    @Test
    fun `field names with dots are matched literally`() {
        val line = """{"log.level":"ERROR","message":"Boom"}"""
        assertEquals("ERROR Boom", JsonLogFormatter.formatLineOrNull(line, "{log.level} {message}"))
    }

    @Test
    fun `dotted paths resolve nested fields`() {
        val line = """{"log":{"level":"WARN","origin":{"file":"Main.kt"}},"message":"Hi"}"""
        assertEquals(
            "WARN Main.kt Hi",
            JsonLogFormatter.formatLineOrNull(line, "{log.level} {log.origin.file} {message}"),
        )
    }

    @Test
    fun `literal dotted key wins over nested path`() {
        val line = """{"log.level":"FLAT","log":{"level":"NESTED"}}"""
        assertEquals("FLAT", JsonLogFormatter.formatLineOrNull(line, "{log.level}"))
    }

    @Test
    fun `path through a non object field is empty`() {
        val line = """{"log":"text"}"""
        assertEquals("", JsonLogFormatter.formatLineOrNull(line, "{log.level}"))
    }

    @Test
    fun `section is shown only when its field is present and not empty`() {
        val sectionTemplate = "{level} {#logger}[{logger}] {/logger}{message}"
        assertEquals(
            "INFO [App] Hi",
            JsonLogFormatter.formatLineOrNull("""{"level":"INFO","logger":"App","message":"Hi"}""", sectionTemplate),
        )
        assertEquals(
            "INFO Hi",
            JsonLogFormatter.formatLineOrNull("""{"level":"INFO","message":"Hi"}""", sectionTemplate),
        )
        assertEquals(
            "INFO Hi",
            JsonLogFormatter.formatLineOrNull("""{"level":"INFO","logger":"","message":"Hi"}""", sectionTemplate),
        )
    }

    @Test
    fun `sections can be nested and use paths`() {
        val nestedTemplate = "{message}{#error} ({error.type}{#error.cause}: {error.cause}{/error.cause}){/error}"
        assertEquals(
            "Boom (IOException: disk full)",
            JsonLogFormatter.formatLineOrNull(
                """{"message":"Boom","error":{"type":"IOException","cause":"disk full"}}""",
                nestedTemplate,
            ),
        )
        assertEquals(
            "Boom (IOException)",
            JsonLogFormatter.formatLineOrNull("""{"message":"Boom","error":{"type":"IOException"}}""", nestedTemplate),
        )
        assertEquals("Boom", JsonLogFormatter.formatLineOrNull("""{"message":"Boom"}""", nestedTemplate))
    }

    @Test
    fun `object values inside a section are not treated as placeholders`() {
        val line = """{"context":{"user":"anna"}}"""
        assertEquals(
            "ctx={\"user\":\"anna\"}",
            JsonLogFormatter.formatLineOrNull(line, "{#context}ctx={context}{/context}"),
        )
    }

    @Test
    fun `nested objects and numbers are rendered as JSON text`() {
        val line = """{"status":200,"context":{"user":"anna"}}"""
        assertEquals("200 {\"user\":\"anna\"}", JsonLogFormatter.formatLineOrNull(line, "{status} {context}"))
    }

    @Test
    fun `carriage returns of terminal line endings are kept`() {
        val text = "{\"message\":\"one\"}\r\n{\"message\":\"two\"}\r\n"
        assertEquals("one\r\ntwo\r\n", JsonLogFormatter.formatTextOrNull(text, "{message}"))
    }

    @Test
    fun `escape sequences in the template become tab, line break and backslash`() {
        val line = """{"level":"INFO","message":"Hi"}"""
        assertEquals("INFO\tHi\n  C:\\x", JsonLogFormatter.formatLineOrNull(line, """{level}\t{message}\n  C:\\x"""))
    }

    @Test
    fun `other backslashes in the template stay as they are`() {
        val line = """{"message":"Hi"}"""
        assertEquals("""C:\data Hi""", JsonLogFormatter.formatLineOrNull(line, """C:\data {message}"""))
    }

    @Test
    fun `escape sequences are not applied to field values`() {
        val line = """{"message":"a\\nb"}"""
        assertEquals("""a\nb""", JsonLogFormatter.formatLineOrNull(line, "{message}"))
    }

    @Test
    fun `template line breaks get a carriage return in terminal output`() {
        val text = "{\"level\":\"INFO\",\"message\":\"Hi\"}\r\n"
        assertEquals("INFO\r\nHi\r\n", JsonLogFormatter.formatTextOrNull(text, """{level}\n{message}"""))
    }

    @Test
    fun `text without JSON lines is left unchanged`() {
        assertNull(JsonLogFormatter.formatTextOrNull("plain\r\nlines\n", "{message}"))
    }

    @Test
    fun `non JSON lines are left unchanged`() {
        assertNull(JsonLogFormatter.formatLineOrNull("Plain log line", template))
        assertNull(JsonLogFormatter.formatLineOrNull("{ not json }", template))
        assertNull(JsonLogFormatter.formatLineOrNull("[1, 2]", template))
    }
}
