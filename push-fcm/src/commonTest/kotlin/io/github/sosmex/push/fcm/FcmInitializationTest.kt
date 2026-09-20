package io.github.sosmex.push.fcm

import io.github.sosmex.push.core.AtomicEventLedger
import io.github.sosmex.push.core.LedgerClaim
import io.github.sosmex.push.core.PermissionGateway
import io.github.sosmex.push.core.PermissionState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs

class FcmInitializationTest {
    @Test
    fun missingDefaultFirebaseAppFailsClosedWithoutInitializingSdk() {
        var initializerInvoked = false

        val result =
            initializeFcm(
                isDefaultFirebaseAppConfigured = { false },
                initializer = {
                    initializerInvoked = true
                    "gateway"
                },
            )

        assertEquals(
            FcmInitializationResult.Unavailable(
                FcmInitializationFailure.DEFAULT_FIREBASE_APP_NOT_CONFIGURED,
            ),
            result,
        )
        assertFalse(initializerInvoked)
    }

    @Test
    fun configuredDefaultFirebaseAppReturnsReadyBinding() {
        val result =
            initializeFcm(
                isDefaultFirebaseAppConfigured = { true },
                initializer = { "gateway" },
            )

        assertEquals("gateway", assertIs<FcmInitializationResult.Ready<String>>(result).value)
    }

    @Test
    fun unexpectedSdkFailureIsReportedWithoutLeakingExceptionDetails() {
        val result =
            initializeFcm(
                isDefaultFirebaseAppConfigured = { true },
                initializer = { error("configuration value that must not escape") },
            )

        assertEquals(
            FcmInitializationResult.Unavailable(FcmInitializationFailure.SDK_INITIALIZATION_FAILED),
            result,
        )
        assertFalse(result.toString().contains("configuration value"))
    }

    @Test
    fun clientFactoryBuildsClientFromEitherPlatformGateway() {
        val result = FcmClientFactory.create(
            permissionGateway = FactoryPermission(),
            gateway = FcmInitializationResult.Ready(FactoryGateway()),
            ledger = FactoryLedger(),
        )

        assertIs<FcmInitializationResult.Ready<FcmPushClient>>(result)
    }

    @Test
    fun clientFactoryPreservesInitializationFailure() {
        val unavailable = FcmInitializationResult.Unavailable(
            FcmInitializationFailure.DEFAULT_FIREBASE_APP_NOT_CONFIGURED,
        )

        val result = FcmClientFactory.create(
            permissionGateway = FactoryPermission(),
            gateway = unavailable,
            ledger = FactoryLedger(),
        )

        assertEquals(unavailable, result)
    }
}

private class FactoryPermission : PermissionGateway {
    override val state = MutableStateFlow<PermissionState>(PermissionState.Granted)
    override suspend fun request(): PermissionState = state.value
}

private class FactoryGateway : FcmGateway {
    override suspend fun setEnabled(enabled: Boolean) = Unit
}

private class FactoryLedger : AtomicEventLedger {
    override suspend fun claim(scope: String, eventId: String) = LedgerClaim.Claimed
    override suspend fun clear(scope: String) = Unit
}
