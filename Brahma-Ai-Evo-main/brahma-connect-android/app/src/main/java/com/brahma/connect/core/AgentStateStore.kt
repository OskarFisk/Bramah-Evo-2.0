package com.brahma.connect.core

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object AgentStateStore {
    private val _visualTheme = MutableStateFlow(VisualTheme.CYBER_CYAN)
    val visualTheme: StateFlow<VisualTheme> = _visualTheme.asStateFlow()

    private val _startupEffect = MutableStateFlow(StartupEffect.ORBITAL_IGNITION)
    val startupEffect: StateFlow<StartupEffect> = _startupEffect.asStateFlow()

    private val _voicePreset = MutableStateFlow(VoicePreset.STUDIO)
    val voicePreset: StateFlow<VoicePreset> = _voicePreset.asStateFlow()

    private val _speakReplies = MutableStateFlow(false)
    val speakReplies: StateFlow<Boolean> = _speakReplies.asStateFlow()

    fun setExperiencePreferences(
        theme: VisualTheme,
        effect: StartupEffect,
        voice: VoicePreset,
        speakReplies: Boolean = _speakReplies.value,
    ) {
        _visualTheme.value = theme
        _startupEffect.value = effect
        _voicePreset.value = voice
        _speakReplies.value = speakReplies
    }

    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _gateway = MutableStateFlow<GatewayEndpoint?>(null)
    val gateway: StateFlow<GatewayEndpoint?> = _gateway.asStateFlow()

    private val _pairingOffer = MutableStateFlow<PairingOffer?>(null)
    val pairingOffer: StateFlow<PairingOffer?> = _pairingOffer.asStateFlow()

    private val _credential = MutableStateFlow<DeviceCredential?>(null)
    val credential: StateFlow<DeviceCredential?> = _credential.asStateFlow()

    private val _statusText = MutableStateFlow("Ready")
    val statusText: StateFlow<String> = _statusText.asStateFlow()

    private val _lastError = MutableStateFlow<String?>(null)
    val lastError: StateFlow<String?> = _lastError.asStateFlow()

    private val _logs = MutableStateFlow<List<String>>(emptyList())
    val logs: StateFlow<List<String>> = _logs.asStateFlow()

    private val _chatHistory = MutableStateFlow<List<ChatMessage>>(emptyList())
    val chatHistory: StateFlow<List<ChatMessage>> = _chatHistory.asStateFlow()

    fun setConnectionState(state: ConnectionState) {
        _connectionState.value = state
    }

    fun setGateway(endpoint: GatewayEndpoint?) {
        _gateway.value = endpoint
    }

    fun setPairingOffer(offer: PairingOffer?) {
        _pairingOffer.value = offer
    }

    fun setCredential(credential: DeviceCredential?) {
        _credential.value = credential
    }

    fun setStatus(text: String) {
        _statusText.value = text
    }

    fun setError(text: String?) {
        _lastError.value = text
    }

    fun addLog(message: String) {
        _logs.value = (_logs.value + message).takeLast(50)
    }

    fun addChatMessage(msg: ChatMessage) {
        val current = _chatHistory.value.toMutableList()
        current.removeAll { it.id == msg.id }
        current.add(msg)
        _chatHistory.value = current
    }

    fun setChatHistory(messages: List<ChatMessage>) {
        _chatHistory.value = messages
    }
}

object BrahmaConnectCapabilities {
    val INITIAL = listOf(
        "device_info",
        "battery",
        "flashlight",
        "volume",
        "media",
        "launch_app",
        "apps",
        "open_url",
        "wifi_state",
        "sharing",
        "messaging",
        "email",
        "maps",
        "calendar",
        "alarms",
        "settings",
        "list_apps",
        "share_text",
        "compose_sms",
        "compose_email",
        "open_map",
        "create_calendar_event",
        "set_alarm",
        "open_settings",
        "file_list",
        "file_read",
        "file_write",
        "file_delete",
        "ui_dump",
        "ui_tap",
        "ui_swipe",
        "ui_type",
    )
}
