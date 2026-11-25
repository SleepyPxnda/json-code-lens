package io.github.sleepypxnda.jsonloglens

import com.intellij.execution.ui.ConsoleViewContentType
import com.intellij.openapi.util.Pair
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class JsonLogInputFilterTest {
    private val template = "{level} {msg}"
    private val standardOutput = ConsoleViewContentType.NORMAL_OUTPUT
    private val errorOutput = ConsoleViewContentType.ERROR_OUTPUT
    private val systemOutput = ConsoleViewContentType.SYSTEM_OUTPUT

    @Test
    fun `split JSON line is held back and formatted once complete`() {
        val inputFilter = JsonLogInputFilter()
        assertEquals(emptyList<Any>(), inputFilter.filterText("""{"level":"INFO","ms""", standardOutput, true, template))
        assertEquals(
            listOf(Pair("INFO Hi\n", standardOutput)),
            inputFilter.filterText("""g":"Hi"}""" + "\n", standardOutput, true, template),
        )
    }

    @Test
    fun `held back fragments are released before system output`() {
        val inputFilter = JsonLogInputFilter()
        inputFilter.filterText("""{"msg":""", standardOutput, true, template)
        inputFilter.filterText("""{"level":"WARN","msg":"Boom"}""" + "\n" + """{"level":"ERROR"""", errorOutput, true, template)

        assertEquals(
            listOf(
                Pair("""{"msg":""", standardOutput),
                Pair("""{"level":"ERROR"""", errorOutput),
                Pair("Process finished with exit code 1\n", systemOutput),
            ),
            inputFilter.filterText("Process finished with exit code 1\n", systemOutput, true, template),
        )
    }

    @Test
    fun `held back fragments are released when formatting is turned off`() {
        val inputFilter = JsonLogInputFilter()
        inputFilter.filterText("""{"level":"INFO",""", standardOutput, true, template)
        assertEquals(
            listOf(Pair("""{"level":"INFO",""", standardOutput), Pair(""""msg":"Hi"}""" + "\n", standardOutput)),
            inputFilter.filterText(""""msg":"Hi"}""" + "\n", standardOutput, false, template),
        )
    }

    @Test
    fun `plain text and user input stay unchanged`() {
        val inputFilter = JsonLogInputFilter()
        assertNull(inputFilter.filterText("plain line\n", standardOutput, true, template))
        assertNull(inputFilter.filterText("{", ConsoleViewContentType.USER_INPUT, true, template))
    }
}
