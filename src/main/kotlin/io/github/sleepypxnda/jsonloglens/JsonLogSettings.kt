package io.github.sleepypxnda.jsonloglens

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.service

/** Application-wide settings, stored in `jsonLogLens.xml`. Edited in [JsonLogSettingsConfigurable]. */
@Service(Service.Level.APP)
@State(name = "JsonLogLensSettings", storages = [Storage("jsonLogLens.xml")])
class JsonLogSettings : PersistentStateComponent<JsonLogSettings.SettingsState> {
    class SettingsState {
        var formattingEnabled: Boolean = true
        var template: String = DEFAULT_TEMPLATE
    }

    private var currentState = SettingsState()

    override fun getState(): SettingsState = currentState

    override fun loadState(loadedState: SettingsState) {
        currentState = loadedState
    }

    companion object {
        const val DEFAULT_TEMPLATE = "{timestamp} {level} {#logger}[{logger}] {/logger}{message}"

        fun getInstance(): JsonLogSettings = service()
    }
}
