package io.github.sosmex.push.consumer

import io.github.sosmex.push.onesignal.OneSignalIosCallbackBridge
import io.github.sosmex.push.onesignal.OneSignalPushClient

/** Compiles the iOS callback bridge from the published KLIB into a linked framework. */
fun createOneSignalIosBridge(client: OneSignalPushClient): OneSignalIosCallbackBridge =
    OneSignalIosCallbackBridge(client)
