package io.github.sosmex.push.core

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class PushClientEngineTest {
    @Test
    fun coldStartOpenIsClaimedExactlyOnceWithinLedgerScope() = runTest {
        val engine = engine(initiallyEnabled = true)
        val values = payload("event-1")

        val first = engine.offerOpened(values, observedGeneration = 0, coldStart = true)
        val second = engine.offerOpened(values, observedGeneration = 0, coldStart = true)

        assertIs<EventOfferResult.Accepted>(first)
        assertTrue((first.event as PushEvent.Opened).coldStart)
        assertEquals(EventOfferResult.Duplicate, second)
    }

    @Test
    fun concurrentDuplicateOffersEmitOneAcceptedHandoff() = runTest {
        val engine = PushClientEngine(
            provider = ProviderKind.FCM,
            permissionGateway = TestPermissionGateway(),
            transportGateway = object : PushTransportGateway {
                override suspend fun setEnabled(enabled: Boolean) = Unit
            },
            ledger = MutexLedger(),
            initiallyEnabled = true,
        )

        val results = coroutineScope {
            List(32) {
                async { engine.offerOpened(payload("event-race"), 0, coldStart = true) }
            }.awaitAll()
        }

        assertEquals(1, results.count { it is EventOfferResult.Accepted })
        assertEquals(31, results.count { it == EventOfferResult.Duplicate })
    }

    @Test
    fun staleGenerationCannotPublishDestinationOrEvent() = runTest {
        val engine = engine(initiallyEnabled = true)
        assertEquals(1, engine.advanceGeneration())

        val destinationResult = engine.observeDestination(0, PushDestination.FcmToken("secret-token"))
        val eventResult = engine.offerForeground(payload("event-2"), observedGeneration = 0)

        assertEquals(EventOfferResult.StaleGeneration, destinationResult)
        assertEquals(EventOfferResult.StaleGeneration, eventResult)
        assertEquals(DestinationState.Resolving, engine.destination.value)
    }

    @Test
    fun secretsAreRedactedFromDefaultRendering() {
        val destination = PushDestination.OneSignalSubscriptionId("subscription-secret")
        val decoded = SemanticPayload.decode(payload("private-event")) as PayloadDecodeResult.Decoded

        assertFalse(destination.toString().contains("subscription-secret"))
        assertFalse(decoded.payload.toString().contains("private-event"))
        assertFalse(decoded.payload.toString().contains("private-value"))
    }

    @Test
    fun incompatibleAndMalformedPayloadsFailClosed() = runTest {
        val engine = engine(initiallyEnabled = true)
        val incompatible = payload("event-3") + ("push_version" to "2")
        val malformed = payload("event-4") - "push_event_id"

        assertEquals(EventOfferResult.Incompatible(2), engine.offerForeground(incompatible, 0))
        assertIs<EventOfferResult.Malformed>(engine.offerForeground(malformed, 0))
    }

    private fun engine(initiallyEnabled: Boolean) = PushClientEngine(
        provider = ProviderKind.FCM,
        permissionGateway = TestPermissionGateway(),
        transportGateway = object : PushTransportGateway {
            override suspend fun setEnabled(enabled: Boolean) = Unit
        },
        ledger = TestLedger(),
        initiallyEnabled = initiallyEnabled,
    )

    private fun payload(eventId: String) = mapOf(
        "push_version" to "1",
        "push_event_id" to eventId,
        "push_type" to "sample.updated",
        "detail" to "private-value",
    )
}

private class TestPermissionGateway : PermissionGateway {
    override val state = MutableStateFlow<PermissionState>(PermissionState.NotDetermined)
    override suspend fun request(): PermissionState = state.value
}

private class TestLedger : AtomicEventLedger {
    private val claims = mutableSetOf<String>()
    override suspend fun claim(scope: String, eventId: String): LedgerClaim =
        if (claims.add("$scope:$eventId")) LedgerClaim.Claimed else LedgerClaim.AlreadyClaimed

    override suspend fun clear(scope: String) {
        claims.removeAll { it.startsWith("$scope:") }
    }
}

private class MutexLedger : AtomicEventLedger {
    private val mutex = Mutex()
    private val claims = mutableSetOf<String>()

    override suspend fun claim(scope: String, eventId: String): LedgerClaim = mutex.withLock {
        if (claims.add("$scope:$eventId")) LedgerClaim.Claimed else LedgerClaim.AlreadyClaimed
    }

    override suspend fun clear(scope: String) {
        mutex.withLock { claims.removeAll { it.startsWith("$scope:") } }
    }
}
