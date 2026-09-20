package io.github.sosmex.push.fcm

enum class FcmInitializationFailure {
    DEFAULT_FIREBASE_APP_NOT_CONFIGURED,
    SDK_INITIALIZATION_FAILED,
}

sealed interface FcmInitializationResult<out T> {
    data class Ready<T>(val value: T) : FcmInitializationResult<T>

    data class Unavailable(
        val reason: FcmInitializationFailure,
    ) : FcmInitializationResult<Nothing>
}

internal inline fun <T> initializeFcm(
    isDefaultFirebaseAppConfigured: () -> Boolean,
    initializer: () -> T,
): FcmInitializationResult<T> {
    val configured =
        try {
            isDefaultFirebaseAppConfigured()
        } catch (_: Throwable) {
            return FcmInitializationResult.Unavailable(FcmInitializationFailure.SDK_INITIALIZATION_FAILED)
        }

    if (!configured) {
        return FcmInitializationResult.Unavailable(
            FcmInitializationFailure.DEFAULT_FIREBASE_APP_NOT_CONFIGURED,
        )
    }

    return try {
        FcmInitializationResult.Ready(initializer())
    } catch (_: Throwable) {
        FcmInitializationResult.Unavailable(FcmInitializationFailure.SDK_INITIALIZATION_FAILED)
    }
}
