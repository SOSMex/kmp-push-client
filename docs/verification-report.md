# Verification report

## Symmetric FCM API refactor — 2026-09-19

The FCM public surface now uses one provider vocabulary and one client-construction path. `FirebaseMessagingAndroidGateway` and `FcmIosCallbackBridge` were replaced by `FcmAndroidGateway`, `FcmIosGateway`, `FcmIosHostApi` and `FcmClientFactory`.

| Claim | Target/environment | Evidence level | Procedure | Result | Residual gate |
| --- | --- | --- | --- | --- | --- |
| Either initialized platform gateway creates the same FCM client contract | JVM and iOS simulator arm64 | `Automated-tested` | `FcmInitializationTest` through `:push-fcm:jvmTest` and `:push-fcm:iosSimulatorArm64Test` | Factory success and unavailable propagation passed | Configured host runtime |
| Missing Firebase configuration fails closed on iOS | iOS simulator arm64 | `Automated-tested` | `FcmIosGatewayTest.missingFirebaseAppReturnsUnavailable` | Passed | Configured and unconfigured Swift host application |
| iOS enablement reaches the host-owned Firebase control | iOS simulator arm64 | `Automated-tested` | `FcmIosGatewayTest.configuredGatewayControlsNativeAutoInit` | `true` and `false` calls observed in order | Real Firebase Messaging SDK and physical iPhone |
| New Android FCM gateway compiles against Firebase Messaging | Android SDK 36 | `Compiled` | `:push-fcm:compileAndroidMain` | Passed | Configured Android runtime |
| New iOS FCM gateway compiles for a device target | iOS arm64 | `Compiled` | `:push-fcm:compileKotlinIosArm64` | Passed | Swift export consumption and physical-device flow |
| Updated `push-fcm` publication is consumable | Local Maven repository and external JVM Gradle project | `Automated-tested` | Four local publication tasks, then `work/maven-consumer` with `--refresh-dependencies` | `FcmClientFactory` resolved and `maven_consumer=accepted` | Android/iOS external host projects |

The same full 271-task matrix used by CI passed locally, including all JVM tests, all iOS simulator tests, Android bindings, iOS arm64 compilation, the sample and all local Maven publications. This proves shared behavior, the iOS host seam, compilation and publication shape. It does not prove Firebase registration, delivery, foreground receipt or notification opens on a configured physical Android device or iPhone.

## Provider configuration follow-up — 2026-09-19

The FCM adapter now exposes a fail-closed initialization result and the provider setup documentation explicitly covers OneSignal-only consumers.

| Claim | Target/environment | Evidence level | Procedure | Result | Residual gate |
| --- | --- | --- | --- | --- | --- |
| Missing default Firebase app does not invoke the FCM initializer | JVM and iOS simulator arm64 | `Automated-tested` | `FcmInitializationTest` through `:push-fcm:jvmTest` and `:push-fcm:iosSimulatorArm64Test` | 3 new tests per target, 0 failures | Configured and unconfigured host-app runtime |
| FCM binding still compiles after guarded creation | Android SDK 36 and iOS arm64 | `Compiled` | `:push-fcm:compileAndroidMain`, `:push-fcm:compileKotlinIosArm64` | Passed | Swift export consumption and physical-device flow |
| OneSignal adapter remains independent from `push-fcm` | Gradle Android compile classpath | `Inspected` | `:push-onesignal:dependencies --configuration androidCompileClasspath` | Contains `OneSignal`, transitive `firebase-messaging`, and `push-core`; no `push-fcm` project dependency | Configured OneSignal host runtime |
| OneSignal adapter still compiles/tests without Firebase app configuration files | JVM, Android SDK 36 and iOS arm64 | `Automated-tested`/`Compiled` | `:push-onesignal:jvmTest`, `:push-onesignal:compileAndroidMain`, `:push-onesignal:compileKotlinIosArm64` | Passed | Real provider registration/delivery/open |

The OneSignal dependency graph containing Firebase Messaging on Android is expected: FCM is OneSignal's Google-device transport. It does not mean that a OneSignal-only consuming app must initialize Firebase's default app or package Firebase configuration files. The pinned OneSignal 5.9.8 source initializes a separate named Firebase app using its provider configuration.

