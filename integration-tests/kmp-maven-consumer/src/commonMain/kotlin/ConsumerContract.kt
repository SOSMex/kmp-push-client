package io.github.sosmex.push.consumer

import io.github.sosmex.push.core.ProviderKind
import io.github.sosmex.push.core.PushClient

/** Compiles the neutral API from the root KMP metadata publication. */
fun observedProvider(client: PushClient): ProviderKind = client.provider
