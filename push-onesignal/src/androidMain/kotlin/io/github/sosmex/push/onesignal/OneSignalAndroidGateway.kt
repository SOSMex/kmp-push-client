package io.github.sosmex.push.onesignal

import android.content.Context
import com.onesignal.OneSignal

/** Thin OneSignal binding. Call [initialize] once from the Application host. */
class OneSignalAndroidGateway(
    context: Context,
    appId: String,
) : OneSignalGateway {
    init {
        require(appId.isNotBlank()) { "OneSignal app ID must be configured by the host" }
        OneSignal.initWithContext(context.applicationContext, appId)
    }

    override suspend fun setEnabled(enabled: Boolean) {
        if (enabled) OneSignal.User.pushSubscription.optIn()
        else OneSignal.User.pushSubscription.optOut()
    }

    override suspend fun login(externalId: String, generation: Long) {
        OneSignal.login(externalId)
    }

    override suspend fun logout(generation: Long) {
        OneSignal.User.pushSubscription.optOut()
        OneSignal.logout()
    }
}
