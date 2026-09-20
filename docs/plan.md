# Implementation plan

## Outcome advanced

Implement AC-01 through AC-05 from `spec.md` as the smallest compileable provider-honest SDK slice.

## Existing project shape

The delivery directory started empty. The reference product is not modified. A new Gradle Kotlin Multiplatform build will declare Android, iOS arm64, iOS simulator arm64 and JVM targets.

## Vertical slice

A fake/native gateway emits permission, enablement, typed destination, foreground and open observations into shared provider clients. Shared handoff atomically deduplicates opens and rejects stale generations. Android gateways bind current vendor SDKs; iOS bridges are host-called entry points.

## Ownership matrix

| Behavior | Module/source set | Boundary rationale |
| --- | --- | --- |
| Permission/destination/event models | `push-core/commonMain` | Deterministic and provider-neutral |
| Generation fence and open handoff | `push-core/commonMain` | Same semantics on every target |
| FCM orchestration | `push-fcm/commonMain` | Provider-specific, platform-neutral |
| Firebase SDK calls | `push-fcm/androidMain` | Android vendor API |
| FCM callback forwarding | `push-fcm/iosMain` | iOS host/native boundary |
| OneSignal orchestration and identity | `push-onesignal/commonMain` | Provider-specific capability |
| OneSignal SDK calls | `push-onesignal/androidMain` | Android vendor API |
| OneSignal callback forwarding | `push-onesignal/iosMain` | iOS host/native boundary |
| Fakes | `push-test/commonMain` | Consumer-test support |

## Contract and state changes

- Semantic payload contract version is `1`; unknown versions are incompatible.
- Stable deduplication key is `eventId` scoped to provider and identity generation.
- Destination callbacks must match the current generation.
- Disabling or advancing generation clears current destination.
- Open handoff emits only after atomic ledger claim succeeds.
- Unknown/malformed data never maps to success.

## Ordered delivery

1. Create Gradle build, license and publication conventions.
2. Implement/test core models, validation, redaction, fence and handoff.
3. Implement provider clients and explicit capability differences.
4. Add Android native gateways and iOS callback bridges.
5. Add fakes and sample.
6. Run discovered build/test/publication checks and write verification report.

## Verification matrix

| Acceptance scenario | Target/environment | Evidence required |
| --- | --- | --- |
| AC-01 to AC-05 deterministic behavior | JVM common tests | Automated-tested |
| Android Firebase/OneSignal bindings | Android compile | Compiled |
| iOS callback bridges | iOS arm64 and simulator compile | Compiled |
| Maven consumability | Temporary Maven repository + sample | Automated-tested |
| Real permission/delivery/open | Physical Android and iPhone | Physically-tested, remains open locally |

## Open decisions

- Owner approval for public group/artifact coordinates.
- Whether a future version should own CocoaPods/SPM dependencies or keep host-managed native SDKs.
- Which durable ledger implementation should be recommended for production consumers.

