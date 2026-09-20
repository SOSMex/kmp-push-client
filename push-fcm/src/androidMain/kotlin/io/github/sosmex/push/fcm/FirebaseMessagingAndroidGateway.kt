package io.github.sosmex.push.fcm

import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging

/** Thin Firebase binding. Permission requests stay in the host Activity/UI boundary. */
class FirebaseMessagingAndroidGateway private constructor(
    private val messaging: FirebaseMessaging = FirebaseMessaging.getInstance(),
) : FcmGateway {
    override suspend fun setEnabled(enabled: Boolean) {
        messaging.isAutoInitEnabled = enabled
    }

    companion object {
        /**
         * Creates the FCM binding only when the host has configured Firebase's default app.
         * This check intentionally lives in push-fcm and is never evaluated by push-onesignal.
         */
        fun create(): FcmInitializationResult<FirebaseMessagingAndroidGateway> =
            initializeFcm(
                isDefaultFirebaseAppConfigured = {
                    try {
                        FirebaseApp.getInstance()
                        true
                    } catch (_: IllegalStateException) {
                        false
                    }
                },
                initializer = { FirebaseMessagingAndroidGateway() },
            )
    }
}
