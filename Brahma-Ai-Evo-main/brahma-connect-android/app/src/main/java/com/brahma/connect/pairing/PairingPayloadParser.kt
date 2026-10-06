package com.brahma.connect.pairing

import com.brahma.connect.core.PairingOffer
import okhttp3.HttpUrl
import org.json.JSONObject

object PairingPayloadParser {
    fun parse(raw: String): PairingOffer? {
        if (raw.isBlank()) return null
        return try {
            PairingOffer.fromJson(JSONObject(raw)).takeIf(::isValid)
        } catch (_: Exception) {
            null
        }
    }

    internal fun isValid(offer: PairingOffer): Boolean {
        if (!offer.service.equals("_BRAHMA._tcp.local.", ignoreCase = true)) return false
        if (offer.pairingToken.isBlank() || !offer.pairingCode.matches(Regex("\\d{6}"))) return false
        if (offer.expiresInSeconds <= 0) return false

        return runCatching {
            HttpUrl.Builder()
                .scheme("http")
                .host(offer.host)
                .port(offer.port)
                .addPathSegment("ws")
                .build()
        }.isSuccess
    }
}
