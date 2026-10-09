package io.github.sosmex.push.consumer

import io.github.sosmex.push.core.ProviderKind
import io.github.sosmex.push.fcm.FcmPushClient

/** Exercises a provider symbol through the selected FCM publication only. */
fun fcmProvider(client: FcmPushClient): ProviderKind = client.provider
