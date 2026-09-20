package io.github.sosmex.push.fcm

import io.github.sosmex.push.core.AtomicEventLedger
import io.github.sosmex.push.core.CapabilityResult
import io.github.sosmex.push.core.DestinationState
import io.github.sosmex.push.core.LedgerClaim
import io.github.sosmex.push.core.PermissionGateway
import io.github.sosmex.push.core.PermissionState
import io.github.sosmex.push.core.PushCapability
import io.github.sosmex.push.core.PushDestination
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class FcmPushClientTest {
    @Test
    fun identityIsExplicitlyUnsupportedAndTokenIsTyped() = runTest {
        val client = FcmPushClient(Permission(), Gateway(), Ledger(), initiallyEnabled = true)

        assertEquals(
            CapabilityResult.Unsupported(PushCapability.IDENTITY),
            client.identity.login("external-id"),
        )
        client.observeToken("fcm-token", generation = 0)
        val state = assertIs<DestinationState.Available>(client.destination.value)
        assertIs<PushDestination.FcmToken>(state.destination)
    }
}

private class Permission : PermissionGateway {
    override val state = MutableStateFlow<PermissionState>(PermissionState.Granted)
    override suspend fun request(): PermissionState = state.value
}

private class Gateway : FcmGateway {
    override suspend fun setEnabled(enabled: Boolean) = Unit
}

private class Ledger : AtomicEventLedger {
    override suspend fun claim(scope: String, eventId: String) = LedgerClaim.Claimed
    override suspend fun clear(scope: String) = Unit
}
