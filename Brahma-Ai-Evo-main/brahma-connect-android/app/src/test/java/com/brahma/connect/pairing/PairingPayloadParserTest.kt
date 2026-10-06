package com.brahma.connect.pairing

import com.brahma.connect.core.PairingOffer
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PairingPayloadParserTest {
    @Test
    fun acceptsValidGatewayOffer() {
        assertTrue(PairingPayloadParser.isValid(validOffer()))
    }

    @Test
    fun rejectsUnexpectedService() {
        assertFalse(PairingPayloadParser.isValid(validOffer().copy(service = "_OTHER._tcp.local.")))
    }

    @Test
    fun rejectsInvalidHostPortAndPairingDetails() {
        assertFalse(PairingPayloadParser.isValid(validOffer().copy(host = "")))
        assertFalse(PairingPayloadParser.isValid(validOffer().copy(port = 65536)))
        assertFalse(PairingPayloadParser.isValid(validOffer().copy(pairingToken = "")))
        assertFalse(PairingPayloadParser.isValid(validOffer().copy(pairingCode = "12345")))
        assertFalse(PairingPayloadParser.isValid(validOffer().copy(expiresInSeconds = 0)))
    }

    private fun validOffer() = PairingOffer(
        service = "_BRAHMA._tcp.local.",
        host = "192.168.1.20",
        port = 8765,
        pairingToken = "pairing-token",
        pairingCode = "123456",
        expiresInSeconds = 300,
        createdAt = "2026-10-06T00:00:00+00:00",
    )
}
