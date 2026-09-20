# Delivery handoff

Outcome: Local, independent KMP push client 0.1.0 ready for architecture review.

Scope: `push-core`, `push-fcm`, `push-onesignal`, `push-test`, a credential-free sample, Apache-2.0 license, Gradle wrapper, Maven publications, specification/RFC/plan/tasks, provider setup guidance and evidence report.

Artifacts: Source tree in this directory; local Git baseline `f237d38d5f4d240cf869f0a88d9e883c2ec92cdd`; generated local Maven repository under `build/test-maven` (ignored by Git).

Validation: 9 JVM tests plus the same 9 Kotlin/Native simulator tests; Android FCM/OneSignal compilation; iOS arm64 and simulator compilation; 20 local Maven publications; sample and external Maven consumer execution.

Decisions: One provider per consumer build; provider-specific typed destinations; optional real OneSignal identity and explicit unsupported FCM identity; atomic caller-supplied ledger; monotonic identity generation; host-managed Apple SDK dependencies; no inferred provider/OS/open state.

Residual risk: No durable production ledger implementation, configured native host application, physical-device push flow, Android runtime observation, remote signing/publication or owner-approved coordinates. Dual-provider dependency exclusion is documented but not mechanically enforced.

Next action: Architecture review of the public API and owner decision on durable ledger guidance and Apple dependency ownership, followed by two configured sample apps and physical Android/iPhone cold-start/open verification.

