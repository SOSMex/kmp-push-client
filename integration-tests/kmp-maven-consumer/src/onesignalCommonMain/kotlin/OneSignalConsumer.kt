package io.github.sosmex.push.consumer

import io.github.sosmex.push.core.ProviderKind
import io.github.sosmex.push.onesignal.OneSignalPushClient

/** Exercises a provider symbol through the selected OneSignal publication only. */
fun oneSignalProvider(client: OneSignalPushClient): ProviderKind = client.provider
