package io.github.sosmex.push.onesignal

import io.github.sosmex.push.core.EventOfferResult

/**
 * Swift host entry points for OneSignalFramework callbacks.
 * The host owns OneSignal initialization, observers and SPM/CocoaPods configuration.
 */
class OneSignalIosCallbackBridge(private val client: OneSignalPushClient) {
    suspend fun didChangeSubscription(
        subscriptionId: String?,
        generation: Long,
        identityConfirmed: Boolean,
    ): EventOfferResult? =
        client.observeSubscriptionId(subscriptionId, generation, identityConfirmed)

    suspend fun didReceiveForeground(
        data: Map<String, String>,
        generation: Long,
    ): EventOfferResult = client.offerForeground(data, generation)

    suspend fun didOpen(
        data: Map<String, String>,
        generation: Long,
        coldStart: Boolean,
    ): EventOfferResult = client.offerOpened(data, generation, coldStart)
}

