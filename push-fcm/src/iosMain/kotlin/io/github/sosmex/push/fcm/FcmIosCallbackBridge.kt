package io.github.sosmex.push.fcm

import io.github.sosmex.push.core.EventOfferResult

/**
 * Swift host entry points for Firebase Messaging and UNUserNotificationCenter callbacks.
 * This module intentionally does not own the host's SPM/CocoaPods dependency graph.
 */
class FcmIosCallbackBridge private constructor(private val client: FcmPushClient) {
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

    companion object {
        /**
         * Fail-closed factory for Swift hosts. Pass the result of `FirebaseApp.app() != nil`
         * after the host has attempted `FirebaseApp.configure()`.
         */
        fun create(
            client: FcmPushClient,
            isDefaultFirebaseAppConfigured: Boolean,
        ): FcmInitializationResult<FcmIosCallbackBridge> =
            initializeFcm(
                isDefaultFirebaseAppConfigured = { isDefaultFirebaseAppConfigured },
                initializer = { FcmIosCallbackBridge(client) },
            )
    }
}
