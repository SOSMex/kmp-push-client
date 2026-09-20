package io.github.sosmex.push.test

import io.github.sosmex.push.core.EventOfferResult
import io.github.sosmex.push.core.PermissionState
import io.github.sosmex.push.core.ProviderKind
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class FakePushClientTest {
    @Test
    fun fakeDrivesPermissionDuplicateStorageFailureAndFence() = runTest {
        val permission = FakePermissionGateway(
            initial = PermissionState.NotDetermined,
            requestResult = PermissionState.Denied,
        )
        val client = FakePushClient(
            provider = ProviderKind.ONESIGNAL,
            permissionGateway = permission,
            initiallyEnabled = true,
        )
        val payload = mapOf(
            "push_version" to "1",
            "push_event_id" to "event-1",
            "push_type" to "sample.updated",
        )

        assertEquals(PermissionState.Denied, client.requestPermission())
        assertIs<EventOfferResult.Accepted>(client.emitOpened(payload, coldStart = true))
        assertEquals(EventOfferResult.Duplicate, client.emitOpened(payload, coldStart = true))

        val oldGeneration = client.currentGeneration()
        client.advanceGeneration()
        assertEquals(
            EventOfferResult.StaleGeneration,
            client.emitForeground(payload, generation = oldGeneration),
        )

        client.ledger.failWith = "disk unavailable"
        assertIs<EventOfferResult.StorageFailure>(
            client.emitOpened(payload + ("push_event_id" to "event-2"), coldStart = false)
        )
    }
}
