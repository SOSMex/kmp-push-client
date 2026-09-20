# Push Client KMP 0.1.0 specification

## Outcome

An Android or iOS application can select exactly one push provider per build and consume a small, provider-honest Kotlin Multiplatform contract for permission, enablement, destination registration, foreground receipt, notification open (including cold start), and identity fencing.

The client reports observations made by the app or provider SDK. It never describes provider acceptance, operating-system delivery, or user opening unless the corresponding callback was actually observed.

## Actors and entry points

- The host application initializes one provider adapter at process launch.
- The host supplies a permission gateway and the provider's native callbacks.
- Product code observes permission, enablement, destination and event flows.
- Product code consumes opened events through the exactly-once handoff.
- An authenticated host may use OneSignal identity; FCM identity is explicitly unsupported.

## Functional requirements

1. Expose permission as `NotDetermined`, `Denied`, `Granted`, `Provisional`, `Ephemeral`, or `Unavailable`.
2. Request permission through an injected platform boundary and return the observed result.
3. Enable or disable push explicitly. Disabled clients publish no destination.
4. Publish a typed destination: `FcmToken` or `OneSignalSubscriptionId`; values must be nonblank, bounded and redacted by `toString()`.
5. Expose neutral payload data with a stable logical event ID, optional provider message ID, and bounded string metadata.
6. Distinguish `ForegroundReceived` from `Opened`; an opened event records whether it came from cold start.
7. Deduplicate opened-event handoff with an atomic ledger. Duplicate claims return `Duplicate` and never emit a second open event.
8. Fence callbacks by identity generation. A callback from an older generation returns `StaleGeneration` and changes no public state.
9. Make identity a capability. OneSignal supports `login` and `logout`; FCM returns `Unsupported(IDENTITY)`.
10. Make unsupported operations explicit. No adapter may silently succeed.
11. Redact destinations, external IDs, provider message IDs and payload values from default string representations and SDK diagnostic messages.
12. Provide deterministic fakes for permission, destination, events, duplicates, unsupported capabilities, and identity changes.
13. Fail closed when `push-fcm` is selected without a configured default Firebase app; configuration failures expose bounded reason enums and no configuration values.
14. Keep provider configuration independent: a OneSignal-only host does not require `push-fcm`, a default Firebase app, `google-services.json`, or `GoogleService-Info.plist`.
15. Expose FCM consistently as `FcmClientFactory`, `FcmAndroidGateway` and `FcmIosGateway`; vendor SDK class names remain implementation details.
16. Provide a credential-free reference sample that exercises both provider clients, collects shared events and clearly labels native callbacks as simulated.

## Acceptance scenarios

### AC-01 Typed destination

Given an enabled FCM adapter at generation 4, when a nonblank registration token callback for generation 4 arrives, then destination state becomes `Available(FcmToken)` and string rendering does not contain the token.

Given an enabled OneSignal adapter at generation 4 with confirmed identity, when a subscription callback for generation 4 arrives, then destination state becomes `Available(OneSignalSubscriptionId)`.

### AC-02 Permission and enablement

Given permission is not determined, when permission is requested, then the adapter exposes the exact platform result.

Given an available destination, when the client is disabled, then provider opt-out/auto-init behavior is invoked and destination state becomes `Disabled`.

### AC-03 Foreground and opened events

Given a valid semantic payload, when the foreground callback is observed, then one `ForegroundReceived` event is emitted and no open is inferred.

Given a valid cold-start open callback, when it is offered twice with the same event ID and generation, then the first result is `Accepted(coldStart=true)` and the second is `Duplicate`.

### AC-04 Identity/provider fencing

Given generation 8 is current, when a destination or event callback tagged generation 7 arrives, then the result is `StaleGeneration`, no destination/event changes, and no sensitive value is logged.

Given FCM is selected, when product code requests identity login, then it receives `Unsupported(IDENTITY)`.

### AC-05 Failure and compatibility behavior

Blank, oversized or structurally invalid destinations/payloads are rejected as `Malformed`.

An unknown semantic payload version is rejected as `Incompatible`; it is never converted to a successful event.

When a durable ledger fails, the opened event returns `StorageFailure` and is not emitted; the SDK does not downgrade to process-only deduplication silently.

### AC-06 Provider configuration independence

Given the FCM Android adapter is selected and Firebase's default app is not configured, when the host creates the gateway, then creation returns `Unavailable(DEFAULT_FIREBASE_APP_NOT_CONFIGURED)` and does not call Firebase Messaging.

Given the FCM iOS adapter is selected, when the host reports that `FirebaseApp.app()` is absent after configuration, then `FcmIosGateway.create` returns `Unavailable(DEFAULT_FIREBASE_APP_NOT_CONFIGURED)`.

Given either Android or iOS produces a ready FCM gateway, when it is passed to `FcmClientFactory.create`, then the result exposes the same `FcmPushClient` contract and callback methods.

Given only the OneSignal adapter is selected, then no default Firebase app or Firebase configuration file is required by the KMP adapter. Android delivery may still use FCM internally through OneSignal; iOS delivery uses APNs.

### AC-07 Reference sample

Given no provider credentials, when the sample runs for FCM or OneSignal, then it uses the corresponding real provider client and demonstrates permission, enablement, destination registration, foreground receipt, cold-start open and duplicate-open handling.

The sample collects the public event flow and redacts destination values. It identifies provider callbacks as simulated and never presents the run as notification delivery evidence.

## Non-goals

Sending/backend APIs, credentials, permission UI/Compose, local notifications, campaigns, analytics, topics, rich media/actions, in-app messages, scheduling, web/desktop runtime support, and two active providers in one build.

## Supported targets

- Kotlin common code: Android, iOS arm64, iOS simulator arm64, plus JVM for fast tests/sample.
- Android provider bindings: Firebase Messaging and OneSignal native SDKs.
- iOS FCM gateway: Swift owns Firebase through SPM/CocoaPods, implements the narrow `FcmIosHostApi` control, and forwards callbacks to the shared client.
- iOS OneSignal bridge: Swift host integrates the native SDK and forwards typed callbacks; the Maven library has no CocoaPods/SPM ownership.
- Physical push delivery requires configured apps, APNs/FCM credentials and real devices and is outside local automated evidence.

## Safety, privacy, authorization and compatibility

- Payloads are untrusted hints, never authoritative application state.
- The SDK performs no authorization and owns no credentials.
- Firebase configuration files and OneSignal/APNs/FCM credentials remain owned by the consuming application or provider dashboard; they are never packaged in this repository.
- Sensitive destination/identity/payload data is never included in `toString()` or SDK diagnostics.
- Contract version 1 rejects unknown versions and malformed bounds.
- Exactly-once means atomic handoff within the caller-supplied ledger scope; cross-install/provider delivery cannot be guaranteed.

## Evidence required

- Common contracts, duplicates, cold start, stale generation and redaction: automated tests.
- Android provider source: target compilation plus focused gateway tests where SDK APIs permit.
- iOS gateway/bridge source: Kotlin/Native tests and target compilation; real notification runtime remains a physical-device gate.
- Maven consumption: publish to a temporary local repository and compile a consumer/sample.
- Reference sample: automated tests for both providers plus a successful deterministic run.
