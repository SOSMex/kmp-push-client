package io.github.sosmex.push.core

enum class ProviderKind {
    FCM,
    ONESIGNAL,
}

sealed interface PermissionState {
    data object NotDetermined : PermissionState
    data object Denied : PermissionState
    data object Granted : PermissionState
    data object Provisional : PermissionState
    data object Ephemeral : PermissionState
    data class Unavailable(val reason: UnavailableReason) : PermissionState
}

enum class UnavailableReason {
    PLATFORM_UNSUPPORTED,
    SDK_NOT_CONFIGURED,
    HOST_INTEGRATION_MISSING,
}

enum class EnablementState {
    ENABLED,
    DISABLED,
}

sealed class PushDestination protected constructor(private val rawValue: String) {
    init {
        require(rawValue.isNotBlank()) { "Destination must not be blank" }
        require(rawValue == rawValue.trim()) { "Destination must be trimmed" }
        require(rawValue.length <= MAX_DESTINATION_LENGTH) { "Destination is too long" }
    }

    val value: String get() = rawValue

    final override fun toString(): String = "${this::class.simpleName}(<redacted>)"

    final override fun equals(other: Any?): Boolean =
        other != null && this::class == other::class &&
            other is PushDestination && rawValue == other.rawValue

    final override fun hashCode(): Int = 31 * this::class.hashCode() + rawValue.hashCode()

    class FcmToken(value: String) : PushDestination(value)
    class OneSignalSubscriptionId(value: String) : PushDestination(value)

    private companion object {
        const val MAX_DESTINATION_LENGTH = 4_096
    }
}

sealed interface DestinationState {
    data object Resolving : DestinationState
    data object Disabled : DestinationState
    data object Unavailable : DestinationState
    data class Available(val destination: PushDestination) : DestinationState {
        override fun toString(): String = "Available(${destination})"
    }
}

class SemanticPayload private constructor(
    val version: Int,
    val eventId: String,
    val type: String,
    val data: Map<String, String>,
) {
    override fun toString(): String =
        "SemanticPayload(version=$version, eventId=<redacted>, type=$type, data=<redacted>)"

    companion object {
        const val SUPPORTED_VERSION = 1
        private const val MAX_EVENT_ID = 160
        private const val MAX_TYPE = 80
        private const val MAX_ENTRIES = 32
        private const val MAX_KEY = 80
        private const val MAX_VALUE = 1_024

        fun decode(values: Map<String, String>): PayloadDecodeResult {
            val versionText = values["push_version"]
                ?: return PayloadDecodeResult.Malformed("missing push_version")
            val version = versionText.toIntOrNull()
                ?.takeIf { it > 0 && it.toString() == versionText }
                ?: return PayloadDecodeResult.Malformed("invalid push_version")
            if (version != SUPPORTED_VERSION) return PayloadDecodeResult.Incompatible(version)

            val eventId = values["push_event_id"].bounded(MAX_EVENT_ID)
                ?: return PayloadDecodeResult.Malformed("invalid push_event_id")
            val type = values["push_type"].bounded(MAX_TYPE)
                ?: return PayloadDecodeResult.Malformed("invalid push_type")
            if (values.size > MAX_ENTRIES) return PayloadDecodeResult.Malformed("too many fields")
            if (values.any { (key, value) -> key.bounded(MAX_KEY) == null || value.length > MAX_VALUE }) {
                return PayloadDecodeResult.Malformed("field outside bounds")
            }
            return PayloadDecodeResult.Decoded(
                SemanticPayload(version, eventId, type, values.toMap())
            )
        }

        private fun String?.bounded(max: Int): String? =
            this?.takeIf { it.isNotBlank() && it == it.trim() && it.length <= max }
    }
}

sealed interface PayloadDecodeResult {
    data class Decoded(val payload: SemanticPayload) : PayloadDecodeResult
    data class Malformed(val reason: String) : PayloadDecodeResult
    data class Incompatible(val observedVersion: Int) : PayloadDecodeResult
}

sealed interface PushEvent {
    val payload: SemanticPayload
    val generation: Long

    data class ForegroundReceived(
        override val payload: SemanticPayload,
        override val generation: Long,
    ) : PushEvent {
        override fun toString(): String =
            "ForegroundReceived(payload=<redacted>, generation=$generation)"
    }

    data class Opened(
        override val payload: SemanticPayload,
        override val generation: Long,
        val coldStart: Boolean,
    ) : PushEvent {
        override fun toString(): String =
            "Opened(payload=<redacted>, generation=$generation, coldStart=$coldStart)"
    }
}
