package io.github.sosmex.push.sample

import io.github.sosmex.push.core.EventOfferResult
import io.github.sosmex.push.core.ProviderKind
import io.github.sosmex.push.core.PushEvent
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

fun main(args: Array<String>) = runBlocking {
    val providers = providerArgument(args)?.let(::listOf) ?: ProviderKind.entries

    println("KMP Push Client sample")
    println("Native provider callbacks are simulated; no credentials or delivery are involved.\n")
    providers.forEach { runScenario(it) }
}

private suspend fun runScenario(provider: ProviderKind) = coroutineScope {
    val app = DemoPushApp.create(provider)
    val eventListener = launch(start = CoroutineStart.UNDISPATCHED) {
        app.events.take(2).collect { event ->
            println("  event listener  ${event.summary()}")
        }
    }

    println("${provider.displayName()} demo")
    println("  permission      ${app.initialize().displayName()}")
    println("  registration    ${app.simulateRegistration().outcomeName()}")
    println("  destination     ${app.snapshot.destination}")
    println("  foreground      ${app.simulateForeground().outcomeName()}")
    println("  cold-start open ${app.simulateColdStartOpen().outcomeName()}")
    println("  duplicate open  ${app.simulateColdStartOpen().outcomeName()}")
    eventListener.join()
    println()
}

private fun providerArgument(args: Array<String>): ProviderKind? {
    val rawValue = args.firstOrNull { it.startsWith("--provider=") }
        ?.substringAfter('=')
        ?.lowercase()
        ?: return null
    return when (rawValue) {
        "fcm" -> ProviderKind.FCM
        "onesignal" -> ProviderKind.ONESIGNAL
        else -> error("Unknown provider '$rawValue'. Use fcm or onesignal.")
    }
}

private fun ProviderKind.displayName(): String = when (this) {
    ProviderKind.FCM -> "FCM"
    ProviderKind.ONESIGNAL -> "OneSignal"
}

private fun Any?.displayName(): String = this?.let { it::class.simpleName } ?: "None"

private fun EventOfferResult?.outcomeName(): String = when (this) {
    null -> "Observed"
    is EventOfferResult.Accepted -> "Accepted"
    EventOfferResult.Duplicate -> "Duplicate"
    EventOfferResult.StaleGeneration -> "StaleGeneration"
    is EventOfferResult.Malformed -> "Malformed"
    is EventOfferResult.Incompatible -> "Incompatible"
    is EventOfferResult.StorageFailure -> "StorageFailure"
}

private fun PushEvent.summary(): String = when (this) {
    is PushEvent.ForegroundReceived -> "ForegroundReceived(type=${payload.type})"
    is PushEvent.Opened -> "Opened(type=${payload.type}, coldStart=$coldStart)"
}
