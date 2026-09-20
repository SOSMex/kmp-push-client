package io.github.sosmex.push.fcm

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class FcmIosGatewayTest {
    @Test
    fun missingFirebaseAppReturnsUnavailable() {
        val result = FcmIosGateway.create(
            hostApi = HostApi(),
            isDefaultFirebaseAppConfigured = false,
        )

        assertEquals(
            FcmInitializationResult.Unavailable(
                FcmInitializationFailure.DEFAULT_FIREBASE_APP_NOT_CONFIGURED,
            ),
            result,
        )
    }

    @Test
    fun configuredGatewayControlsNativeAutoInit() = runTest {
        val hostApi = HostApi()
        val result = FcmIosGateway.create(
            hostApi = hostApi,
            isDefaultFirebaseAppConfigured = true,
        )
        val gateway = assertIs<FcmInitializationResult.Ready<FcmIosGateway>>(result).value

        gateway.setEnabled(true)
        gateway.setEnabled(false)

        assertEquals(listOf(true, false), hostApi.enablementCalls)
    }
}

private class HostApi : FcmIosHostApi {
    val enablementCalls = mutableListOf<Boolean>()

    override fun setAutoInitEnabled(enabled: Boolean) {
        enablementCalls += enabled
    }
}
