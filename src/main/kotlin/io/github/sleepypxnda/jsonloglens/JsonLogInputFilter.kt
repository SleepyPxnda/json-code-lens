package io.github.sleepypxnda.jsonloglens

import com.intellij.execution.filters.ConsoleInputFilterProvider
import com.intellij.execution.filters.InputFilter
import com.intellij.execution.ui.ConsoleViewContentType
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Pair

class JsonLogInputFilterProvider : ConsoleInputFilterProvider {
    // Called once per console, so every console gets its own filter and its own held back fragments.
    override fun getDefaultFilters(project: Project): Array<InputFilter> = arrayOf(JsonLogInputFilter())
}

/** Rewrites every console line that is a JSON object using the template from the settings. */
class JsonLogInputFilter : InputFilter {
    // One joiner per output stream (stdout, stderr, ...), because their chunks are interleaved.
    private val splitLineJoinersByContentType = LinkedHashMap<ConsoleViewContentType, SplitJsonLineJoiner>()

    override fun applyFilter(
        text: String,
        contentType: ConsoleViewContentType,
    ): List<Pair<String, ConsoleViewContentType>>? {
        val settingsState = JsonLogSettings.getInstance().state
        return filterText(text, contentType, settingsState.formattingEnabled, settingsState.template)
    }

    /**
     * Returns null to show [text] unchanged, an empty list to show nothing (the text is held back until
     * its line is complete), or the parts to show instead.
     */
    internal fun filterText(
        text: String,
        contentType: ConsoleViewContentType,
        formattingEnabled: Boolean,
        template: String,
    ): List<Pair<String, ConsoleViewContentType>>? {
        // What the user types arrives character by character and must never be held back.
        if (contentType == ConsoleViewContentType.USER_INPUT) return null

        val partsToShow = mutableListOf<Pair<String, ConsoleViewContentType>>()
        synchronized(splitLineJoinersByContentType) {
            // System output such as "Process finished with exit code 0" means no more process output follows,
            // so held back fragments are shown now instead of getting lost. Same when formatting was turned off.
            val releaseHeldBackFragments = !formattingEnabled || contentType == ConsoleViewContentType.SYSTEM_OUTPUT
            if (releaseHeldBackFragments) {
                for ((heldBackContentType, splitLineJoiner) in splitLineJoinersByContentType) {
                    val heldBackFragment = splitLineJoiner.takeHeldBackFragment()
                    if (heldBackFragment.isNotEmpty()) {
                        partsToShow += Pair(formatIfEnabled(heldBackFragment, formattingEnabled, template), heldBackContentType)
                    }
                }
                partsToShow += Pair(formatIfEnabled(text, formattingEnabled, template), contentType)
            } else {
                val splitLineJoiner = splitLineJoinersByContentType.getOrPut(contentType) { SplitJsonLineJoiner() }
                val textToShow = splitLineJoiner.textToShowForChunk(text)
                if (textToShow.isNotEmpty()) {
                    partsToShow += Pair(formatIfEnabled(textToShow, formattingEnabled, template), contentType)
                }
            }
        }

        val textIsShownUnchanged = partsToShow.size == 1 && partsToShow[0].first == text
        return if (textIsShownUnchanged) null else partsToShow
    }

    private fun formatIfEnabled(text: String, formattingEnabled: Boolean, template: String): String =
        if (formattingEnabled) JsonLogFormatter.formatTextOrNull(text, template) ?: text else text
}
