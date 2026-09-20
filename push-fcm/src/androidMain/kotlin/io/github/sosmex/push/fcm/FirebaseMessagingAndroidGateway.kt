package io.github.sosmex.push.fcm

import com.google.firebase.messaging.FirebaseMessaging

/** Thin Firebase binding. Permission requests stay in the host Activity/UI boundary. */
class FirebaseMessagingAndroidGateway(
    private val messaging: FirebaseMessaging = FirebaseMessaging.getInstance(),
) : FcmGateway {
    override suspend fun setEnabled(enabled: Boolean) {
        messaging.isAutoInitEnabled = enabled
    }
}
