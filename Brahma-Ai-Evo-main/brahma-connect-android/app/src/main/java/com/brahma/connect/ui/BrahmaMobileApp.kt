package com.brahma.connect.ui

import android.content.Context
import android.content.Intent
import android.speech.tts.TextToSpeech
import android.widget.Toast
import androidx.compose.animation.core.animateFloat
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.drawscope.Stroke
import com.brahma.connect.commands.DeviceCommandHandler
import com.brahma.connect.core.AgentStateStore
import com.brahma.connect.core.AiProvider
import com.brahma.connect.core.AiProviderConfig
import com.brahma.connect.core.MobileSkill
import com.brahma.connect.core.MobileSkillCatalog
import com.brahma.connect.core.StartupEffect
import com.brahma.connect.core.VisualTheme
import com.brahma.connect.core.VoicePreset
import com.brahma.connect.network.MobileAiClient
import com.brahma.connect.pairing.PairingStorage
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import java.util.Locale

private data class MobileChatMessage(val role: String, val text: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrahmaMobileApp() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val storage = remember { PairingStorage(context.applicationContext) }
    val commandHandler = remember { DeviceCommandHandler(context.applicationContext) }
    val coroutineScope = rememberCoroutineScope()
    val visualTheme by AgentStateStore.visualTheme.collectAsState()
    val startupEffect by AgentStateStore.startupEffect.collectAsState()
    val voicePreset by AgentStateStore.voicePreset.collectAsState()
    var bootVisible by rememberSaveable { mutableStateOf(true) }
    var showSkills by rememberSaveable { mutableStateOf(false) }
    val speakReplies by AgentStateStore.speakReplies.collectAsState()
    var selectedProviderName by rememberSaveable { mutableStateOf(storage.loadAiProvider().name) }
    val selectedProvider = remember(selectedProviderName) { AiProvider.valueOf(selectedProviderName) }
    var apiKey by remember(selectedProvider) { mutableStateOf(storage.loadAiApiKey(selectedProvider).orEmpty()) }
    var model by remember(selectedProvider) { mutableStateOf(storage.loadAiModel(selectedProvider)) }
    var showSettings by rememberSaveable {
        mutableStateOf(storage.loadAiApiKey(storage.loadAiProvider()).isNullOrBlank())
    }
    var input by rememberSaveable { mutableStateOf("") }
    var isSending by remember { mutableStateOf(false) }
    var ttsReady by remember { mutableStateOf(false) }
    val textToSpeech = remember(context) {
        TextToSpeech(context) { status ->
            ttsReady = status == TextToSpeech.SUCCESS
        }
    }
    DisposableEffect(textToSpeech) {
        onDispose {
            textToSpeech.stop()
            textToSpeech.shutdown()
        }
    }
    LaunchedEffect(voicePreset) {
        textToSpeech.setLanguage(Locale.getDefault())
        textToSpeech.setPitch(voicePreset.pitch)
        textToSpeech.setSpeechRate(voicePreset.rate)
    }
    LaunchedEffect(Unit) {
        delay(1600)
        bootVisible = false
    }
    val messages = remember {
        mutableStateListOf(MobileChatMessage(
            role = "model",
            text = "I'm Brahma, running directly on this phone. I can answer questions, control supported phone settings, open apps and maps, and prepare messages, email, calendar events, or alarms for you to review.",
        ))
    }

    if (bootVisible) {
        BootSequenceScreen(visualTheme, startupEffect)
        return
    }

    if (showSkills) {
        SkillLibraryScreen(
            onBack = { showSkills = false },
            onSelect = { skill ->
                input = "${skill.title}\n\n${skill.prompt}"
                showSkills = false
            },
        )
        return
    }

    if (showSettings) {
        MobileSettingsScreen(
            provider = selectedProvider,
            apiKey = apiKey,
            model = model,
            hasSavedKey = apiKey.isNotBlank(),
            visualTheme = visualTheme,
            startupEffect = startupEffect,
            voicePreset = voicePreset,
            speakReplies = speakReplies,
            onProviderChange = { selectedProviderName = it.name },
            onApiKeyChange = { apiKey = it },
            onModelChange = { model = it },
            onVisualThemeChange = { theme ->
                storage.saveExperiencePreferences(theme, startupEffect, voicePreset)
                AgentStateStore.setExperiencePreferences(theme, startupEffect, voicePreset)
            },
            onStartupEffectChange = { effect ->
                storage.saveExperiencePreferences(visualTheme, effect, voicePreset)
                AgentStateStore.setExperiencePreferences(visualTheme, effect, voicePreset)
            },
            onVoicePresetChange = { voice ->
                storage.saveExperiencePreferences(visualTheme, startupEffect, voice)
                AgentStateStore.setExperiencePreferences(visualTheme, startupEffect, voice)
            },
            onSpeakRepliesChange = {
                storage.saveSpeakReplies(it)
                AgentStateStore.setExperiencePreferences(visualTheme, startupEffect, voicePreset, it)
            },
            onTestVoice = {
                if (ttsReady) {
                    textToSpeech.speak(
                        "Brahma online. Your voice preset is ready.",
                        TextToSpeech.QUEUE_FLUSH,
                        null,
                        "brahma-voice-preview",
                    )
                } else {
                    Toast.makeText(context, "Text-to-Speech is not ready yet.", Toast.LENGTH_SHORT).show()
                }
            },
            onInstallVoices = { openTtsVoiceInstaller(context) },
            onSave = {
                storage.saveAiProviderConfig(selectedProvider, model, apiKey)
                apiKey = apiKey.trim()
                model = model.trim()
                showSettings = false
            },
            onRemoveKey = {
                storage.clearAiApiKey(selectedProvider)
                apiKey = ""
            },
            onBack = { showSettings = false },
        )
        return
    }

    fun sendMessage() {
        val text = input.trim()
        if (text.isBlank() || isSending) return
        input = ""
        messages.add(MobileChatMessage("user", text))
        isSending = true
        coroutineScope.launch {
            try {
                val history = messages.drop(1).takeLast(MAX_CONTEXT_MESSAGES).map { it.role to it.text }
                val answer = MobileAiClient.reply(
                    AiProviderConfig(selectedProvider, model, apiKey),
                    history,
                    commandHandler,
                )
                messages.add(MobileChatMessage("model", answer))
                if (speakReplies && ttsReady) {
                    textToSpeech.speak(answer, TextToSpeech.QUEUE_FLUSH, null, "brahma-reply")
                }
            } catch (error: Exception) {
                messages.add(MobileChatMessage("model", error.message ?: "Brahma couldn't complete that request."))
            } finally {
                isSending = false
            }
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets.navigationBars,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("BRAHMA MOBILE", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        Text("DIRECT AI · ON-DEVICE TOOLS", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                    }
                },
                actions = {
                    TextButton(onClick = { showSkills = true }) {
                        Text("200 Skills")
                    }
                    IconButton(onClick = {
                        showSettings = true
                    }) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
            )
        },
        bottomBar = {
            Surface(color = MaterialTheme.colorScheme.surface, tonalElevation = 3.dp) {
                Row(
                    modifier = Modifier.fillMaxWidth().imePadding().padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.Bottom,
                ) {
                    OutlinedTextField(
                        value = input,
                        onValueChange = { input = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Message Brahma") },
                        shape = RoundedCornerShape(24.dp),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(onSend = { sendMessage() }),
                        maxLines = 4,
                    )
                    Spacer(Modifier.width(8.dp))
                    IconButton(
                        onClick = { sendMessage() },
                        enabled = input.isNotBlank() && !isSending,
                        modifier = Modifier.size(52.dp).clip(CircleShape).background(
                            if (input.isNotBlank() && !isSending) MaterialTheme.colorScheme.secondary
                            else MaterialTheme.colorScheme.surfaceVariant,
                        ),
                    ) {
                        if (isSending) {
                            CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(
                                Icons.Default.Send,
                                contentDescription = "Send message",
                                tint = if (input.isNotBlank() && !isSending) MaterialTheme.colorScheme.onPrimary
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                Text("ON THIS PHONE", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)
            }
            itemsIndexed(messages) { _, message ->
                MobileMessageBubble(message)
            }
        }
    }
}

private const val MAX_CONTEXT_MESSAGES = 20

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MobileSettingsScreen(
    provider: AiProvider,
    apiKey: String,
    model: String,
    hasSavedKey: Boolean,
    visualTheme: VisualTheme,
    startupEffect: StartupEffect,
    voicePreset: VoicePreset,
    speakReplies: Boolean,
    onProviderChange: (AiProvider) -> Unit,
    onApiKeyChange: (String) -> Unit,
    onModelChange: (String) -> Unit,
    onVisualThemeChange: (VisualTheme) -> Unit,
    onStartupEffectChange: (StartupEffect) -> Unit,
    onVoicePresetChange: (VoicePreset) -> Unit,
    onSpeakRepliesChange: (Boolean) -> Unit,
    onTestVoice: () -> Unit,
    onInstallVoices: () -> Unit,
    onSave: () -> Unit,
    onRemoveKey: () -> Unit,
    onBack: () -> Unit,
) {
    var providerMenuExpanded by remember { mutableStateOf(false) }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mobile setup") },
                navigationIcon = {
                    if (hasSavedKey) TextButton(onClick = onBack) { Text("Back") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 24.dp, vertical = 20.dp)
                .verticalScroll(androidx.compose.foundation.rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Text("Choose an AI provider", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(
                "Provider API keys are encrypted on this device. Prompts are sent directly to the provider you select.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyLarge,
            )
            ExposedDropdownMenuBox(
                expanded = providerMenuExpanded,
                onExpandedChange = { providerMenuExpanded = it },
            ) {
                OutlinedTextField(
                    value = provider.title,
                    onValueChange = {},
                    readOnly = true,
                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                    label = { Text("Provider") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = providerMenuExpanded) },
                )
                ExposedDropdownMenu(
                    expanded = providerMenuExpanded,
                    onDismissRequest = { providerMenuExpanded = false },
                ) {
                    AiProvider.entries.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option.title) },
                            onClick = {
                                onProviderChange(option)
                                providerMenuExpanded = false
                            },
                        )
                    }
                }
            }
            OutlinedTextField(
                value = model,
                onValueChange = onModelChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Model ID") },
                supportingText = { Text("Default: ${provider.defaultModel}. You can enter another model supported by this provider.") },
                singleLine = true,
            )
            OutlinedTextField(
                value = apiKey,
                onValueChange = onApiKeyChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(provider.keyLabel) },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
            )
            Button(
                onClick = onSave,
                enabled = apiKey.isNotBlank() && model.isNotBlank(),
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) {
                Text("Save and start")
            }
            if (hasSavedKey) {
                TextButton(onClick = onRemoveKey) { Text("Remove this provider's saved key") }
            }
            Text(
                "Providers: ${AiProvider.entries.joinToString { it.title }}. " +
                    "Phone skills include device info, battery, flashlight, volume, apps, maps, sharing, drafts, calendar, alarms, and settings.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            ExperienceSettingsPanel(
                visualTheme = visualTheme,
                startupEffect = startupEffect,
                voicePreset = voicePreset,
                speakReplies = speakReplies,
                onVisualThemeChange = onVisualThemeChange,
                onStartupEffectChange = onStartupEffectChange,
                onVoicePresetChange = onVoicePresetChange,
                onSpeakRepliesChange = onSpeakRepliesChange,
                onTestVoice = onTestVoice,
                onInstallVoices = onInstallVoices,
            )
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun ExperienceSettingsPanel(
    visualTheme: VisualTheme,
    startupEffect: StartupEffect,
    voicePreset: VoicePreset,
    speakReplies: Boolean,
    onVisualThemeChange: (VisualTheme) -> Unit,
    onStartupEffectChange: (StartupEffect) -> Unit,
    onVoicePresetChange: (VoicePreset) -> Unit,
    onSpeakRepliesChange: (Boolean) -> Unit,
    onTestVoice: () -> Unit,
    onInstallVoices: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("VISUAL THEMES", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text("Choose an accent palette for the app.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(VisualTheme.entries, key = VisualTheme::name) { option ->
                FilterChip(
                    selected = visualTheme == option,
                    onClick = { onVisualThemeChange(option) },
                    label = { Text(option.title) },
                )
            }
        }
        Text("BOOT ANIMATION", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(StartupEffect.entries, key = StartupEffect::name) { option ->
                FilterChip(
                    selected = startupEffect == option,
                    onClick = { onStartupEffectChange(option) },
                    label = { Text(option.title) },
                )
            }
        }
        Text("VOICE PRESET", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(
            "Presets use the Android speech engine. Install additional high-quality voices from your device's Text-to-Speech settings.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(VoicePreset.entries, key = VoicePreset::name) { option ->
                FilterChip(
                    selected = voicePreset == option,
                    onClick = { onVoicePresetChange(option) },
                    label = { Text(option.title) },
                )
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Speak assistant replies", modifier = Modifier.weight(1f))
            Switch(checked = speakReplies, onCheckedChange = onSpeakRepliesChange)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onTestVoice) { Text("Preview voice") }
            OutlinedButton(onClick = onInstallVoices) { Text("Get voices") }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun AppearancePreferencesScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val storage = remember { PairingStorage(context.applicationContext) }
    val visualTheme by AgentStateStore.visualTheme.collectAsState()
    val startupEffect by AgentStateStore.startupEffect.collectAsState()
    val voicePreset by AgentStateStore.voicePreset.collectAsState()
    val speakReplies by AgentStateStore.speakReplies.collectAsState()
    var ttsReady by remember { mutableStateOf(false) }
    val speech = remember(context) {
        TextToSpeech(context) { status -> ttsReady = status == TextToSpeech.SUCCESS }
    }
    DisposableEffect(speech) {
        onDispose {
            speech.stop()
            speech.shutdown()
        }
    }
    LaunchedEffect(voicePreset) {
        speech.setLanguage(Locale.getDefault())
        speech.setPitch(voicePreset.pitch)
        speech.setSpeechRate(voicePreset.rate)
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Personalize Brahma") },
                navigationIcon = { TextButton(onClick = onBack) { Text("Back") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(20.dp)
                .verticalScroll(androidx.compose.foundation.rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            ExperienceSettingsPanel(
                visualTheme = visualTheme,
                startupEffect = startupEffect,
                voicePreset = voicePreset,
                speakReplies = speakReplies,
                onVisualThemeChange = { theme ->
                    storage.saveExperiencePreferences(theme, startupEffect, voicePreset)
                    AgentStateStore.setExperiencePreferences(theme, startupEffect, voicePreset)
                },
                onStartupEffectChange = { effect ->
                    storage.saveExperiencePreferences(visualTheme, effect, voicePreset)
                    AgentStateStore.setExperiencePreferences(visualTheme, effect, voicePreset)
                },
                onVoicePresetChange = { voice ->
                    storage.saveExperiencePreferences(visualTheme, startupEffect, voice)
                    AgentStateStore.setExperiencePreferences(visualTheme, startupEffect, voice)
                },
                onSpeakRepliesChange = { enabled ->
                    storage.saveSpeakReplies(enabled)
                    AgentStateStore.setExperiencePreferences(visualTheme, startupEffect, voicePreset, enabled)
                },
                onTestVoice = {
                    if (ttsReady) {
                        speech.speak(
                            "Brahma online. Your selected voice is ready.",
                            TextToSpeech.QUEUE_FLUSH,
                            null,
                            "brahma-voice-preview",
                        )
                    } else {
                        Toast.makeText(context, "Text-to-Speech is not ready yet.", Toast.LENGTH_SHORT).show()
                    }
                },
                onInstallVoices = { openTtsVoiceInstaller(context) },
            )
        }
    }
}

private fun openTtsVoiceInstaller(context: Context) {
    val intent = Intent(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA)
    if (intent.resolveActivity(context.packageManager) == null) {
        Toast.makeText(context, "No Text-to-Speech voice installer is available.", Toast.LENGTH_LONG).show()
    } else {
        context.startActivity(intent)
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun SkillLibraryScreen(
    onBack: () -> Unit,
    onSelect: (MobileSkill) -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    var selectedCategory by rememberSaveable { mutableStateOf("All") }
    val categories = remember { listOf("All") + MobileSkillCatalog.categories() }
    val skills = remember(query, selectedCategory) {
        MobileSkillCatalog.all.filter { skill ->
            (selectedCategory == "All" || skill.category == selectedCategory) &&
                (query.isBlank() || skill.title.contains(query, ignoreCase = true) || skill.prompt.contains(query, ignoreCase = true))
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Skill library · ${MobileSkillCatalog.all.size}") },
                navigationIcon = { TextButton(onClick = onBack) { Text("Back") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Search skills") },
                singleLine = true,
            )
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(categories, key = { it }) { category ->
                    FilterChip(
                        selected = selectedCategory == category,
                        onClick = { selectedCategory = category },
                        label = { Text(category) },
                    )
                }
            }
            Text("${skills.size} skills · Select one to prepare a prompt", color = MaterialTheme.colorScheme.onSurfaceVariant)
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(skills, key = MobileSkill::id) { skill ->
                    Card(
                        onClick = { onSelect(skill) },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(Modifier.padding(14.dp)) {
                            Text(skill.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Text(skill.category, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                            Text(
                                skill.prompt,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BootSequenceScreen(theme: VisualTheme, effect: StartupEffect) {
    val motion = androidx.compose.animation.core.rememberInfiniteTransition(label = "boot-motion")
    val rotation by motion.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
            animation = androidx.compose.animation.core.tween(5200, easing = androidx.compose.animation.core.LinearEasing),
        ),
        label = "boot-rotation",
    )
    val pulse by motion.animateFloat(
        initialValue = 0.82f,
        targetValue = 1.08f,
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
            animation = androidx.compose.animation.core.tween(1800),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse,
        ),
        label = "boot-pulse",
    )
    val accent = Color(theme.accent)
    Box(
        modifier = Modifier.fillMaxSize().background(Color(theme.background)),
        contentAlignment = Alignment.Center,
    ) {
        androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) {
            val center = androidx.compose.ui.geometry.Offset(size.width / 2f, size.height / 2f)
            val radius = size.minDimension * 0.23f
            drawCircle(accent.copy(alpha = 0.07f), radius = radius * 1.65f * pulse, center = center)
            drawCircle(accent.copy(alpha = 0.12f), radius = radius * 1.25f * pulse, center = center)
            when (effect) {
                StartupEffect.ORBITAL_IGNITION -> {
                    for (index in 0..2) {
                        rotate(rotation * (if (index == 1) -1f else 1f) + index * 60f, center) {
                            drawCircle(
                                accent.copy(alpha = 0.72f - index * 0.12f),
                                radius = radius * (0.72f + index * 0.2f),
                                center = center,
                                style = Stroke(width = 2.dp.toPx()),
                            )
                            drawCircle(accent, radius = 4.dp.toPx(), center = center + androidx.compose.ui.geometry.Offset(radius * (0.72f + index * 0.2f), 0f))
                        }
                    }
                }
                StartupEffect.SINGULARITY -> {
                    for (index in 0..7) {
                        rotate(rotation + index * 22.5f, center) {
                            drawCircle(accent.copy(alpha = 0.45f), radius = radius * (0.35f + index * 0.09f), center = center, style = Stroke(width = 1.5.dp.toPx()))
                        }
                    }
                }
                StartupEffect.NEURAL_PULSE -> {
                    for (index in 0..10) {
                        val x = size.width * (index + 1) / 12f
                        drawLine(
                            accent.copy(alpha = 0.28f + 0.4f * kotlin.math.abs(kotlin.math.sin(rotation / 35f + index).toFloat())),
                            start = androidx.compose.ui.geometry.Offset(x, center.y - radius * pulse),
                            end = androidx.compose.ui.geometry.Offset(x, center.y + radius * pulse),
                            strokeWidth = (1.2f + (index % 3)) * density,
                        )
                    }
                    drawCircle(accent, radius = 7.dp.toPx() * pulse, center = center)
                }
                StartupEffect.QUANTUM_GATE -> {
                    rotate(rotation / 3f, center) {
                        for (index in 0 until 16) {
                            rotate(index * 22.5f, center) {
                                drawLine(
                                    accent.copy(alpha = 0.55f),
                                    start = center,
                                    end = center + androidx.compose.ui.geometry.Offset(0f, -radius * pulse),
                                    strokeWidth = 1.5.dp.toPx(),
                                )
                            }
                        }
                    }
                    drawCircle(accent, radius = 8.dp.toPx(), center = center)
                }
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("BRAHMA", style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Black, color = accent)
            Text(effect.title.uppercase(), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun MobileMessageBubble(message: MobileChatMessage) {
    val isUser = message.role == "user"
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
    ) {
        Text(
            text = message.text,
            modifier = Modifier
                .fillMaxWidth(if (isUser) 0.88f else 1f)
                .clip(RoundedCornerShape(18.dp))
                .background(
                    if (isUser) MaterialTheme.colorScheme.secondary.copy(alpha = 0.28f)
                    else MaterialTheme.colorScheme.surfaceVariant,
                )
                .padding(horizontal = 16.dp, vertical = 14.dp),
            color = Color(0xFFF1F5F8),
            fontSize = 16.sp,
            lineHeight = 23.sp,
        )
    }
}
