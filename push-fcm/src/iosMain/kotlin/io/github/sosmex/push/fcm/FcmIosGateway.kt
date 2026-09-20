package io.github.sosmex.push.fcm

/**
 * Minimal Firebase Messaging operations supplied by the Swift host.
 *
 * The host keeps ownership of its SPM/CocoaPods dependency and implements this boundary with
 * `Messaging.messaging().isAutoInitEnabled`.
 */
interface FcmIosHostApi {
    fun setAutoInitEnabled(enabled: Boolean)
}

/** iOS FCM gateway backed by the Firebase Messaging SDK owned by the host application. */
class FcmIosGateway private constructor(
    private val hostApi: FcmIosHostApi,
) : FcmGateway {
    override suspend fun setEnabled(enabled: Boolean) {
        hostApi.setAutoInitEnabled(enabled)
    }

    companion object {
        /**
         * Creates the gateway only after the Swift host has called `FirebaseApp.configure()`.
         * Pass the result of `FirebaseApp.app() != nil` without exposing configuration values.
         */
        fun create(
            hostApi: FcmIosHostApi,
            isDefaultFirebaseAppConfigured: Boolean,
        ): FcmInitializationResult<FcmIosGateway> =
            initializeFcm(
                isDefaultFirebaseAppConfigured = { isDefaultFirebaseAppConfigured },
                initializer = { FcmIosGateway(hostApi) },
            )
    }
}
