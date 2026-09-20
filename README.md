# KMP Push Client 0.1.0

A small Kotlin Multiplatform push client contract for Android and iOS. It supports one provider per build and preserves the real differences between Firebase Cloud Messaging and OneSignal.

This is an independent repository and local 0.1.0 evaluation build. It has not been published to Maven Central or any remote repository.

## Motivation

Push notification integrations often start as a few platform callbacks and grow into duplicated lifecycle logic, provider-specific assumptions in shared code, and claims that a token means a notification was delivered. Multiplatform wrappers can make that worse when they flatten APIs that are not equivalent.

KMP Push Client exists to provide a deliberately small shared contract while keeping those differences visible:

- FCM exposes a registration token and transport lifecycle; it has no equivalent to OneSignal login or external identity.
- OneSignal exposes users, subscriptions and identity operations in addition to notification transport.
- registration, provider acceptance, operating-system delivery and user opening are separate observations;
- foreground receipt and notification opening are different events;
- cold-start callbacks, duplicate delivery and identity changes need deterministic handoff rules;
- tokens, subscription IDs, external IDs and payload values must not leak through diagnostics.

The goal is not to replace either native SDK. The goal is to keep shared application code small, testable and honest while platform hosts continue to own native lifecycle integration.

## Scope

Version 0.1.0 includes:

- Android and iOS targets;
- exactly one selected provider adapter per application build;
- FCM and OneSignal adapters;
- permission state and permission request boundaries;
- explicit enable/disable state;
- typed `FcmToken` and `OneSignalSubscriptionId` destinations;
- neutral, versioned semantic payloads;
- foreground-received and opened events, including cold start;
- atomic exactly-once handoff within a caller-supplied ledger scope;
- generation fencing for stale identity/provider callbacks;
- optional OneSignal identity and explicit unsupported FCM identity;
- deterministic fakes for application tests;
- local Maven publication metadata.

Version 0.1.0 intentionally excludes:

- sending APIs, backend orchestration and provider credentials;
- permission UI or Compose components;
- local notifications, topics, campaigns and analytics;
- rich media, notification actions and in-app messages;
- scheduling, web and desktop runtime targets;
- two simultaneously active providers;
- a built-in production persistence implementation.

See [`docs/spec.md`](docs/spec.md) for the behavioral contract and [`docs/verification-report.md`](docs/verification-report.md) for the evidence actually collected.

## Modules

| Module | Purpose |
| --- | --- |
| `push-core` | Permission, enablement, typed destinations, neutral payloads, events, fencing and deduplication ports |
| `push-fcm` | FCM client, Android Firebase Messaging gateway and iOS callback bridge |
| `push-onesignal` | OneSignal client, identity capability, Android gateway and iOS callback bridge |
| `push-test` | Deterministic fake client, permission/transport fakes and atomic in-memory ledger |
| `sample` | Credential-free JVM example |

Choose exactly one provider adapter in an application build. The core and test libraries may be shared.

## Installation

The project can be published into its local test Maven repository:

```bash
./gradlew \
  :push-core:publishAllPublicationsToTestRepository \
  :push-fcm:publishAllPublicationsToTestRepository \
  :push-onesignal:publishAllPublicationsToTestRepository \
  :push-test:publishAllPublicationsToTestRepository
```

Set `ANDROID_HOME` or an ignored `local.properties` with `sdk.dir=...` before Android tasks.

For local composite development, depend directly on the modules. Proposed coordinates for a future public publication are:

```kotlin
commonMain.dependencies {
    implementation("io.github.sosmex.push:push-core:0.1.0")
    implementation("io.github.sosmex.push:push-fcm:0.1.0") // or push-onesignal
}
```

The final public group ID and repository are owner decisions and have not been reserved.

## Usage example

The following Android FCM example shows the intended ownership split. The application supplies permission UI/state and durable deduplication storage; the SDK owns provider-neutral state and event handoff.

```kotlin
val pushClient = FcmPushClient(
    permissionGateway = appNotificationPermissionGateway,
    gateway = FirebaseMessagingAndroidGateway(),
    ledger = appDurableEventLedger,
    initiallyEnabled = false,
)

// Called from application orchestration, never inferred from token presence.
applicationScope.launch {
    when (pushClient.requestPermission()) {
        PermissionState.Granted -> pushClient.setEnabled(true)
        PermissionState.Provisional -> pushClient.setEnabled(true)
        else -> pushClient.setEnabled(false)
    }
}

// Called by FirebaseMessagingService.onNewToken.
fun onNewFcmToken(token: String) {
    applicationScope.launch {
        pushClient.observeToken(
            token = token,
            generation = pushClient.currentGeneration(),
        )
    }
}

// Called by the host's foreground and Activity/intent callbacks.
fun onNotificationOpened(data: Map<String, String>, coldStart: Boolean) {
    applicationScope.launch {
        pushClient.offerOpened(
            data = data,
            generation = pushClient.currentGeneration(),
            coldStart = coldStart,
        )
    }
}

applicationScope.launch {
    pushClient.events.collect { event ->
        when (event) {
            is PushEvent.ForegroundReceived -> refreshAuthoritativeState(event.payload)
            is PushEvent.Opened -> navigateFromVerifiedHint(event.payload)
        }
    }
}
```

