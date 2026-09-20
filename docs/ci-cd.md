# CI/CD contract

## Continuous integration

`.github/workflows/ci.yml` runs on pull requests to `main`, pushes to `main`, and manual dispatches. It grants read-only repository access.

The Ubuntu job verifies:

- all JVM tests;
- both Android provider bindings;
- the credential-free sample.

The Apple Silicon `macos-15` job verifies:

- all iOS simulator tests;
- all iOS arm64 device-target compilations;
- the complete local Maven publication shape for `push-core`, `push-fcm`, `push-onesignal`, and `push-test`.

Failed jobs retain test reports for seven days. Successful CI retains the generated local Maven repository for seven days as a review artifact. These artifacts are CI evidence, not a supported package registry or public release.

## Continuous delivery

`.github/workflows/publish-github-packages.yml` is triggered only by publishing a GitHub Release. The workflow:

1. checks out the exact release tag;
2. accepts semantic versions in the form `vMAJOR.MINOR.PATCH` with an optional suffix;
3. verifies that the tagged commit is an ancestor of `origin/main`;
4. reruns the JVM, Android and iOS release matrix;
5. derives the Maven version from the tag without its leading `v`;
6. publishes every KMP target publication to GitHub Packages.

Publishing an existing version fails instead of overwriting it. The workflow does not merge branches, create tags, create GitHub Releases, publish to Maven Central, or make the repository/package public.

## Release procedure

1. Merge an approved change to `main` and wait for CI to pass.
2. Create a tag such as `v0.1.0` on the exact validated `main` commit.
3. Create and publish a GitHub Release for that tag.
4. Wait for `Publish GitHub Packages` to complete.
5. Verify the expected module versions in the repository's Packages view before announcing availability.

Release creation and publication are distinct evidence states. A green CI run does not mean a package was published, and a GitHub Release does not mean the package workflow completed successfully.

## Private registry consumption

Consumers need GitHub Packages read access. Keep tokens outside source control:

```kotlin
repositories {
    maven {
        url = uri("https://maven.pkg.github.com/SOSMex/kmp-push-client")
        credentials {
            username = providers.gradleProperty("gpr.user").orNull
            password = providers.gradleProperty("gpr.key").orNull
        }
    }
}
```

Provide `gpr.user` and a token with package read permission through user-level Gradle properties or the consumer's secret manager. Do not commit either value.

## Local verification

The CI behavior can be reproduced with:

```bash
./gradlew \
  :push-core:jvmTest :push-fcm:jvmTest :push-onesignal:jvmTest :push-test:jvmTest \
  :push-core:iosSimulatorArm64Test :push-fcm:iosSimulatorArm64Test \
  :push-onesignal:iosSimulatorArm64Test :push-test:iosSimulatorArm64Test \
  :push-fcm:compileAndroidMain :push-onesignal:compileAndroidMain \
  :push-core:compileKotlinIosArm64 :push-fcm:compileKotlinIosArm64 \
  :push-onesignal:compileKotlinIosArm64 :push-test:compileKotlinIosArm64 \
  :sample:run
```

To verify version injection without publishing:

```bash
./gradlew :push-core:properties -PreleaseVersion=1.2.3
```
