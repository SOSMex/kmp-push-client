import io.github.sosmex.push.core.EventOfferResult
import io.github.sosmex.push.core.PermissionState
import io.github.sosmex.push.fcm.FcmClientFactory
import io.github.sosmex.push.fcm.FcmGateway
import io.github.sosmex.push.fcm.FcmInitializationResult
import io.github.sosmex.push.onesignal.OneSignalGateway
import io.github.sosmex.push.onesignal.OneSignalPushClient
import io.github.sosmex.push.test.FakePermissionGateway
import io.github.sosmex.push.test.InMemoryAtomicEventLedger
import kotlinx.coroutines.runBlocking

// This independent build resolves Maven artifacts, never project dependencies.
// Gateways and callbacks are simulated; this is not a device delivery test.
fun main() = runBlocking {
    val fcm = when (val result = FcmClientFactory.create(
        permissionGateway = FakePermissionGateway(requestResult = PermissionState.Granted),
        gateway = FcmInitializationResult.Ready(object : FcmGateway {
            override suspend fun setEnabled(enabled: Boolean) = Unit
        }),
        ledger = InMemoryAtomicEventLedger(),
    )) {
        is FcmInitializationResult.Ready -> result.value
        is FcmInitializationResult.Unavailable -> error("FCM sample gateway unavailable")
    }
    check(fcm.requestPermission() == PermissionState.Granted)
    fcm.setEnabled(true)
    val callbackGeneration = fcm.currentGeneration()
    val payload = mapOf(
        "push_version" to "1",
        "push_event_id" to "consumer:open:1",
        "push_type" to "consumer.opened",
    )
    check(fcm.offerOpened(payload, callbackGeneration, coldStart = true) is EventOfferResult.Accepted)
    check(fcm.offerOpened(payload, callbackGeneration, coldStart = true) == EventOfferResult.Duplicate)
    fcm.advanceGeneration()
    check(fcm.observeToken("simulated-old-token", callbackGeneration) == EventOfferResult.StaleGeneration)

    val oneSignal = OneSignalPushClient(
        permissionGateway = FakePermissionGateway(requestResult = PermissionState.Granted),
        gateway = object : OneSignalGateway {
            override suspend fun setEnabled(enabled: Boolean) = Unit
            override suspend fun login(externalId: String, generation: Long) = Unit
            override suspend fun logout(generation: Long) = Unit
        },
        ledger = InMemoryAtomicEventLedger(),
    )
    check(oneSignal.requestPermission() == PermissionState.Granted)
    oneSignal.setEnabled(true)
    println("maven_consumer=accepted (simulated callbacks; no device delivery)")
}
