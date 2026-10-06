package com.brahma.connect.pairing

import android.content.Context
import android.os.Build
import com.brahma.connect.core.AiProvider
import com.brahma.connect.core.DeviceCredential
import com.brahma.connect.core.PairingOffer
import org.json.JSONObject
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class PairingStorage(context: Context) {
    private val prefs = EncryptedSharedPreferences.create(
        context,
        "brahma_connect_secure",
        MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )

    fun saveCredential(credential: DeviceCredential) {
        prefs.edit()
            .putString("device_credential", JSONObject()
                .put("device_id", credential.deviceId)
                .put("device_secret", credential.deviceSecret)
                .put("device_name", credential.deviceName)
                .put("gateway_host", credential.gatewayHost)
                .put("gateway_port", credential.gatewayPort)
                .put("paired_at", credential.pairedAt)
                .toString())
            .apply()
    }

    fun loadCredential(): DeviceCredential? {
        val raw = prefs.getString("device_credential", null) ?: return null
        return try {
            val json = JSONObject(raw)
            DeviceCredential(
                deviceId = json.optString("device_id"),
                deviceSecret = json.optString("device_secret"),
                deviceName = json.optString("device_name", Build.MODEL),
                gatewayHost = json.optString("gateway_host"),
                gatewayPort = json.optInt("gateway_port", 8765),
                pairedAt = json.optString("paired_at"),
            )
        } catch (_: Exception) {
            null
        }
    }

    fun clearCredential() {
        prefs.edit().remove("device_credential").apply()
    }

    fun saveAiProviderConfig(provider: AiProvider, model: String, apiKey: String) {
        prefs.edit()
            .putString("mobile_ai_provider", provider.name)
            .putString("mobile_ai_model_${provider.name}", model.trim())
            .putString("mobile_ai_key_${provider.name}", apiKey.trim())
            .apply()
    }

    fun loadAiProvider(): AiProvider {
        val savedProvider = prefs.getString("mobile_ai_provider", null)
        return runCatching { AiProvider.valueOf(savedProvider.orEmpty()) }
            .getOrDefault(AiProvider.GEMINI)
    }

    fun loadAiModel(provider: AiProvider): String {
        return prefs.getString("mobile_ai_model_${provider.name}", null)
            ?.takeIf(String::isNotBlank)
            ?: provider.defaultModel
    }

    fun loadAiApiKey(provider: AiProvider): String? {
        val key = prefs.getString("mobile_ai_key_${provider.name}", null)
        if (!key.isNullOrBlank()) return key
        return if (provider == AiProvider.GEMINI) prefs.getString("mobile_gemini_api_key", null) else null
    }

    fun clearAiApiKey(provider: AiProvider) {
        val editor = prefs.edit().remove("mobile_ai_key_${provider.name}")
        if (provider == AiProvider.GEMINI) editor.remove("mobile_gemini_api_key")
        editor.apply()
    }

    fun saveGatewayHint(offer: PairingOffer) {
        prefs.edit()
            .putString("last_pairing_offer", offer.toJson().toString())
            .apply()
    }

    fun loadGatewayHint(): PairingOffer? {
        val raw = prefs.getString("last_pairing_offer", null) ?: return null
        return try {
            PairingOffer.fromJson(JSONObject(raw))
        } catch (_: Exception) {
            null
        }
    }
}
