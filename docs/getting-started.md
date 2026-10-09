# Getting started

The source version is 0.1.0. There is no downloadable package release yet.
Choose a composite build for source integration, or the local Maven repository
to check artifact consumption. Neither path requires a GitHub token.

## Run before configuring a provider

Install JDK 17 and Android SDK platform 36. Set `JAVA_HOME` to your JDK and
`ANDROID_HOME` to your SDK (or set `sdk.dir` in an ignored `local.properties`).
Even the JVM sample configures the Android library projects.

```bash
git clone https://github.com/SOSMex/kmp-push-client.git
cd kmp-push-client
./gradlew :sample:run
```

The sample exercises both real client APIs with simulated native callbacks.
It demonstrates permission, enablement, registration, foreground receipt,
cold-start opening and duplicate rejection. It does not contact FCM, APNs or
OneSignal. Filter with `--args='--provider=fcm'` or `--args='--provider=onesignal'`.

## Use the source in your app

Keep the SDK checkout next to your app checkout. In your app's
`settings.gradle.kts`, add:

```kotlin
includeBuild("../kmp-push-client") {
    dependencySubstitution {
        substitute(module("io.github.sosmex.push:push-core")).using(project(":push-core"))
        substitute(module("io.github.sosmex.push:push-fcm")).using(project(":push-fcm"))
        substitute(module("io.github.sosmex.push:push-onesignal")).using(project(":push-onesignal"))
        substitute(module("io.github.sosmex.push:push-test")).using(project(":push-test"))
    }
}
```

In your shared module, choose **one** provider:

```kotlin
kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation("io.github.sosmex.push:push-core:0.1.0")
            implementation("io.github.sosmex.push:push-fcm:0.1.0")
            // For OneSignal, replace push-fcm with push-onesignal.
        }
        commonTest.dependencies {
            implementation("io.github.sosmex.push:push-test:0.1.0")
        }
    }
}
```

Use the repository's pinned Kotlin/AGP versions as the initial compatibility
baseline; arbitrary consumer toolchain combinations are not verified. Record
the checkout commit when collecting integration evidence. The substitutions
make the version above resolve to that checkout, not to a registry release.

## Consume local Maven artifacts

For the complete Android/iOS publication set, use macOS with Xcode installed.
From the SDK root:

```bash
./gradlew \
  :push-core:publishAllPublicationsToTestRepository \
  :push-fcm:publishAllPublicationsToTestRepository \
  :push-onesignal:publishAllPublicationsToTestRepository \
  :push-test:publishAllPublicationsToTestRepository
```

This writes `build/test-maven`, **not** `~/.m2/repository`. In your app's
`dependencyResolutionManagement.repositories`, add the checkout's repository:

```kotlin
maven {
    url = uri("../kmp-push-client/build/test-maven")
    content { includeGroup("io.github.sosmex.push") }
}
google()
mavenCentral()
```

Keep the module dependencies shown above, and remove `includeBuild` when testing
Maven resolution so source substitution cannot hide a packaging problem.

Run the included independent consumer from the SDK root:

```bash
./gradlew -p integration-tests/maven-consumer run
```

Expected final application line:

```text
maven_consumer=accepted (simulated callbacks; no device delivery)
```

This separate JVM build resolves all four modules from `build/test-maven` and
checks cold-start acceptance, duplicate rejection and a stale token callback.
It has no project dependencies or composite substitution. CI runs it after
building the publications. It verifies JVM artifact consumption, not Android/iOS
host integration. If publishing with `-PreleaseVersion=...`, pass the same version
to the consumer with `-PsdkVersion=...`.

CI also compiles an independent KMP consumer once for FCM and once for OneSignal:

```bash
./gradlew -p integration-tests/kmp-maven-consumer \
  clean compileAndroidMain \
  linkDebugFrameworkIosSimulatorArm64 linkDebugFrameworkIosArm64 \
  -PpushProvider=fcm

./gradlew -p integration-tests/kmp-maven-consumer \
  clean compileAndroidMain \
  linkDebugFrameworkIosSimulatorArm64 linkDebugFrameworkIosArm64 \
  -PpushProvider=onesignal
```

Each run selects one provider in `commonMain`, exercises that provider's native
gateway symbols, and links final Apple framework binaries from Maven artifacts.
The build has no project dependencies or composite substitution. The default
`sdkRepository=local` mode exclusively resolves `io.github.sosmex.push` from
`build/test-maven`.

After a version has been observed on Maven Central, repeat each command with
`-PsdkRepository=central -PsdkVersion=<version>`. In that mode the SDK group is
resolved exclusively from Maven Central and the local publication repository is
not configured. See the [Central release runbook](central-release.md).

## Connect native callbacks

The host owns permission UI, native SDK configuration, callback lifecycle and
a durable implementation of `AtomicEventLedger`. The in-memory ledger from
`push-test` is for tests and examples; it cannot retain deduplication across
process restarts.

| Provider | Android host | iOS host |
| --- | --- | --- |
| FCM | Configure Firebase, create `FcmAndroidGateway`, forward `FirebaseMessagingService.onNewToken`, foreground data and activity notification-open callbacks. | Configure Firebase in Swift, implement `FcmIosHostApi`, create `FcmIosGateway`, forward Messaging token and notification delegate callbacks. |
| OneSignal | Initialize `OneSignalAndroidGateway`; install subscription, foreground and click observers. | Initialize native OneSignal; supply `OneSignalGateway` and forward callbacks through `OneSignalIosCallbackBridge`. |

All forwarded callbacks carry the generation of the session that registered
them. Capture it when binding that session; never look up the new generation
inside delayed work from an old session. For FCM, `advanceGeneration()` marks
a host identity/session boundary. OneSignal login/logout advances the generation
and passes it to the host gateway. Replace old observers at that boundary and
retain the old generation on work already queued. Provider callbacks still need
to be associated with the correct session by the host; a current-generation
lookup alone cannot establish that association.

`observeToken(...)` / `observeSubscriptionId(...)` are input methods, not
listeners. Collect `client.events` for foreground/open events, and observe
`client.destination` for registration state. Do not print raw tokens or payloads.

## Before calling your integration ready

On configured physical Android and iPhone devices, verify registration,
foreground receipt, background and cold-start opens, denied permission,
duplicate opens and a session change with delayed callbacks. Registration alone
does not prove delivery. Keep those results separate from the simulated sample
and compiler/test results. The repository does not yet provide a complete native
host sample or claim verified physical-device delivery.
