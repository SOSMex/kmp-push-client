# RFC 0001: provider-honest push boundary

- Status: Accepted constraints; implementation proposal local to 0.1.0
- Owners: SDK product owner
- Reviewers: Mobile architecture owner, Android owner, iOS owner

## Problem

Applications need shared push behavior without pretending that FCM tokens and OneSignal subscriptions/identity are equivalent. The boundary also has to survive cold-start callbacks, duplicates and identity changes without leaking identifiers.

## Constraints

- Android and iOS, one provider per build.
- FCM and OneSignal only.
- Maven-publicable, Apache-2.0.
- No backend sending, credentials, campaign or analytics surface.
- Native SDK callbacks are observations, not proof of provider acceptance, OS delivery or user open beyond the observed callback.
- No silent no-ops for unsupported provider features.

## Proposal

Use four public libraries and one sample:

- `push-core`: neutral models, state, event handoff, ledger ports and redaction.
- `push-fcm`: FCM-specific orchestration and Android Firebase binding; explicit iOS host bridge.
- `push-onesignal`: OneSignal orchestration, optional identity capability and Android binding; explicit iOS host bridge.
- `push-test`: deterministic fake client, gateways and ledger.
- `sample`: JVM demonstration that compiles without provider credentials.

The application selects a provider by depending on only one adapter. `ProviderKind`, destination subclasses and `CapabilityResult.Unsupported` preserve non-symmetry. A monotonically increasing identity generation fences stale callbacks. `OpenedEventHandoff` claims an event ID in an injected atomic ledger before publishing it.

Provider configuration follows the same boundary. `push-fcm` validates that Firebase's default app is configured before exposing its binding. `push-onesignal` neither depends on `push-fcm` nor requires Firebase app configuration files: OneSignal owns its Android FCM transport bootstrap from provider-side configuration, while its iOS SDK uses APNs.

## Alternatives considered

- Depend on KMPNotifier: rejected for 0.1.0 because it expands dependency/scope and reduces direct control over provider differences.
- One universal string token and provider no-ops: rejected because it erases semantics and permits unsafe false success.
- Bundle CocoaPods into the KMP publication: deferred; it couples consumer dependency management to this SDK and makes Maven-only adoption less predictable.
- Process-only deduplication: available only as a clearly named test/sample implementation, never a silent production fallback.

## Risks and mitigations

- Native SDK drift: pin versions, keep bindings thin and document a compatibility matrix.
- Cold-start duplicate: require an atomic durable ledger in production and test duplicate races.
- Stale authenticated identity: generation fence clears destination and rejects old callbacks.
- Sensitive logging: redacted model strings and diagnostics limited to enums/counts.
- iOS runtime gap: compile bridges locally, but keep real-device APNs/FCM/OneSignal evidence open.
- Missing host configuration: return a bounded unavailable reason before FCM use; never attach configuration values or credentials to the result.

## Verification

Run common/JVM tests, Android compilation for both adapters, Kotlin/Native compilation for both iOS targets, Maven local publication and sample execution. Real delivery/open evidence requires configured physical Android and iPhone apps.

## Decision

The provider/target/scope constraints were supplied as decided input on 2026-09-19. This RFC records the local 0.1.0 proposal. It does not claim organizational approval or a published artifact.
