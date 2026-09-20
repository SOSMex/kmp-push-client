package io.github.sosmex.push.fcm

import io.github.sosmex.push.core.AtomicEventLedger
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
import io.github.sosmex.push.core.UnsupportedIdentityCapability
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface FcmGateway : PushTransportGateway

class FcmPushClient(
    permissionGateway: PermissionGateway,
    gateway: FcmGateway,
    ledger: AtomicEventLedger,
    initiallyEnabled: Boolean = false,
) : PushClient {
    private val engine = PushClientEngine(
        provider = ProviderKind.FCM,
        permissionGateway = permissionGateway,
        transportGateway = gateway,
        ledger = ledger,
        initiallyEnabled = initiallyEnabled,
    )

    override val provider: ProviderKind = ProviderKind.FCM
    override val capabilities: Set<PushCapability> = emptySet()
    override val permission: StateFlow<PermissionState> = permissionGateway.state
    override val enablement: StateFlow<EnablementState> = engine.enablement
    override val destination: StateFlow<DestinationState> = engine.destination
    override val events: Flow<PushEvent> = engine.events
    override val identity: IdentityCapability = UnsupportedIdentityCapability

    override suspend fun requestPermission(): PermissionState = engine.permissionGateway.request()
    override suspend fun setEnabled(enabled: Boolean) = engine.setEnabled(enabled)
    override suspend fun advanceGeneration(): Long = engine.advanceGeneration()
    suspend fun currentGeneration(): Long = engine.currentGeneration()

    suspend fun observeToken(token: String?, generation: Long): EventOfferResult? {
        val destination = token?.let {
            runCatching { PushDestination.FcmToken(it) }
                .getOrElse { return EventOfferResult.Malformed("invalid FCM token") }
        }
        return engine.observeDestination(generation, destination)
    }

    suspend fun offerForeground(
        data: Map<String, String>,
        generation: Long,
    ): EventOfferResult = engine.offerForeground(data, generation)

    suspend fun offerOpened(
        data: Map<String, String>,
        generation: Long,
        coldStart: Boolean,
    ): EventOfferResult = engine.offerOpened(data, generation, coldStart)
}
