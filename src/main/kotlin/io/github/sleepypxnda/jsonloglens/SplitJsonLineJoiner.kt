package io.github.sleepypxnda.jsonloglens

/**
 * The console receives process output in chunks that do not always end at a line break, so a long
 * JSON line can arrive in two pieces and neither piece alone is a JSON object. This joiner holds back
 * a trailing fragment that starts like a JSON object until the rest of its line arrives.
 *
 * Only such fragments are held back: other output (e.g. a prompt without a line break) is passed on
 * at once. A fragment that is already a complete JSON object is passed on as well.
 */
class SplitJsonLineJoiner {
    private var heldBackFragment = ""

    /** Returns the text that can be shown now; it is empty while the whole chunk is held back. */
    fun textToShowForChunk(chunk: String): String {
        val pendingText = heldBackFragment + chunk
        val trailingFragment = pendingText.substringAfterLast(LINE_FEED)
        heldBackFragment = if (isStartOfIncompleteJsonObject(trailingFragment)) trailingFragment else ""
        return pendingText.dropLast(heldBackFragment.length)
    }

    /** Returns the held back fragment and forgets it, e.g. when formatting was turned off in the meantime. */
    fun takeHeldBackFragment(): String = heldBackFragment.also { heldBackFragment = "" }

    private fun isStartOfIncompleteJsonObject(fragment: String): Boolean =
        fragment.trimStart().startsWith("{") && !JsonLogFormatter.isJsonObjectLine(fragment)

    private companion object {
        const val LINE_FEED = "\n"
    }
}
