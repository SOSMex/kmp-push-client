package io.github.sosmex.push.sample

import io.github.sosmex.push.core.DestinationState
import io.github.sosmex.push.core.EnablementState
import io.github.sosmex.push.core.EventOfferResult
import io.github.sosmex.push.core.PermissionState
import io.github.sosmex.push.core.ProviderKind
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class DemoPushAppTest {
    @Test
    fun fcmScenarioUsesTheRealFcmClientContract() = runTest {
        assertScenario(ProviderKind.FCM)
    }

    @Test
    fun oneSignalScenarioUsesTheRealOneSignalClientContract() = runTest {
        assertScenario(ProviderKind.ONESIGNAL)
    }

    private suspend fun assertScenario(provider: ProviderKind) {
        val app = DemoPushApp.create(provider)

        assertEquals(PermissionState.Granted, app.initialize())
        assertEquals(EnablementState.ENABLED, app.snapshot.enablement)
        assertEquals(null, app.simulateRegistration())
        assertIs<DestinationState.Available>(app.snapshot.destination)
        assertIs<EventOfferResult.Accepted>(app.simulateForeground())
        assertIs<EventOfferResult.Accepted>(app.simulateColdStartOpen())
        assertEquals(EventOfferResult.Duplicate, app.simulateColdStartOpen())
    }
}
