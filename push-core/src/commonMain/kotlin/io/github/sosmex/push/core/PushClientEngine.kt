package io.github.sosmex.push.core

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class PushClientEngine(
    val provider: ProviderKind,
    val permissionGateway: PermissionGateway,
    private val transportGateway: PushTransportGateway,
    private val ledger: AtomicEventLedger,
    initiallyEnabled: Boolean = false,
) {
    private val mutex = Mutex()
    private val mutableEnablement = MutableStateFlow(
        if (initiallyEnabled) EnablementState.ENABLED else EnablementState.DISABLED
    )
    private val mutableDestination = MutableStateFlow<DestinationState>(
        if (initiallyEnabled) DestinationState.Resolving else DestinationState.Disabled
    )
    private val mutableEvents = MutableSharedFlow<PushEvent>(extraBufferCapacity = 16)
    private var generation = 0L

    val enablement: StateFlow<EnablementState> = mutableEnablement.asStateFlow()
    val destination: StateFlow<DestinationState> = mutableDestination.asStateFlow()
    val events: Flow<PushEvent> = mutableEvents.asSharedFlow()

    suspend fun setEnabled(enabled: Boolean) {
        transportGateway.setEnabled(enabled)
        mutex.withLock {
            mutableEnablement.value =
                if (enabled) EnablementState.ENABLED else EnablementState.DISABLED
            mutableDestination.value =
                if (enabled) DestinationState.Resolving else DestinationState.Disabled
        }
    }

    suspend fun advanceGeneration(): Long = mutex.withLock {
        generation += 1
        mutableDestination.value = when (mutableEnablement.value) {
            EnablementState.ENABLED -> DestinationState.Resolving
            EnablementState.DISABLED -> DestinationState.Disabled
        }
        generation
    }

    suspend fun currentGeneration(): Long = mutex.withLock { generation }

    suspend fun observeDestination(
        observedGeneration: Long,
        destination: PushDestination?,
    ): EventOfferResult? = mutex.withLock {
        if (observedGeneration != generation) return@withLock EventOfferResult.StaleGeneration
        if (mutableEnablement.value == EnablementState.DISABLED) {
            mutableDestination.value = DestinationState.Disabled
            return@withLock null
        }
        mutableDestination.value = destination?.let(DestinationState::Available)
            ?: DestinationState.Unavailable
        null
    }

    suspend fun offerForeground(
        values: Map<String, String>,
        observedGeneration: Long,
    ): EventOfferResult = when (val decoded = SemanticPayload.decode(values)) {
        is PayloadDecodeResult.Malformed -> EventOfferResult.Malformed(decoded.reason)
        is PayloadDecodeResult.Incompatible -> EventOfferResult.Incompatible(decoded.observedVersion)
        is PayloadDecodeResult.Decoded -> mutex.withLock {
            if (observedGeneration != generation) return@withLock EventOfferResult.StaleGeneration
            val event = PushEvent.ForegroundReceived(decoded.payload, observedGeneration)
            mutableEvents.emit(event)
            EventOfferResult.Accepted(event)
        }
    }

    suspend fun offerOpened(
        values: Map<String, String>,
        observedGeneration: Long,
        coldStart: Boolean,
    ): EventOfferResult = when (val decoded = SemanticPayload.decode(values)) {
        is PayloadDecodeResult.Malformed -> EventOfferResult.Malformed(decoded.reason)
        is PayloadDecodeResult.Incompatible -> EventOfferResult.Incompatible(decoded.observedVersion)
        is PayloadDecodeResult.Decoded -> {
            val scope = mutex.withLock {
                if (observedGeneration != generation) return EventOfferResult.StaleGeneration
                "${provider.name}:$observedGeneration"
            }
            when (val claim = ledger.claim(scope, decoded.payload.eventId)) {
                LedgerClaim.Claimed -> {
                    val event = PushEvent.Opened(decoded.payload, observedGeneration, coldStart)
                    mutex.withLock {
                        if (observedGeneration != generation) return EventOfferResult.StaleGeneration
                        mutableEvents.emit(event)
                    }
                    EventOfferResult.Accepted(event)
                }
                LedgerClaim.AlreadyClaimed -> EventOfferResult.Duplicate
                is LedgerClaim.Failed -> EventOfferResult.StorageFailure(claim.reason)
            }
        }
    }
}
