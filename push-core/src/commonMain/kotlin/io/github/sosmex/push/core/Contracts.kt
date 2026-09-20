package io.github.sosmex.push.core

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

enum class PushCapability {
    IDENTITY,
}

sealed interface CapabilityResult<out T> {
    /** The supported SDK operation was invoked; this is not provider acceptance. */
    data class Invoked<T>(val value: T) : CapabilityResult<T>
    data class Unsupported(val capability: PushCapability) : CapabilityResult<Nothing>
    data class Rejected(val reason: String) : CapabilityResult<Nothing>
}

interface IdentityCapability {
    suspend fun login(externalId: String): CapabilityResult<Long>
    suspend fun logout(): CapabilityResult<Long>
}

object UnsupportedIdentityCapability : IdentityCapability {
    override suspend fun login(externalId: String): CapabilityResult<Long> =
        CapabilityResult.Unsupported(PushCapability.IDENTITY)

    override suspend fun logout(): CapabilityResult<Long> =
        CapabilityResult.Unsupported(PushCapability.IDENTITY)
}

interface PermissionGateway {
    val state: StateFlow<PermissionState>
    suspend fun request(): PermissionState
}

interface PushTransportGateway {
    suspend fun setEnabled(enabled: Boolean)
}

interface PushClient {
    val provider: ProviderKind
    val capabilities: Set<PushCapability>
    val permission: StateFlow<PermissionState>
    val enablement: StateFlow<EnablementState>
    val destination: StateFlow<DestinationState>
    val events: Flow<PushEvent>
    val identity: IdentityCapability

    suspend fun requestPermission(): PermissionState
    suspend fun setEnabled(enabled: Boolean)
    suspend fun advanceGeneration(): Long
}

interface AtomicEventLedger {
    suspend fun claim(scope: String, eventId: String): LedgerClaim
    suspend fun clear(scope: String)
}

sealed interface LedgerClaim {
    data object Claimed : LedgerClaim
    data object AlreadyClaimed : LedgerClaim
    data class Failed(val reason: String) : LedgerClaim
}

sealed interface EventOfferResult {
    data class Accepted(val event: PushEvent) : EventOfferResult
    data object Duplicate : EventOfferResult
    data object StaleGeneration : EventOfferResult
    data class Malformed(val reason: String) : EventOfferResult
    data class Incompatible(val observedVersion: Int) : EventOfferResult
    data class StorageFailure(val reason: String) : EventOfferResult
}
