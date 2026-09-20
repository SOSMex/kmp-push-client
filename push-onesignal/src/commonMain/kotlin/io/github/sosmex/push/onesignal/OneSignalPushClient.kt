package io.github.sosmex.push.onesignal

import io.github.sosmex.push.core.AtomicEventLedger
import io.github.sosmex.push.core.CapabilityResult
import io.github.sosmex.push.core.DestinationState
import io.github.sosmex.push.core.EnablementState
import io.github.sosmex.push.core.EventOfferResult
import io.github.sosmex.push.core.IdentityCapability
import io.github.sosmex.push.core.PermissionGateway
import io.github.sosmex.push.core.PermissionState
import io.github.sosmex.push.core.ProviderKind
import io.github.sosmex.push.core.PushCapability
import io.github.sosmex.push.core.PushClient
import io.github.sosmex.push.core.PushClientEngine
import io.github.sosmex.push.core.PushDestination
import io.github.sosmex.push.core.PushEvent
import io.github.sosmex.push.core.PushTransportGateway
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface OneSignalGateway : PushTransportGateway {
    suspend fun login(externalId: String, generation: Long)
    suspend fun logout(generation: Long)
}

class OneSignalPushClient(
    permissionGateway: PermissionGateway,
    private val gateway: OneSignalGateway,
    ledger: AtomicEventLedger,
    initiallyEnabled: Boolean = false,
) : PushClient {
    private val engine = PushClientEngine(
        provider = ProviderKind.ONESIGNAL,
        permissionGateway = permissionGateway,
        transportGateway = gateway,
        ledger = ledger,
        initiallyEnabled = initiallyEnabled,
    )

    override val provider: ProviderKind = ProviderKind.ONESIGNAL
    override val capabilities: Set<PushCapability> = setOf(PushCapability.IDENTITY)
    override val permission: StateFlow<PermissionState> = permissionGateway.state
    override val enablement: StateFlow<EnablementState> = engine.enablement
    override val destination: StateFlow<DestinationState> = engine.destination
    override val events: Flow<PushEvent> = engine.events
    override val identity: IdentityCapability = OneSignalIdentityCapability()

    override suspend fun requestPermission(): PermissionState = engine.permissionGateway.request()
    override suspend fun setEnabled(enabled: Boolean) = engine.setEnabled(enabled)
    override suspend fun advanceGeneration(): Long = engine.advanceGeneration()
    suspend fun currentGeneration(): Long = engine.currentGeneration()

    suspend fun observeSubscriptionId(
        subscriptionId: String?,
        generation: Long,
        identityConfirmed: Boolean,
    ): EventOfferResult? {
        if (!identityConfirmed) return engine.observeDestination(generation, null)
        val destination = subscriptionId?.let {
            runCatching { PushDestination.OneSignalSubscriptionId(it) }
                .getOrElse { return EventOfferResult.Malformed("invalid OneSignal subscription ID") }
        }
        return engine.observeDestination(generation, destination)
    }

    suspend fun offerForeground(data: Map<String, String>, generation: Long): EventOfferResult =
        engine.offerForeground(data, generation)

    suspend fun offerOpened(
        data: Map<String, String>,
        generation: Long,
        coldStart: Boolean,
    ): EventOfferResult = engine.offerOpened(data, generation, coldStart)

    private inner class OneSignalIdentityCapability : IdentityCapability {
        override suspend fun login(externalId: String): CapabilityResult<Long> {
            if (!externalId.isValidExternalId()) return CapabilityResult.Rejected("invalid external ID")
            val generation = engine.advanceGeneration()
            gateway.login(externalId, generation)
            return CapabilityResult.Invoked(generation)
        }

        override suspend fun logout(): CapabilityResult<Long> {
            val generation = engine.advanceGeneration()
            gateway.logout(generation)
            return CapabilityResult.Invoked(generation)
        }
    }
}

private fun String.isValidExternalId(): Boolean =
    isNotBlank() && this == trim() && length <= 256
