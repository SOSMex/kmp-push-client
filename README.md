# KMP Push Client 0.1.0

A small Kotlin Multiplatform push client contract for Android and iOS. It supports one provider per build and preserves the real differences between Firebase Cloud Messaging and OneSignal.

This is a local evaluation build. It has not been published to Maven Central or any remote repository.

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
./gradlew publishAllPublicationsToTestRepository
```

For local composite development, depend directly on the modules. Proposed coordinates for a future public publication are:

```kotlin
commonMain.dependencies {
    implementation("io.github.sosmex.push:push-core:0.1.0")
    implementation("io.github.sosmex.push:push-fcm:0.1.0") // or push-onesignal
}
```

The final public group ID and repository are owner decisions and have not been reserved.

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
