package io.github.sosmex.push.onesignal

import io.github.sosmex.push.core.AtomicEventLedger
import io.github.sosmex.push.core.CapabilityResult
import io.github.sosmex.push.core.DestinationState
import io.github.sosmex.push.core.EventOfferResult
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

class OneSignalPushClientTest {
    @Test
    fun identityAdvancesFenceAndRequiresConfirmedProviderIdentity() = runTest {
        val gateway = Gateway()
        val client = OneSignalPushClient(Permission(), gateway, Ledger(), initiallyEnabled = true)

        assertEquals(setOf(PushCapability.IDENTITY), client.capabilities)
        val login = assertIs<CapabilityResult.Invoked<Long>>(
            client.identity.login("user-123")
        )
        assertEquals(1L, login.value)
        assertEquals(1, gateway.loginGeneration)

        assertEquals(
            EventOfferResult.StaleGeneration,
            client.observeSubscriptionId("old-sub", generation = 0, identityConfirmed = true),
        )
        client.observeSubscriptionId("current-sub", generation = 1, identityConfirmed = false)
        assertEquals(DestinationState.Unavailable, client.destination.value)
        client.observeSubscriptionId("current-sub", generation = 1, identityConfirmed = true)
        val available = assertIs<DestinationState.Available>(client.destination.value)
        assertIs<PushDestination.OneSignalSubscriptionId>(available.destination)
    }

    @Test
    fun invalidIdentityIsRejectedWithoutCallingProvider() = runTest {
        val gateway = Gateway()
        val client = OneSignalPushClient(Permission(), gateway, Ledger())

        assertIs<CapabilityResult.Rejected>(client.identity.login("  "))
        assertEquals(null, gateway.loginGeneration)
    }
}

private class Permission : PermissionGateway {
    override val state = MutableStateFlow<PermissionState>(PermissionState.Granted)
    override suspend fun request(): PermissionState = state.value
}

private class Gateway : OneSignalGateway {
    var loginGeneration: Long? = null
    override suspend fun setEnabled(enabled: Boolean) = Unit
    override suspend fun login(externalId: String, generation: Long) {
        loginGeneration = generation
    }
    override suspend fun logout(generation: Long) = Unit
}

private class Ledger : AtomicEventLedger {
    override suspend fun claim(scope: String, eventId: String) = LedgerClaim.Claimed
    override suspend fun clear(scope: String) = Unit
}
