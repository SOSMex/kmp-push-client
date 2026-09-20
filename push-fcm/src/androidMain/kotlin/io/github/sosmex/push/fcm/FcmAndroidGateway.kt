package io.github.sosmex.push.fcm

import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging

/** Android FCM binding. Permission requests stay in the host Activity/UI boundary. */
class FcmAndroidGateway private constructor(
    private val messaging: FirebaseMessaging = FirebaseMessaging.getInstance(),
) : FcmGateway {
    override suspend fun setEnabled(enabled: Boolean) {
        messaging.isAutoInitEnabled = enabled
    }

    companion object {
        /**
         * Creates the gateway only when the host has configured Firebase's default app.
         * This check is never evaluated by the OneSignal adapter.
         */
        fun create(): FcmInitializationResult<FcmAndroidGateway> =
            initializeFcm(
                isDefaultFirebaseAppConfigured = {
                    try {
                        FirebaseApp.getInstance()
                        true
                    } catch (_: IllegalStateException) {
                        false
                    }
                },
                initializer = { FcmAndroidGateway() },
            )
    }
}
