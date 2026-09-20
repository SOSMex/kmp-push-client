# Delivery tasks

## Foundation

- [x] Add Gradle KMP modules, Apache-2.0 license and Maven publications.
- [x] Add pure models, bounded payload parsing, redaction and focused tests.
- [x] Add generation fencing and atomic opened-event handoff tests.

## Provider adapters

- [x] Implement FCM orchestration with explicit unsupported identity.
- [x] Implement OneSignal orchestration with login/logout capability.
- [x] Add Android vendor gateways and iOS host callback bridges.
- [x] Normalize the FCM API as `FcmClientFactory`, `FcmAndroidGateway` and `FcmIosGateway`.
- [x] Replace the callback-only FCM iOS bridge with a host-backed gateway and shared client callbacks.

## Test support and sample

- [x] Implement fake client, gateways and atomic in-memory ledger.
- [x] Add a credential-free sample demonstrating cold-start duplicate handling and fencing.
- [x] Expand the sample to exercise both real provider clients, destination state and the shared event listener.
- [x] Test both sample provider paths and keep native callbacks explicitly simulated.

## Verification

- [x] Run JVM/common tests.
- [x] Compile Android and iOS targets.
- [x] Publish to a temporary Maven repository and compile/run the sample.
- [x] Record physical-device and distribution evidence as unavailable, not inferred.

## Provider configuration follow-up

- [x] Document FCM versus OneSignal configuration ownership on Android and iOS.
- [x] Return an explicit unavailable result when FCM's default Firebase app is absent.
- [x] Keep the OneSignal-only adapter independent from `push-fcm` and Firebase app configuration files.
- [ ] Verify both adapters in configured physical-device host applications.

## CI/CD

- [x] Add pull-request and `main` CI for JVM, Android and iOS.
- [x] Preserve test reports and complete local Maven publications as bounded CI artifacts.
- [x] Add release-gated publication to the private GitHub Packages Maven registry.
- [x] Derive the Maven version from a validated semantic GitHub Release tag.
- [ ] Publish a GitHub Release and verify the resulting packages; release creation remains owner-controlled.
