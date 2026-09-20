package io.github.sosmex.push.fcm

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs

class FcmInitializationTest {
    @Test
    fun missingDefaultFirebaseAppFailsClosedWithoutInitializingSdk() {
        var initializerInvoked = false

        val result =
            initializeFcm(
                isDefaultFirebaseAppConfigured = { false },
                initializer = {
                    initializerInvoked = true
                    "gateway"
                },
            )

        assertEquals(
            FcmInitializationResult.Unavailable(
                FcmInitializationFailure.DEFAULT_FIREBASE_APP_NOT_CONFIGURED,
            ),
            result,
        )
        assertFalse(initializerInvoked)
    }

    @Test
    fun configuredDefaultFirebaseAppReturnsReadyBinding() {
        val result =
            initializeFcm(
                isDefaultFirebaseAppConfigured = { true },
                initializer = { "gateway" },
            )

        assertEquals("gateway", assertIs<FcmInitializationResult.Ready<String>>(result).value)
    }

    @Test
    fun unexpectedSdkFailureIsReportedWithoutLeakingExceptionDetails() {
        val result =
            initializeFcm(
                isDefaultFirebaseAppConfigured = { true },
                initializer = { error("configuration value that must not escape") },
            )

        assertEquals(
            FcmInitializationResult.Unavailable(FcmInitializationFailure.SDK_INITIALIZATION_FAILED),
            result,
        )
        assertFalse(result.toString().contains("configuration value"))
    }
}
