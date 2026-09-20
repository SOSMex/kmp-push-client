package io.github.sosmex.push.sample

import io.github.sosmex.push.core.PermissionState
import io.github.sosmex.push.core.ProviderKind
import io.github.sosmex.push.test.FakePermissionGateway
import io.github.sosmex.push.test.FakePushClient
import kotlinx.coroutines.runBlocking

fun main() = runBlocking {
    val client = FakePushClient(
        provider = ProviderKind.ONESIGNAL,
        permissionGateway = FakePermissionGateway(
            requestResult = PermissionState.Granted,
        ),
        initiallyEnabled = true,
    )
    val payload = mapOf(
        "push_version" to "1",
        "push_event_id" to "sample:event:1",
        "push_type" to "sample.updated",
        "record_id" to "sensitive-value",
    )

    println("permission=${client.requestPermission()}")
    println("first_open=${client.emitOpened(payload, coldStart = true)::class.simpleName}")
    println("duplicate_open=${client.emitOpened(payload, coldStart = true)::class.simpleName}")
    val previousGeneration = client.currentGeneration()
    client.advanceGeneration()
    println(
        "stale_callback=" +
            client.emitForeground(payload, generation = previousGeneration)::class.simpleName
    )
}

