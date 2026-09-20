package io.github.sosmex.push.sample

import io.github.sosmex.push.core.CapabilityResult
import io.github.sosmex.push.core.DestinationState
import io.github.sosmex.push.core.EnablementState
import io.github.sosmex.push.core.EventOfferResult
import io.github.sosmex.push.core.PermissionState
import io.github.sosmex.push.core.ProviderKind
import io.github.sosmex.push.core.PushClient
import io.github.sosmex.push.core.PushEvent
import io.github.sosmex.push.fcm.FcmClientFactory
import io.github.sosmex.push.fcm.FcmGateway
import io.github.sosmex.push.fcm.FcmInitializationResult
import io.github.sosmex.push.fcm.FcmPushClient
import io.github.sosmex.push.onesignal.OneSignalGateway
import io.github.sosmex.push.onesignal.OneSignalPushClient
import io.github.sosmex.push.test.FakePermissionGateway
import io.github.sosmex.push.test.InMemoryAtomicEventLedger
import kotlinx.coroutines.flow.Flow

/**
 * A credential-free reference host for the SDK.
 *
 * It uses the real provider clients and simulates only the native SDK callbacks that would
 * normally come from Firebase Messaging or OneSignal.
 */
class DemoPushApp private constructor(
    private val session: DemoProviderSession,
) {
    val provider: ProviderKind get() = session.client.provider
    val events: Flow<PushEvent> get() = session.client.events

    val snapshot: DemoSnapshot
        get() = DemoSnapshot(
            provider = provider,
            permission = session.client.permission.value,
            enablement = session.client.enablement.value,
            destination = session.client.destination.value,
        )

    suspend fun initialize(): PermissionState {
        val permission = session.client.requestPermission()
        if (permission == PermissionState.Granted) {
            session.client.setEnabled(true)
            session.prepare()
        }
        return permission
    }

    suspend fun simulateRegistration(): EventOfferResult? = session.observeDestination()

    suspend fun simulateForeground(): EventOfferResult =
        session.offerForeground(
            semanticPayload(
                eventId = "sample:foreground:1",
                type = "sample.foreground",
            )
        )

    suspend fun simulateColdStartOpen(): EventOfferResult =
        session.offerOpened(
            data = semanticPayload(
                eventId = "sample:open:1",
                type = "sample.opened",
            ),
            coldStart = true,
        )

    companion object {
        fun create(provider: ProviderKind): DemoPushApp =
            when (provider) {
                ProviderKind.FCM -> DemoPushApp(FcmDemoSession())
                ProviderKind.ONESIGNAL -> DemoPushApp(OneSignalDemoSession())
            }
    }
}

data class DemoSnapshot(
    val provider: ProviderKind,
    val permission: PermissionState,
    val enablement: EnablementState,
    val destination: DestinationState,
)

private interface DemoProviderSession {
    val client: PushClient

    suspend fun prepare()
    suspend fun observeDestination(): EventOfferResult?
    suspend fun offerForeground(data: Map<String, String>): EventOfferResult
    suspend fun offerOpened(data: Map<String, String>, coldStart: Boolean): EventOfferResult
}

private class FcmDemoSession : DemoProviderSession {
    private val typedClient = when (
        val result = FcmClientFactory.create(
            permissionGateway = grantedPermissionGateway(),
            gateway = FcmInitializationResult.Ready(DemoFcmGateway),
            ledger = InMemoryAtomicEventLedger(),
        )
    ) {
        is FcmInitializationResult.Ready -> result.value
        is FcmInitializationResult.Unavailable -> error("Demo FCM gateway unavailable: ${result.reason}")
    }
    private var callbackGeneration = 0L

    override val client: FcmPushClient = typedClient

    override suspend fun prepare() {
        // Capture once when native observers are registered. Do not look it up per callback.
        callbackGeneration = typedClient.currentGeneration()
    }

    override suspend fun observeDestination(): EventOfferResult? =
        typedClient.observeToken(
            token = "demo-fcm-token",
            generation = callbackGeneration,
        )

    override suspend fun offerForeground(data: Map<String, String>): EventOfferResult =
        typedClient.offerForeground(data, callbackGeneration)

    override suspend fun offerOpened(
        data: Map<String, String>,
        coldStart: Boolean,
    ): EventOfferResult = typedClient.offerOpened(data, callbackGeneration, coldStart)
}

private class OneSignalDemoSession : DemoProviderSession {
    private val gateway = DemoOneSignalGateway()
    private val typedClient = OneSignalPushClient(
        permissionGateway = grantedPermissionGateway(),
        gateway = gateway,
        ledger = InMemoryAtomicEventLedger(),
    )
    private var callbackGeneration = 0L

    override val client: OneSignalPushClient = typedClient

    override suspend fun prepare() {
        callbackGeneration = when (val result = typedClient.identity.login("sample-user")) {
            is CapabilityResult.Invoked -> result.value
            is CapabilityResult.Rejected -> error("Demo identity rejected: ${result.reason}")
            is CapabilityResult.Unsupported -> error("OneSignal identity unexpectedly unsupported")
        }
    }

    override suspend fun observeDestination(): EventOfferResult? =
        typedClient.observeSubscriptionId(
            subscriptionId = "demo-onesignal-subscription",
            generation = callbackGeneration,
            identityConfirmed = true,
        )

    override suspend fun offerForeground(data: Map<String, String>): EventOfferResult =
        typedClient.offerForeground(data, callbackGeneration)

    override suspend fun offerOpened(
        data: Map<String, String>,
        coldStart: Boolean,
    ): EventOfferResult = typedClient.offerOpened(data, callbackGeneration, coldStart)
}

private object DemoFcmGateway : FcmGateway {
    override suspend fun setEnabled(enabled: Boolean) = Unit
}

private class DemoOneSignalGateway : OneSignalGateway {
    override suspend fun setEnabled(enabled: Boolean) = Unit
    override suspend fun login(externalId: String, generation: Long) = Unit
    override suspend fun logout(generation: Long) = Unit
}

private fun grantedPermissionGateway() = FakePermissionGateway(
    requestResult = PermissionState.Granted,
)

private fun semanticPayload(eventId: String, type: String): Map<String, String> = mapOf(
    "push_version" to "1",
    "push_event_id" to eventId,
    "push_type" to type,
    "sample" to "true",
)
