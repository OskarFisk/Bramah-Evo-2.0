package com.brahma.connect.network

import org.junit.Assert.assertEquals
import org.junit.Test

class MobileAiClientAuthTest {
    @Test
    fun usesBearerAuthorizationForOpenAiCompatibleProviders() {
        assertEquals("Bearer test-key", apiKeyHeaderValue("Authorization", "test-key"))
    }

    @Test
    fun preservesProviderSpecificApiKeyHeaders() {
        assertEquals("test-key", apiKeyHeaderValue("x-goog-api-key", "test-key"))
        assertEquals("test-key", apiKeyHeaderValue("x-api-key", "test-key"))
    }
}
