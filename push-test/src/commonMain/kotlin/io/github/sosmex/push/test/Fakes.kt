package io.github.sosmex.push.test

import io.github.sosmex.push.core.AtomicEventLedger
import io.github.sosmex.push.core.CapabilityResult
import io.github.sosmex.push.core.DestinationState
import io.github.sosmex.push.core.EnablementState
import io.github.sosmex.push.core.EventOfferResult
import io.github.sosmex.push.core.IdentityCapability
import io.github.sosmex.push.core.LedgerClaim
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class InMemoryAtomicEventLedger : AtomicEventLedger {
    private val mutex = Mutex()
    private val claims = mutableSetOf<String>()
    var failWith: String? = null

    override suspend fun claim(scope: String, eventId: String): LedgerClaim = mutex.withLock {
        failWith?.let { return@withLock LedgerClaim.Failed(it) }
        if (claims.add("$scope:$eventId")) LedgerClaim.Claimed else LedgerClaim.AlreadyClaimed
    }

    override suspend fun clear(scope: String) {
        mutex.withLock {
        claims.removeAll { it.startsWith("$scope:") }
        }
    }
}

class FakePermissionGateway(
    initial: PermissionState = PermissionState.NotDetermined,
    var requestResult: PermissionState = initial,
) : PermissionGateway {
    override val state = MutableStateFlow(initial)

    override suspend fun request(): PermissionState {
        state.value = requestResult
        return requestResult
    }
}

class FakeTransportGateway : PushTransportGateway {
    val enablementCalls = mutableListOf<Boolean>()
    override suspend fun setEnabled(enabled: Boolean) {
        enablementCalls += enabled
    }
}

class FakePushClient(
    override val provider: ProviderKind = ProviderKind.FCM,
    val permissionGateway: FakePermissionGateway = FakePermissionGateway(),
    val transportGateway: FakeTransportGateway = FakeTransportGateway(),
    val ledger: InMemoryAtomicEventLedger = InMemoryAtomicEventLedger(),
    initiallyEnabled: Boolean = false,
    identitySupported: Boolean = provider == ProviderKind.ONESIGNAL,
) : PushClient {
    private val engine = PushClientEngine(
        provider,
        permissionGateway,
        transportGateway,
        ledger,
        initiallyEnabled,
    )

    override val capabilities: Set<PushCapability> =
        if (identitySupported) setOf(PushCapability.IDENTITY) else emptySet()
    override val permission: StateFlow<PermissionState> = permissionGateway.state
    override val enablement: StateFlow<EnablementState> = engine.enablement
    override val destination: StateFlow<DestinationState> = engine.destination
    override val events: Flow<PushEvent> = engine.events
    override val identity: IdentityCapability =
        if (identitySupported) FakeIdentity() else UnsupportedIdentityCapability

    var lastExternalId: String? = null
        private set

    override suspend fun requestPermission(): PermissionState = permissionGateway.request()
    override suspend fun setEnabled(enabled: Boolean) = engine.setEnabled(enabled)
    override suspend fun advanceGeneration(): Long = engine.advanceGeneration()
    suspend fun currentGeneration(): Long = engine.currentGeneration()

    suspend fun emitDestination(value: String?): EventOfferResult? =
        emitDestination(value, currentGeneration())

    suspend fun emitDestination(value: String?, generation: Long): EventOfferResult? {
        val destination = value?.let {
            when (provider) {
                ProviderKind.FCM -> PushDestination.FcmToken(it)
                ProviderKind.ONESIGNAL -> PushDestination.OneSignalSubscriptionId(it)
            }
        }
        return engine.observeDestination(generation, destination)
    }

    suspend fun emitForeground(data: Map<String, String>): EventOfferResult =
        emitForeground(data, currentGeneration())

    suspend fun emitForeground(data: Map<String, String>, generation: Long): EventOfferResult =
        engine.offerForeground(data, generation)

    suspend fun emitOpened(data: Map<String, String>, coldStart: Boolean): EventOfferResult =
        emitOpened(data, coldStart, currentGeneration())

    suspend fun emitOpened(
        data: Map<String, String>,
        coldStart: Boolean,
        generation: Long,
    ): EventOfferResult = engine.offerOpened(data, generation, coldStart)

    private inner class FakeIdentity : IdentityCapability {
        override suspend fun login(externalId: String): CapabilityResult<Long> {
            if (externalId.isBlank()) return CapabilityResult.Rejected("invalid external ID")
            lastExternalId = externalId
            return CapabilityResult.Invoked(engine.advanceGeneration())
        }

        override suspend fun logout(): CapabilityResult<Long> {
            lastExternalId = null
            return CapabilityResult.Invoked(engine.advanceGeneration())
        }
    }
}
