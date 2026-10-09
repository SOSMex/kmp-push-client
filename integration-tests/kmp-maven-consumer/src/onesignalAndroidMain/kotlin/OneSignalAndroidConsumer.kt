package io.github.sosmex.push.consumer

import android.content.Context
import io.github.sosmex.push.onesignal.OneSignalAndroidGateway

/** Compiles the Android native gateway from the published AAR. */
fun createOneSignalAndroidGateway(
    context: Context,
    appId: String,
): OneSignalAndroidGateway = OneSignalAndroidGateway(context, appId)
