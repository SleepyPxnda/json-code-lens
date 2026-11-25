package io.github.sleepypxnda.jsonloglens

import com.intellij.openapi.options.BoundConfigurable
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.DocumentAdapter
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.components.JBTextField
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.COLUMNS_LARGE
import com.intellij.ui.dsl.builder.LabelPosition
import com.intellij.ui.dsl.builder.MAX_LINE_LENGTH_NO_WRAP
import com.intellij.ui.dsl.builder.bindSelected
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.columns
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.builder.rows
import java.awt.Font
import javax.swing.event.DocumentEvent

/** Settings page under Settings | Tools | JSON Log Lens. */
class JsonLogSettingsConfigurable : BoundConfigurable("JSON Log Lens") {
    override fun createPanel(): DialogPanel {
        val settingsState = JsonLogSettings.getInstance().state
        lateinit var templateField: JBTextField
        lateinit var exampleLinesArea: JBTextArea
        lateinit var previewResultArea: JBTextArea

        val settingsPanel = panel {
            row {
                checkBox("Format JSON log lines in the Run/Debug console")
                    .bindSelected(settingsState::formattingEnabled)
            }
            row("Template:") {
                templateField = textField()
                    .bindText(settingsState::template)
                    .columns(COLUMNS_LARGE)
                    .comment("Default: ${JsonLogSettings.DEFAULT_TEMPLATE}")
                    .component
            }
            group("Preview") {
                row {
                    // Only a test input for the preview, not stored in the settings.
                    exampleLinesArea = textArea()
                        .label("Example console output (one log line per line):", LabelPosition.TOP)
                        .rows(PREVIEW_AREA_ROWS)
                        .align(AlignX.FILL)
                        .component
                        .apply {
                            font = monospacedFont(font)
                            text = EXAMPLE_LOG_LINES
                        }
                }
                row {
                    previewResultArea = textArea()
                        .label("Result in the console:", LabelPosition.TOP)
                        .rows(PREVIEW_AREA_ROWS)
                        .align(AlignX.FILL)
                        .component
                        .apply {
                            font = monospacedFont(font)
                            isEditable = false
                        }
                }
            }
            group("How the Template Works") {
                row {
                    text(TEMPLATE_HELP_HTML, maxLineLength = MAX_LINE_LENGTH_NO_WRAP)
                }
            }
        }

        fun updatePreview() {
            // Same as the console: lines that are not a JSON object stay unchanged.
            previewResultArea.text = JsonLogFormatter.formatTextOrNull(exampleLinesArea.text, templateField.text)
                ?: exampleLinesArea.text
        }
        val previewUpdater = object : DocumentAdapter() {
            override fun textChanged(event: DocumentEvent) = updatePreview()
        }
        templateField.document.addDocumentListener(previewUpdater)
        exampleLinesArea.document.addDocumentListener(previewUpdater)
        updatePreview()
        return settingsPanel
    }

    private fun monospacedFont(currentFont: Font): Font = Font(Font.MONOSPACED, Font.PLAIN, currentFont.size)

    private companion object {
        const val PREVIEW_AREA_ROWS = 6
        val TEMPLATE_HELP_HTML = """
            Every console line that is a complete JSON object is replaced by the template.
            All other lines stay as they are.
            <br><br>
            <table cellpadding="3">
              <tr><th align="left">Write</th><th align="left">Meaning</th><th align="left">Example</th></tr>
              <tr>
                <td valign="top"><code>{message}</code></td>
                <td valign="top">Value of the field <code>message</code>.<br>
                    Missing or null → empty. Objects and arrays → JSON text.</td>
                <td valign="top"><code>{"message":"Hi"}</code> → <code>Hi</code></td>
              </tr>
              <tr>
                <td valign="top"><code>{log.level}</code></td>
                <td valign="top">Field inside an object (path separated by dots).<br>
                    A key literally named <code>"log.level"</code> is used first.</td>
                <td valign="top"><code>{"log":{"level":"WARN"}}</code> → <code>WARN</code></td>
              </tr>
              <tr>
                <td valign="top"><code>{#logger}</code>…<code>{/logger}</code></td>
                <td valign="top">Optional part: everything between the two tags is only shown<br>
                    if <code>logger</code> is present and not empty. Can be nested.</td>
                <td valign="top"><code>{#logger}[{logger}] {/logger}</code><br>
                    with logger → <code>[App] </code>, without → nothing</td>
              </tr>
              <tr>
                <td valign="top"><code>\t</code> <code>\n</code> <code>\\</code></td>
                <td valign="top">Tab, line break, backslash.<br>
                    Any other backslash is kept as it is.</td>
                <td valign="top"><code>{level}\t{message}</code> → <code>INFO&nbsp;&nbsp;&nbsp;&nbsp;Started</code></td>
              </tr>
            </table>
            <br>
            Full example: <code>{timestamp} {log.level} {#logger}[{logger}] {/logger}{message}</code><br>
            <code>{"timestamp":"12:00:01","log":{"level":"INFO"},"message":"Started"}</code>
            → <code>12:00:01 INFO Started</code>
        """.trimIndent()
        val EXAMPLE_LOG_LINES = listOf(
            """{"timestamp":"2026-11-25T15:27:22+01:00","level":"INFO","logger":"App","message":"Hello world!"}""",
            """{"timestamp":"2026-11-25T15:27:23+01:00","log":{"level":"WARN"},"message":"Nested level, no logger"}""",
            "Plain line that is not JSON",
        ).joinToString("\n")
    }
}
