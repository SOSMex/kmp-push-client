package io.github.sosmex.push.fcm

import io.github.sosmex.push.core.AtomicEventLedger
import io.github.sosmex.push.core.PermissionGateway

/** Builds the same FCM client from an initialized Android or iOS gateway. */
object FcmClientFactory {
    fun create(
        permissionGateway: PermissionGateway,
        gateway: FcmInitializationResult<FcmGateway>,
        ledger: AtomicEventLedger,
        initiallyEnabled: Boolean = false,
    ): FcmInitializationResult<FcmPushClient> =
        when (gateway) {
            is FcmInitializationResult.Ready ->
                FcmInitializationResult.Ready(
                    FcmPushClient(
                        permissionGateway = permissionGateway,
                        gateway = gateway.value,
                        ledger = ledger,
                        initiallyEnabled = initiallyEnabled,
                    )
                )

            is FcmInitializationResult.Unavailable -> gateway
        }
}
