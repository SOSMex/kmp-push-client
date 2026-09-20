# Verification report

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
| Public availability | Maven Central or other remote | No distribution evidence | No remote publication attempted | Open by explicit scope | Repository/release owner |

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

The local 0.1.0 source is reviewable. Shared/provider rules are automated-tested on JVM and iOS simulator, Android native gateways and iOS bridges compile, Maven publications are generated locally, and an external JVM consumer resolves them. Real provider integration, physical-device delivery/open behavior, durable production ledger integration, remote publication and public release remain unproven.