Payloads use a minimal semantic envelope:

```kotlin
val payload = mapOf(
    "push_version" to "1",
    "push_event_id" to "orders:updated:42",
    "push_type" to "orders.updated",
    "order_id" to "42",
)
```

For tests, consumers can drive the same contract without a provider SDK:

```kotlin
val fake = FakePushClient(initiallyEnabled = true)

val first = fake.emitOpened(payload, coldStart = true)   // Accepted
val again = fake.emitOpened(payload, coldStart = true)   // Duplicate

val oldGeneration = fake.currentGeneration()
fake.advanceGeneration()
val stale = fake.emitForeground(payload, oldGeneration) // StaleGeneration
```

The sample can be run with:

```bash
./gradlew :sample:run
```

## Compatibility snapshot

| Component | Version used by this evaluation |
| --- | --- |
| Kotlin | 2.4.10 |
| Gradle wrapper | 9.1.0 |
| Android Gradle Plugin | 9.0.1 |
| Android compile/min SDK | 36 / 24 |
| kotlinx.coroutines | 1.11.0 |
| Firebase Android BoM | 34.18.0 |
| OneSignal Android | 5.9.8 |
| Apple integration | Host-managed Firebase/OneSignal SDK via SPM or CocoaPods |

These pins prove only the local checks recorded in `docs/verification-report.md`. They are not a blanket compatibility promise.

## Core setup

Production code must provide:

- a `PermissionGateway` owned by the host UI/platform layer;
- an `AtomicEventLedger` whose durability matches the product's cold-start requirements;
- one provider gateway;
- the identity generation attached to every native callback.

The in-memory ledger in `push-test` is for tests and samples. It is not a durable production fallback.

## Android: FCM

Add the Google Services plugin and the app's own `google-services.json` in the consuming app. Credentials/configuration do not belong in this SDK.

Create `FirebaseMessagingAndroidGateway` and `FcmPushClient`, then forward `FirebaseMessagingService.onNewToken` to `observeToken`. Forward foreground data messages to `offerForeground`. For a notification opened from an Activity intent, call `offerOpened` once with `coldStart = savedInstanceState == null` and clear the consumed extras.

`setEnabled(false)` disables Firebase Messaging auto-init and hides the destination in client state. It does not claim to delete a token already issued by Firebase.

Android 13+ permission requests remain in the Activity/Compose host and return their result through `PermissionGateway`; this library ships no permission UI.

## Android: OneSignal

Construct `OneSignalAndroidGateway(applicationContext, appId)` and `OneSignalPushClient`. Register OneSignal push-subscription, notification lifecycle and click observers in the host. Forward subscription IDs only after the currently logged external ID is confirmed, and attach the current generation to every callback.

The identity capability is real for OneSignal:

```kotlin
when (val result = client.identity.login(accountOpaqueId)) {
    is CapabilityResult.Invoked -> Unit // invoked, not provider-confirmed
    is CapabilityResult.Rejected -> showRecoverableFailure()
    is CapabilityResult.Unsupported -> error("unexpected provider configuration")
}
```

Do not log the external ID or subscription ID.

## iOS: FCM

The consuming Xcode app owns Firebase installation (Swift Package Manager is recommended by Firebase), `FirebaseApp.configure()`, APNs registration, `MessagingDelegate`, and `UNUserNotificationCenterDelegate`.

Create `FcmIosCallbackBridge` from the exported Kotlin framework and forward:

- `messaging(_:didReceiveRegistrationToken:)` to `didReceiveRegistrationToken`;
- foreground notification data to `didReceiveForeground`;
- response/open data to `didOpen`, setting `coldStart` only from actual launch context.

When Firebase method swizzling is disabled, the host must map its APNs token according to Firebase's setup guide. The SDK does not infer delivery from token registration.

## iOS: OneSignal

The consuming Xcode app owns OneSignalFramework installation and initialization. Register its subscription, foreground lifecycle, click and user-state observers, then forward them through `OneSignalIosCallbackBridge`. Callbacks observed before current external identity confirmation must use `identityConfirmed = false`; they will not publish a destination.

## Payload contract

Version 1 requires these string fields:

- `push_version=1`
- `push_event_id`: stable semantic event ID used for handoff deduplication
- `push_type`: product-owned semantic type

Additional bounded string fields are preserved in `SemanticPayload.data`. The SDK does not interpret product entities and treats the payload as an untrusted hint. Unknown versions and malformed fields fail closed.

## Evidence states

This client distinguishes:

- destination registration observed by a native SDK;
- provider request acceptance (not exposed in V1);
- delivery callback observed by the application;
- notification open callback observed after user/system interaction.

Registration is not proof of provider acceptance, OS delivery or user opening. A foreground callback is not converted into an open.

## Limitations

- Real push delivery requires configured provider projects, APNs credentials and physical devices.
- Exactly-once applies to SDK handoff within the atomic ledger's scope, not global provider delivery.
- No built-in durable Android/iOS ledger is included in 0.1.0.
- Apple SDK dependencies are intentionally host-managed; iOS bridges compile but require host integration.
- No sending/backend, credentials, UI, local notifications, campaigns, analytics, topics, rich media/actions, in-app messages, scheduling, web/desktop runtime, or dual-provider mode.

## License

Apache License 2.0. See `LICENSE`.
