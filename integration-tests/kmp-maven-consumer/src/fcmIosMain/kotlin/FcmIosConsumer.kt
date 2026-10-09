package io.github.sosmex.push.consumer

import io.github.sosmex.push.fcm.FcmInitializationResult
import io.github.sosmex.push.fcm.FcmIosGateway
import io.github.sosmex.push.fcm.FcmIosHostApi

/** Compiles the iOS host seam from the published KLIB into a linked framework. */
fun createFcmIosGateway(
    hostApi: FcmIosHostApi,
    configured: Boolean,
): FcmInitializationResult<FcmIosGateway> =
    FcmIosGateway.create(hostApi, configured)
