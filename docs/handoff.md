# Delivery handoff

Outcome: Independent KMP push client 0.1.0 uploaded to a private GitHub pull request and ready for architecture review.

Scope: `push-core`, `push-fcm`, `push-onesignal`, `push-test`, a credential-free sample, Apache-2.0 license, Gradle wrapper, Maven publications, specification/RFC/plan/tasks, provider setup guidance, CI, release-gated GitHub Packages delivery and evidence report.

Artifacts: Source tree in this directory; private repository `SOSMex/kmp-push-client`; PR #1; generated local Maven repository under `build/test-maven` (ignored by Git).

Validation: 12 JVM tests plus the same 12 Kotlin/Native simulator tests; Android FCM/OneSignal compilation; iOS arm64 and simulator compilation; 20 local Maven publications; sample and external Maven consumer execution; release-version and GitHub Packages task discovery.

Decisions: One provider per consumer build; provider-specific typed destinations; optional real OneSignal identity and explicit unsupported FCM identity; atomic caller-supplied ledger; monotonic identity generation; host-managed Apple SDK dependencies; no inferred provider/OS/open state.

Residual risk: Hosted CI has not completed yet. There is no durable production ledger implementation, configured native host application, physical-device push flow, Android runtime observation or published package version. Maven Central signing/publication and public coordinates remain undecided. Dual-provider dependency exclusion is documented but not mechanically enforced.

Next action: Obtain green hosted CI on PR #1, complete architecture review of the public API, and decide durable ledger guidance and Apple dependency ownership. A package release still requires an owner-created semantic GitHub Release; physical Android/iPhone cold-start/open verification remains open.