## CI/CD follow-up — 2026-09-19

The workflow definitions and GitHub Packages Gradle repository are locally validated. The complete existing 271-task matrix remains the implementation baseline. Hosted CI run [35488718285](https://github.com/SOSMex/kmp-push-client/actions/runs/35488718285) then passed both jobs on the pull-request branch.

| Claim | Evidence level | Procedure | Result | Residual gate |
| --- | --- | --- | --- | --- |
| Release version overrides the default Maven version | `Automated-tested` | `:push-core:properties -PreleaseVersion=1.2.3` | Reported `version: 1.2.3` | Verify generated POMs in hosted release run |
| GitHub Packages tasks exist for all published modules | `Automated-inspected` | Gradle task discovery with repository credentials present | Four `publishAllPublicationsToGithubPackagesRepository` tasks discovered | No package upload attempted locally |
| Pull-request CI | `Hosted-tested` | GitHub Actions run `35488718285` | JVM/Android passed in 2m09s; iOS tests, device compilation, complete Maven publications and artifact upload passed in 6m04s | Re-run required for future revisions |
| Release-gated CD definition | `Source-reviewed` | `.github/workflows/publish-github-packages.yml` | Semantic tag, `main` ancestry, full matrix and scoped package publication defined | Requires an owner-published GitHub Release |

The first hosted attempt failed before compilation because `android-actions/setup-android@v3` requested the removed SDK package `tools`. The workflow now validates the Android SDK 36 already present on the GitHub runner images; the succeeding run proves that path on both Ubuntu and Apple Silicon macOS.

- Revision: `f237d38d5f4d240cf869f0a88d9e883c2ec92cdd` (implementation baseline; this report follows in a documentation commit)
- Working tree at baseline: clean
- Date: 2026-09-19, America/Mexico_City
- Host: Apple silicon macOS, Xcode 26.3, JBR 17.0.9, Android SDK 36

## Scope

Verify the local 0.1.0 vertical slice: provider-honest types, permission/enablement boundaries, FCM versus OneSignal capability differences, semantic payload validation, foreground/open separation, cold-start deduplication, concurrent exactly-once handoff within ledger scope, stale-generation fencing, redaction, Android native gateway compilation, iOS callback bridge compilation, fake usability and Maven consumability.

## Evidence matrix

| Claim | Target/environment | Evidence level | Procedure or artifact | Result | Residual gate and owner |
| --- | --- | --- | --- | --- | --- |
| Payload validation, redaction, cold start, duplicates, 32-way concurrent claim and stale generation | JVM/JBR 17 | `Automated-tested` | `:push-core:jvmTest` | 5 tests, 0 failures | Durable production ledger integration; consuming app owner |
| FCM typed token and explicit unsupported identity | JVM/JBR 17 | `Automated-tested` | `:push-fcm:jvmTest` | 1 test, 0 failures | Native runtime callback observation; platform owner |
| OneSignal identity fence and confirmed-subscription gate | JVM/JBR 17 | `Automated-tested` | `:push-onesignal:jvmTest` | 2 tests, 0 failures | Provider identity observer on configured app; platform owner |
| Permission, duplicate, storage failure and stale callback fake | JVM/JBR 17 | `Automated-tested` | `:push-test:jvmTest` | 1 test, 0 failures | None for covered fake behavior |
| Same common/provider behavior under Kotlin/Native | iOS simulator arm64 test executable | `Automated-tested` | Four `iosSimulatorArm64Test` tasks | 9 tests, 0 failures | Native Firebase/OneSignal host callbacks not exercised |
| Firebase Android binding | Android SDK 36 | `Compiled` | `:push-fcm:compileAndroidMain` | Passed against Firebase BoM 34.18.0 | Configured app, permission, token, foreground and open runtime |
| OneSignal Android binding | Android SDK 36 | `Compiled` | `:push-onesignal:compileAndroidMain` | Passed against OneSignal 5.9.8 | Configured app, identity/subscription, foreground and click runtime |
| FCM iOS bridge | iOS arm64 and simulator arm64 | `Compiled` | `:push-fcm:compileKotlinIosArm64`, `:push-fcm:compileKotlinIosSimulatorArm64` | Passed | Swift host + Firebase SDK + APNs physical-device flow |
| OneSignal iOS bridge | iOS arm64 and simulator arm64 | `Compiled` | `:push-onesignal:compileKotlinIosArm64`, `:push-onesignal:compileKotlinIosSimulatorArm64` | Passed | Swift host + OneSignalFramework physical-device flow |
| Credential-free sample | JVM/JBR 17 | `Automated-tested` | `:sample:run` | Printed `Accepted`, `Duplicate`, `StaleGeneration` | Sample is not provider runtime evidence |
| Maven publication shape | Local test Maven repository | `Automated-tested` | Four `publishAllPublicationsToTestRepository` tasks | 20 target/root publications generated | Signing, Central metadata and owner-approved coordinates |
| Maven consumer resolution | External JVM Gradle project | `Automated-tested` | `work/maven-consumer` with `--refresh-dependencies` | `maven_consumer=accepted` | Android/iOS external consumer projects remain desirable |
| Real push registration/delivery/open | Physical Android + iPhone | No runtime evidence | Not available without provider projects, credentials and devices | Open | App/platform owners |
| Remote source availability | Private GitHub repository | `Uploaded` | `SOSMex/kmp-push-client`, PR #1 | Source and PR uploaded; no package version published | Merge/release owner |
| Public package availability | Maven Central or public registry | No distribution evidence | No public package publication attempted | Open by explicit scope | Repository/release owner |

## Commands and workflows

The final complete automated matrix was:

```bash
ANDROID_HOME=/Users/oscar.perez/Library/Android/sdk ./gradlew \
  :push-core:jvmTest :push-fcm:jvmTest :push-onesignal:jvmTest :push-test:jvmTest \
  :push-core:iosSimulatorArm64Test :push-fcm:iosSimulatorArm64Test \
  :push-onesignal:iosSimulatorArm64Test :push-test:iosSimulatorArm64Test \
  :push-fcm:compileAndroidMain :push-onesignal:compileAndroidMain \
  :push-fcm:compileKotlinIosArm64 :push-onesignal:compileKotlinIosArm64 \
  :sample:run \
  :push-core:publishAllPublicationsToTestRepository \
  :push-fcm:publishAllPublicationsToTestRepository \
  :push-onesignal:publishAllPublicationsToTestRepository \
  :push-test:publishAllPublicationsToTestRepository
```

Result: `BUILD SUCCESSFUL`, 271 actionable tasks.

External Maven consumer:

```bash
outputs/kmp-push-client-0.1.0/gradlew -p work/maven-consumer run --refresh-dependencies
```

Result: `maven_consumer=accepted`.

## Degraded and regression coverage

- Unknown payload version fails as `Incompatible`.
- Missing/invalid fields fail as `Malformed`.
- Atomic ledger failure returns `StorageFailure`; no process-only fallback is invented.
- Duplicate cold-start opens return `Duplicate`.
- Concurrent duplicate offers produce exactly one accepted handoff within ledger scope.
- Callbacks from an older identity generation return `StaleGeneration`.
- Unconfirmed OneSignal identity publishes no subscription destination.
- FCM identity returns `Unsupported(IDENTITY)`.
- Destination and payload default strings redact sensitive values.
- OneSignal identity operations return `Invoked`, not provider-confirmed.

## Unavailable or contradictory evidence

- No Android emulator runtime was performed.
- No Swift host application was built or launched; only exported Kotlin bridge targets compiled.
- No physical Android/iPhone push delivery, cold start, notification open, APNs mapping or identity transition was observed.
- No provider acceptance, OS delivery or user-open state is inferred from destination registration.
- Separate provider modules encourage one provider per build, but 0.1.0 does not ship a Gradle enforcement plugin preventing a consumer from adding both.
- Android SDK tooling emitted an XML-version warning from mixed command-line-tools generations during the first configured build; compilation still succeeded.

## Narrowest truthful readiness statement

The 0.1.0 source is reviewable in private PR #1. Shared/provider rules are automated-tested on JVM and iOS simulator, Android native gateways and iOS bridges compile, Maven publications are generated locally and in green hosted CI, and an external JVM consumer resolves them. Real provider integration, physical-device delivery/open behavior, durable production ledger integration, package publication and public release remain separate gates.
