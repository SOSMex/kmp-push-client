# KMP Push Client

[![CI](https://github.com/SOSMex/kmp-push-client/actions/workflows/ci.yml/badge.svg)](https://github.com/SOSMex/kmp-push-client/actions/workflows/ci.yml)

Use Firebase Cloud Messaging or OneSignal from shared Kotlin Multiplatform code without hiding the differences between them.

The library gives Android and iOS apps one small API for permission state, push enablement, registration, foreground notifications and notification opens. Native SDK setup stays in the app, where it belongs.

> The source is at `0.1.0`. Packages have not been released to GitHub Packages or Maven Central yet.

## Why

Push code tends to spread quickly: a token callback in Android, another delegate in iOS, provider state in shared code and slightly different open handling on every screen.

This project keeps that plumbing in one place while staying clear about what each provider can do. An FCM token is not a OneSignal subscription, a registration is not a delivery receipt, and receiving a notification is not the same as opening it.

## What you get

- Android and iOS targets.
- Separate adapters for FCM and OneSignal.
- Typed FCM tokens and OneSignal subscription IDs.
- Permission and enable/disable state.
- Separate foreground and opened events, including cold starts.
- Duplicate-open protection through storage supplied by the app.
- OneSignal login/logout support.
- Fakes for application tests.

The library does not send notifications. It also does not provide notification UI, campaigns, analytics, topics or local notifications.

## Choose a provider

Add `push-core` and one provider module:

| Provider | Module | Android setup | iOS setup |
| --- | --- | --- | --- |
| Firebase Cloud Messaging | `push-fcm` | `google-services.json` and the Google Services plugin | `GoogleService-Info.plist` and `FirebaseApp.configure()` |
| OneSignal | `push-onesignal` | OneSignal App ID; Firebase credentials live in the OneSignal dashboard | OneSignal App ID and APNs configuration |

A OneSignal-only app does not need `push-fcm` or either Firebase configuration file. OneSignal still uses FCM under the hood on Google-enabled Android devices, but its SDK handles that connection. On iOS it uses APNs.

```kotlin
commonMain.dependencies {
    implementation("io.github.sosmex.push:push-core:0.1.0")
    implementation("io.github.sosmex.push:push-fcm:0.1.0")
    // Or: implementation("io.github.sosmex.push:push-onesignal:0.1.0")
}
```

Those coordinates become usable after the first package release. Until then, use the repository as a composite build or publish it locally:

```bash
./gradlew \
  :push-core:publishAllPublicationsToTestRepository \
  :push-fcm:publishAllPublicationsToTestRepository \
  :push-onesignal:publishAllPublicationsToTestRepository \
  :push-test:publishAllPublicationsToTestRepository
```

## Basic FCM example

The app provides permission handling and durable storage for opened-event IDs. The SDK handles the shared state and event stream.

```kotlin
val gateway = when (val result = FirebaseMessagingAndroidGateway.create()) {
    is FcmInitializationResult.Ready -> result.value
    is FcmInitializationResult.Unavailable -> {
        showPushUnavailable(result.reason)
        return
    }
}

val pushClient = FcmPushClient(
    permissionGateway = appPermissionGateway,
    gateway = gateway,
    ledger = appEventLedger,
)

applicationScope.launch {
    if (pushClient.requestPermission() == PermissionState.Granted) {
        pushClient.setEnabled(true)
    }
}

suspend fun onNewFcmToken(token: String) {
    pushClient.observeToken(
        token = token,
        generation = pushClient.currentGeneration(),
    )
}

applicationScope.launch {
    pushClient.events.collect { event ->
        when (event) {
            is PushEvent.ForegroundReceived -> refreshContent()
            is PushEvent.Opened -> openDestination(event.payload)
        }
    }
}
```

Forward notification opens with `offerOpened(...)` and foreground data messages with `offerForeground(...)`. Use the current generation for every native callback so callbacks from an old login or provider session can be ignored.

On iOS, configure Firebase in Swift first and create the bridge only after `FirebaseApp.app()` is available. `FcmIosCallbackBridge.create(...)` returns an unavailable result when the app has not been configured.

## OneSignal example

```kotlin
val gateway = OneSignalAndroidGateway(
    context = applicationContext,
    appId = oneSignalAppId,
)

val pushClient = OneSignalPushClient(
    permissionGateway = appPermissionGateway,
    gateway = gateway,
    ledger = appEventLedger,
)

applicationScope.launch {
    pushClient.identity.login(accountId)
}
```

The host app still registers OneSignal subscription, foreground and click observers and forwards those callbacks to the client. On iOS, use `OneSignalIosCallbackBridge`; Firebase setup is not involved.

## Payloads

The shared client expects three string fields:

```kotlin
val payload = mapOf(
    "push_version" to "1",
    "push_event_id" to "orders:updated:42",
    "push_type" to "orders.updated",
    "order_id" to "42",
)
```

`push_event_id` is the deduplication key. Extra string fields are passed through as application data. Treat them as hints and fetch the latest data from your backend before acting on them.

## Modules

| Module | What it contains |
| --- | --- |
| `push-core` | Shared models, state, events and deduplication contracts |
| `push-fcm` | FCM client, Android gateway and iOS bridge |
| `push-onesignal` | OneSignal client, identity support, Android gateway and iOS bridge |
| `push-test` | Fakes and an in-memory ledger for tests |
| `sample` | Small JVM example with no provider credentials |

## Build and test

Set `ANDROID_HOME` or add an ignored `local.properties` with `sdk.dir=...`, then run:

```bash
./gradlew \
  :push-core:jvmTest \
  :push-fcm:jvmTest \
  :push-onesignal:jvmTest \
  :push-test:jvmTest \
  :push-fcm:compileAndroidMain \
  :push-onesignal:compileAndroidMain \
  :sample:run
```

The project currently uses Kotlin 2.4.10, Android API 36, Firebase BoM 34.18.0 and OneSignal Android 5.9.8.

## Releases

CI runs the JVM, Android and iOS checks on every pull request. Publishing a semantic GitHub Release such as `v0.1.0` triggers publication to the repository's private GitHub Packages registry. Maven Central publication is not configured.

See [CI/CD](docs/ci-cd.md) for the release flow and private registry setup.

## More detail

- [Behavioral specification](docs/spec.md)
- [Architecture decision](docs/rfc-0001-provider-honest-boundary.md)
- [Verification report](docs/verification-report.md)

## License

Apache License 2.0. See [LICENSE](LICENSE).
