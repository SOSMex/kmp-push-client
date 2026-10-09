# Maven Central release runbook

## Current state

The approved public coordinates are:

```text
io.github.sosmex.push:push-core:<version>
io.github.sosmex.push:push-fcm:<version>
io.github.sosmex.push:push-onesignal:<version>
io.github.sosmex.push:push-test:<version>
```

The build and manual release workflow are configured, but no Maven Central
publication has been performed. The Central Portal account exists and namespace
verification for `io.github.sosmex` is in progress. Do not present `0.1.0` or
any later version as available until Central resolves it and the remote-only
consumer checks below pass.

The `sample` module is never published. GitHub Packages remains an optional,
separate private registry path.

## One-time Central Portal onboarding

Complete these steps in [Central Portal](https://central.sonatype.com/):

1. Verify the `io.github.sosmex` namespace for the `SOSMex` GitHub organization.
2. Generate a Central Portal user token. Its generated username and password
   are publishing credentials; the normal account password is not accepted.
3. Create a password-protected GPG signing key and distribute its public key to
   a supported key server.
4. Add these GitHub Actions repository secrets:

   | Secret | Value |
   | --- | --- |
   | `MAVEN_CENTRAL_USERNAME` | Central user-token username |
   | `MAVEN_CENTRAL_PASSWORD` | Central user-token password |
   | `SIGNING_KEY` | Complete ASCII-armored private key |
   | `SIGNING_PASSWORD` | Private-key password |

The workflow maps those four secrets to the Gradle plugin's
`ORG_GRADLE_PROJECT_mavenCentralUsername`,
`ORG_GRADLE_PROJECT_mavenCentralPassword`,
`ORG_GRADLE_PROJECT_signingInMemoryKey`, and
`ORG_GRADLE_PROJECT_signingInMemoryKeyPassword` variables only for the upload
step. It does not print or persist their values.

The project pins `com.vanniktech.maven.publish` `0.37.0`. Central configuration
and signing are disabled unless `-PcentralRelease=true` is explicitly present,
so ordinary builds and local Maven verification need no publishing credentials.

## Required release gates

Before dispatching `.github/workflows/publish-maven-central.yml`:

- the version is still unused on Maven Central;
- the release commit is merged to `main` and CI is green;
- an existing semantic tag such as `v0.1.0` identifies that exact commit;
- namespace verification, user-token credentials and the signing key are complete;
- all four modules are intended to carry the same version;
- no provider credential or consuming-app configuration file is present in the repository.

The workflow rechecks the tag and `main` ancestry and then runs:

- JVM tests, iOS simulator tests, Android provider compilation and iOS arm64 compilation;
- the credential-free sample;
- unsigned local publication of all four modules;
- structural inspection of root metadata, Android AARs, Apple KLIBs, sources,
  documentation jars and Central POM fields;
- the independent JVM consumer;
- separate FCM and OneSignal KMP consumers that compile Android and link both
  simulator and device Apple frameworks.

The consumers use Maven coordinates only. They contain no project dependency or
composite-build substitution.

## Stage first

From the workflow's `main` branch version, dispatch **Stage Maven Central release**,
enter the existing non-snapshot tag, and leave
`publish` set to `false`. The workflow uploads signed artifacts through Central
Portal but leaves public release as a manual Portal action. Review the deployment
and its validation result in the Portal. A successful workflow in this mode is
upload evidence, not by itself a claim of Portal validation or public availability.

If the deployment is wrong, drop it in Central Portal. Fixes require a new commit
and tag/version; released Maven coordinates are immutable.

Set `publish` to `true` only when the owner intends the workflow to request
public release after validation. This is an explicit distribution action and is
not required for a staging rehearsal. This path asks the plugin to wait for the
Central deployment to reach `PUBLISHED`; repository search/download resolution
still needs the separate check below.

## Verify public availability

Central publication and repository availability are separate states. After the
Portal reports the deployment published, verify that each module/version resolves
without any local SDK repository:

```bash
./gradlew -p integration-tests/kmp-maven-consumer \
  clean compileAndroidMain \
  linkDebugFrameworkIosSimulatorArm64 linkDebugFrameworkIosArm64 \
  -PpushProvider=fcm \
  -PsdkRepository=central \
  -PsdkVersion=<version>

./gradlew -p integration-tests/kmp-maven-consumer \
  clean compileAndroidMain \
  linkDebugFrameworkIosSimulatorArm64 linkDebugFrameworkIosArm64 \
  -PpushProvider=onesignal \
  -PsdkRepository=central \
  -PsdkVersion=<version>
```

Resolve all four root modules, including `push-test`, through the independent JVM
consumer as well:

```bash
./gradlew -p integration-tests/maven-consumer run \
  -PsdkRepository=central \
  -PsdkVersion=<version>
```

Only then update README installation language and release notes to say the
version is downloadable.

## Consumer installation after verification

Maven Central needs no custom repository beyond the usual declarations:

```kotlin
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}
```

Choose one provider in shared code:

```kotlin
commonMain.dependencies {
    implementation("io.github.sosmex.push:push-core:<verified-version>")
    implementation("io.github.sosmex.push:push-fcm:<verified-version>")
    // Or replace push-fcm with push-onesignal.
}

commonTest.dependencies {
    implementation("io.github.sosmex.push:push-test:<verified-version>")
}
```

Artifact resolution proves packaging and compatibility with the checked
toolchain. It does not prove provider registration, delivery or notification
opens on physical devices; those remain host-app runtime gates.
