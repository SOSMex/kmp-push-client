# KMP Push Client 0.1.0

[![CI](https://github.com/SOSMex/kmp-push-client/actions/workflows/ci.yml/badge.svg)](https://github.com/SOSMex/kmp-push-client/actions/workflows/ci.yml)

A small Kotlin Multiplatform push client contract for Android and iOS. It supports one provider per build and preserves the real differences between Firebase Cloud Messaging and OneSignal.

This is an independent repository and 0.1.0 evaluation build hosted privately on GitHub. No package version has been published to GitHub Packages or Maven Central yet.

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

Choose exactly one provider adapter in an application build. The core and test libraries may be shared. Provider setup is independent:

| Selected adapter | Android app configuration | iOS app configuration |
| --- | --- | --- |
| `push-fcm` | Firebase SDK, Google Services plugin and the app's `google-services.json` | Firebase SDK, the app's `GoogleService-Info.plist` and `FirebaseApp.configure()` |
| `push-onesignal` | OneSignal App ID; configure the Android platform's Firebase credentials in OneSignal | OneSignal App ID and APNs credentials/capabilities |

A OneSignal-only consumer does not add `push-fcm`, does not need a default `FirebaseApp`, and does not add either Firebase configuration file. On Android, OneSignal still uses FCM as its underlying Google push transport and brings Firebase Messaging transitively; OneSignal 5.9.8 initializes its own named Firebase app from the platform configuration associated with the OneSignal App ID. On iOS, OneSignal uses APNs rather than Firebase.

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

For local composite development, depend directly on the modules. GitHub Packages publications use these coordinates:

```kotlin
commonMain.dependencies {
    implementation("io.github.sosmex.push:push-core:0.1.0")
    implementation("io.github.sosmex.push:push-fcm:0.1.0")
    // Or use push-onesignal instead. Do not add both adapters for one-provider builds.
}
```

The repository and its GitHub Packages registry are currently private. Public Maven Central coordinates, signing and availability remain separate owner decisions.

## CI/CD

The repository includes two GitHub Actions workflows:

- `CI` runs for pull requests targeting `main`, pushes to `main`, and manual dispatches. Ubuntu validates JVM behavior, Android bindings and the credential-free sample. A fixed Apple Silicon `macos-15` runner validates iOS simulator behavior, device-target compilation and the complete local Maven publication shape.
- `Publish GitHub Packages` runs only when a GitHub Release is published. It requires a semantic tag such as `v0.1.0`, verifies that the released commit belongs to `main`, reruns the cross-platform release matrix and publishes all four library modules to the repository's GitHub Packages Maven registry.

The publication workflow uses the scoped GitHub Actions token; no provider credentials, Firebase files or custom publishing secrets are required. It does not publish to Maven Central and it does not create a public release automatically.

Authenticated consumers can add the private registry and use the coordinates above. See [`docs/ci-cd.md`](docs/ci-cd.md) for the release gate and Gradle repository example.

## Usage example

The following Android FCM example shows the intended ownership split. The application supplies permission UI/state and durable deduplication storage; the SDK owns provider-neutral state and event handoff. Initialization fails closed when Firebase's default app is absent:

```kotlin
val firebaseGateway = when (val result = FirebaseMessagingAndroidGateway.create()) {
    is FcmInitializationResult.Ready -> result.value
    is FcmInitializationResult.Unavailable -> {
        reportPushUnavailable(result.reason) // enum only; do not log configuration values
        return
    }
}

val pushClient = FcmPushClient(
    permissionGateway = appNotificationPermissionGateway,
    gateway = firebaseGateway,
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

Call `FirebaseMessagingAndroidGateway.create()` before constructing `FcmPushClient`. It returns `Unavailable(DEFAULT_FIREBASE_APP_NOT_CONFIGURED)` instead of allowing `FirebaseMessaging.getInstance()` to fail when the Google Services plugin/file is absent or Firebase has not otherwise initialized the default app. An unexpected SDK failure returns `Unavailable(SDK_INITIALIZATION_FAILED)` without carrying exception/configuration details.

After a `Ready` result, forward `FirebaseMessagingService.onNewToken` to `observeToken`. Forward foreground data messages to `offerForeground`. For a notification opened from an Activity intent, call `offerOpened` once with `coldStart = savedInstanceState == null` and clear the consumed extras.

`setEnabled(false)` disables Firebase Messaging auto-init and hides the destination in client state. It does not claim to delete a token already issued by Firebase.

Android 13+ permission requests remain in the Activity/Compose host and return their result through `PermissionGateway`; this library ships no permission UI.

## Android: OneSignal

Construct `OneSignalAndroidGateway(applicationContext, appId)` and `OneSignalPushClient`. Register OneSignal push-subscription, notification lifecycle and click observers in the host. Forward subscription IDs only after the currently logged external ID is confirmed, and attach the current generation to every callback.

Do not add the Google Services plugin, `google-services.json`, or `push-fcm` solely for this adapter. Configure the Android Firebase service-account credentials in the OneSignal dashboard and initialize the native SDK with the corresponding OneSignal App ID. Although Firebase Messaging is an Android transport dependency of OneSignal, the consuming application does not own or initialize a default Firebase app for this path.

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

Before constructing the bridge, the Swift host must verify both bundle membership and runtime initialization. If either check fails, leave push unavailable and do not install Firebase delegates:

```swift
guard Bundle.main.url(forResource: "GoogleService-Info", withExtension: "plist") != nil else {
    reportPushUnavailable(.missingFirebaseConfiguration)
    return
}

if FirebaseApp.app() == nil {
    FirebaseApp.configure()
}

guard FirebaseApp.app() != nil else {
    reportPushUnavailable(.firebaseInitializationFailed)
    return
}
```

`FcmIosCallbackBridge.create(client:isDefaultFirebaseAppConfigured:)` accepts that final runtime check and returns the same fail-closed `FcmInitializationResult`. This module deliberately does not inspect the app bundle or link Firebase itself because the native Firebase dependency remains host-managed.

After the factory returns `Ready`, use its `FcmIosCallbackBridge` value and forward:

- `messaging(_:didReceiveRegistrationToken:)` to `didReceiveRegistrationToken`;
- foreground notification data to `didReceiveForeground`;
- response/open data to `didOpen`, setting `coldStart` only from actual launch context.

When Firebase method swizzling is disabled, the host must map its APNs token according to Firebase's setup guide. The SDK does not infer delivery from token registration.

## iOS: OneSignal

The consuming Xcode app owns OneSignalFramework installation and initialization. Register its subscription, foreground lifecycle, click and user-state observers, then forward them through `OneSignalIosCallbackBridge`. Callbacks observed before current external identity confirmation must use `identityConfirmed = false`; they will not publish a destination.

This path does not use Firebase and must not require `GoogleService-Info.plist` or `FirebaseApp.configure()`. Configure APNs credentials for the iOS platform in OneSignal and add the native push capabilities required by its setup guide.

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
