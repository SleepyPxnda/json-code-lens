package io.github.sleepypxnda.jsonloglens

import org.junit.Assert.assertEquals
import org.junit.Test

class SplitJsonLineJoinerTest {
    private val reportedLogLine =
        """{"time":"2026-11-25T16:16:28.035536+01:00","level":"INFO","source":{"function":"http.logRequest.func1",""" +
            """"file":"http/http.go","line":145},"msg":"finished request","method":"GET",""" +
            """"url":"/api/v1/job/0721f0b1-75d9-427c-b94d-82aae0c502e5/image/Teams_Backgrounds_3.png",""" +
            """"duration":"69.061917ms","x-trace-id":"7979c080-334e-43f5-be80-dc55dede3e89",""" +
            """"trace_id":"5aa1302654c2e95ccd0fa0f133a227e2","span_id":"20e651c810e5b6a2"}"""

    @Test
    fun `reported line is formatted when it arrives in one piece`() {
        assertEquals(
            "INFO finished request http/http.go:145",
            JsonLogFormatter.formatLineOrNull(reportedLogLine, "{level} {msg} {source.file}:{source.line}"),
        )
    }

    @Test
    fun `JSON line split across two chunks is joined before it is shown`() {
        val splitLineJoiner = SplitJsonLineJoiner()
        val splitPosition = 200
        val firstChunk = "plain line\r\n" + reportedLogLine.take(splitPosition)
        val secondChunk = reportedLogLine.drop(splitPosition) + "\r\n"

        assertEquals("plain line\r\n", splitLineJoiner.textToShowForChunk(firstChunk))
        assertEquals(reportedLogLine + "\r\n", splitLineJoiner.textToShowForChunk(secondChunk))
    }

    @Test
    fun `whole chunk is held back while it is only the start of a JSON line`() {
        val splitLineJoiner = SplitJsonLineJoiner()
        assertEquals("", splitLineJoiner.textToShowForChunk("""{"level":"IN"""))
        assertEquals("", splitLineJoiner.textToShowForChunk("""FO","msg":"""))
        assertEquals("""{"level":"INFO","msg":"Hi"}""" + "\n", splitLineJoiner.textToShowForChunk(""""Hi"}""" + "\n"))
    }

    @Test
    fun `complete JSON object without line break is passed on at once`() {
        val splitLineJoiner = SplitJsonLineJoiner()
        assertEquals("""{"msg":"Hi"}""", splitLineJoiner.textToShowForChunk("""{"msg":"Hi"}"""))
    }

    @Test
    fun `fragment that does not start like JSON is passed on at once`() {
        val splitLineJoiner = SplitJsonLineJoiner()
        assertEquals("Enter your name: ", splitLineJoiner.textToShowForChunk("Enter your name: "))
    }

    @Test
    fun `held back fragment can be taken out`() {
        val splitLineJoiner = SplitJsonLineJoiner()
        splitLineJoiner.textToShowForChunk("""{"msg":""")
        assertEquals("""{"msg":""", splitLineJoiner.takeHeldBackFragment())
        assertEquals("", splitLineJoiner.takeHeldBackFragment())
    }
}
