package com.brahma.connect.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.brahma.connect.commands.DeviceCommandHandler
import com.brahma.connect.core.AiProvider
import com.brahma.connect.core.AiProviderConfig
import com.brahma.connect.network.MobileAiClient
import com.brahma.connect.pairing.PairingStorage
import kotlinx.coroutines.launch

private data class MobileChatMessage(val role: String, val text: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrahmaMobileApp() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val storage = remember { PairingStorage(context.applicationContext) }
    val commandHandler = remember { DeviceCommandHandler(context.applicationContext) }
    val coroutineScope = rememberCoroutineScope()
    var selectedProviderName by rememberSaveable { mutableStateOf(storage.loadAiProvider().name) }
    val selectedProvider = remember(selectedProviderName) { AiProvider.valueOf(selectedProviderName) }
    var apiKey by remember(selectedProvider) { mutableStateOf(storage.loadAiApiKey(selectedProvider).orEmpty()) }
    var model by remember(selectedProvider) { mutableStateOf(storage.loadAiModel(selectedProvider)) }
    var showSettings by rememberSaveable {
        mutableStateOf(storage.loadAiApiKey(storage.loadAiProvider()).isNullOrBlank())
    }
    var input by rememberSaveable { mutableStateOf("") }
    var isSending by remember { mutableStateOf(false) }
    val messages = remember {
        mutableStateListOf(MobileChatMessage(
            role = "model",
            text = "I'm Brahma, running directly on this phone. I can answer questions, control supported phone settings, open apps and maps, and prepare messages, email, calendar events, or alarms for you to review.",
        ))
    }

    if (showSettings) {
        MobileSettingsScreen(
            provider = selectedProvider,
            apiKey = apiKey,
            model = model,
            hasSavedKey = apiKey.isNotBlank(),
            onProviderChange = { selectedProviderName = it.name },
            onApiKeyChange = { apiKey = it },
            onModelChange = { model = it },
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
                    IconButton(onClick = {
                        showSettings = true
                    }) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF080D14)),
            )
        },
        bottomBar = {
            Surface(color = Color(0xFF080D14), tonalElevation = 3.dp) {
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
                            if (input.isNotBlank() && !isSending) MaterialTheme.colorScheme.secondary else Color(0xFF293541),
                        ),
                    ) {
                        if (isSending) {
                            CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.Send, contentDescription = "Send message", tint = Color(0xFF071113))
                        }
                    }
                }
            }
        },
        containerColor = Color(0xFF080D14),
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
    onProviderChange: (AiProvider) -> Unit,
    onApiKeyChange: (String) -> Unit,
    onModelChange: (String) -> Unit,
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
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF080D14)),
            )
        },
        containerColor = Color(0xFF080D14),
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 24.dp, vertical = 20.dp),
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
                .background(if (isUser) Color(0xFF183A43) else Color(0xFF121C26))
                .padding(horizontal = 16.dp, vertical = 14.dp),
            color = Color(0xFFF1F5F8),
            fontSize = 16.sp,
            lineHeight = 23.sp,
        )
    }
}
