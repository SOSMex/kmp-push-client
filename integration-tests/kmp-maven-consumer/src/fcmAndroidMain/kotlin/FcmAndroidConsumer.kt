package io.github.sosmex.push.consumer

import io.github.sosmex.push.fcm.FcmAndroidGateway
import io.github.sosmex.push.fcm.FcmInitializationResult

/** Compiles the Android native gateway from the published AAR. */
fun createFcmAndroidGateway(): FcmInitializationResult<FcmAndroidGateway> =
    FcmAndroidGateway.create()
