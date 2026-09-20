package io.github.sosmex.push.fcm

import io.github.sosmex.push.core.EventOfferResult

/**
 * Swift host entry points for Firebase Messaging and UNUserNotificationCenter callbacks.
 * This module intentionally does not own the host's SPM/CocoaPods dependency graph.
 */
class FcmIosCallbackBridge(private val client: FcmPushClient) {
    suspend fun didReceiveRegistrationToken(token: String?, generation: Long): EventOfferResult? =
        client.observeToken(token, generation)

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

