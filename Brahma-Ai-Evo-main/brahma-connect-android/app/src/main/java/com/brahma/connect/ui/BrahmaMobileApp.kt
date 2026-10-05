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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import com.brahma.connect.network.DirectGeminiClient
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
    var apiKey by remember { mutableStateOf(storage.loadGeminiApiKey().orEmpty()) }
    var apiKeyDraft by rememberSaveable { mutableStateOf(apiKey) }
    var showSettings by rememberSaveable { mutableStateOf(apiKey.isBlank()) }
    var input by rememberSaveable { mutableStateOf("") }
    var isSending by remember { mutableStateOf(false) }
    val messages = remember {
        mutableStateListOf(MobileChatMessage(
            role = "model",
            text = "I'm Brahma, running directly on this phone. Ask me a question or ask me to check the battery, adjust media volume, open an app, or visit a web address.",
        ))
    }

    if (showSettings) {
        MobileSettingsScreen(
            apiKey = apiKeyDraft,
            hasSavedKey = apiKey.isNotBlank(),
            onApiKeyChange = { apiKeyDraft = it },
            onSave = {
                storage.saveGeminiApiKey(apiKeyDraft)
                apiKey = apiKeyDraft.trim()
                apiKeyDraft = apiKey
                showSettings = false
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
                val history = messages.map { it.role to it.text }
                val answer = DirectGeminiClient.reply(apiKey, history, commandHandler)
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
                        apiKeyDraft = apiKey
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MobileSettingsScreen(
    apiKey: String,
    hasSavedKey: Boolean,
    onApiKeyChange: (String) -> Unit,
    onSave: () -> Unit,
    onBack: () -> Unit,
) {
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
            Text("Brahma, without the desktop", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(
                "Connect directly to Gemini and use supported Android phone actions. Your API key is encrypted on this device; prompts are sent to Google Gemini.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyLarge,
            )
            OutlinedTextField(
                value = apiKey,
                onValueChange = onApiKeyChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Gemini API key") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
            )
            Button(onClick = onSave, enabled = apiKey.isNotBlank(), modifier = Modifier.fillMaxWidth().height(52.dp)) {
                Text("Save and start")
            }
            Text("Phone tools: battery, device info, media volume, opening installed apps, and web links.", color = MaterialTheme.colorScheme.onSurfaceVariant)
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
