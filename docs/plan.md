# Implementation plan

## Outcome advanced

Implement AC-01 through AC-07 from `spec.md` as the smallest compileable provider-honest SDK slice and a useful credential-free reference flow.

## Existing project shape

The delivery directory started empty. The reference product is not modified. A new Gradle Kotlin Multiplatform build will declare Android, iOS arm64, iOS simulator arm64 and JVM targets.

## Vertical slice

A fake/native gateway emits permission, enablement, typed destination, foreground and open observations into shared provider clients. Shared handoff atomically deduplicates opens and rejects stale generations. Android gateways bind current vendor SDKs. FCM on iOS uses a named gateway over a narrow Swift host API; OneSignal retains its host-called callback bridge.

## Ownership matrix

| Behavior | Module/source set | Boundary rationale |
| --- | --- | --- |
| Permission/destination/event models | `push-core/commonMain` | Deterministic and provider-neutral |
| Generation fence and open handoff | `push-core/commonMain` | Same semantics on every target |
| FCM orchestration | `push-fcm/commonMain` | Provider-specific, platform-neutral |
| FCM configuration result | `push-fcm/commonMain` | Shared fail-closed outcome and redacted reason codes |
| Android Firebase SDK calls | `push-fcm/androidMain` | Android vendor API hidden behind `FcmAndroidGateway` |
| iOS Firebase SDK control/configuration assertion | `push-fcm/iosMain` | `FcmIosGateway` delegates to host-owned Firebase through `FcmIosHostApi` |
| FCM client construction | `push-fcm/commonMain` | One `FcmClientFactory` maps either gateway result to the same client contract |
| OneSignal orchestration and identity | `push-onesignal/commonMain` | Provider-specific capability |
| OneSignal SDK calls | `push-onesignal/androidMain` | Android vendor API |
| OneSignal callback forwarding | `push-onesignal/iosMain` | iOS host/native boundary |
| Fakes | `push-test/commonMain` | Consumer-test support |
| Credential-free reference flow | `sample` JVM application | Exercises real provider clients while keeping native callbacks explicitly simulated |

## Contract and state changes

- Semantic payload contract version is `1`; unknown versions are incompatible.
- Stable deduplication key is `eventId` scoped to provider and identity generation.
- Destination callbacks must match the current generation.
- Disabling or advancing generation clears current destination.
- Open handoff emits only after atomic ledger claim succeeds.
- Unknown/malformed data never maps to success.
- Missing Firebase configuration maps to an explicit unavailable result before an FCM binding is used.
- OneSignal configuration and initialization do not traverse the FCM adapter or require a default Firebase app.

## Ordered delivery

1. Create Gradle build, license and publication conventions.
2. Implement/test core models, validation, redaction, fence and handoff.
3. Implement provider clients and explicit capability differences.
4. Add Android native gateways and iOS host boundaries.
5. Add fakes and sample.
6. Run discovered build/test/publication checks and write verification report.
7. Document provider-specific configuration and verify fail-closed FCM initialization without regressing OneSignal-only builds.
8. Add cross-platform pull-request CI and release-gated GitHub Packages delivery without granting provider or Maven Central credentials.
9. Replace the one-off fake sample with a provider-selectable reference flow and test both paths in CI.

## Verification matrix

| Acceptance scenario | Target/environment | Evidence required |
| --- | --- | --- |
| AC-01 to AC-05 deterministic behavior | JVM common tests | Automated-tested |
| Android Firebase/OneSignal bindings | Android compile | Compiled |
| Missing/default Firebase configuration outcome | JVM/common tests plus Android compile | Automated-tested/compiled |
| OneSignal-only dependency path | Gradle dependency inspection plus Android/iOS compile | No direct `push-fcm` dependency or Firebase app configuration prerequisite |
| iOS FCM gateway and OneSignal callback bridge | iOS simulator tests plus arm64 compilation | Automated-tested/compiled |
| Maven consumability | Temporary Maven repository + sample | Automated-tested |
| FCM and OneSignal sample paths | JVM sample tests and deterministic run | Automated-tested; native callbacks remain simulated |
| Real permission/delivery/open | Physical Android and iPhone | Physically-tested, remains open locally |
| CI workflow | GitHub-hosted Ubuntu and macOS runners | Required pull-request checks after workflow publication |
| GitHub Packages delivery | Published semantic GitHub Release on a `main` commit | Registry publication; no Maven Central/public-release claim |

## Open decisions

- Owner approval for public group/artifact coordinates.
- Whether a future version should own CocoaPods/SPM dependencies or keep host-managed native SDKs.
- Which durable ledger implementation should be recommended for production consumers.
