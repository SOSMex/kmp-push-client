#!/usr/bin/env bash
set -euo pipefail

repository="${1:-build/test-maven}"
version="${2:-0.1.0}"
group_path="${repository}/io/github/sosmex/push"
modules=(push-core push-fcm push-onesignal push-test)
targets=(android iosarm64 iossimulatorarm64 jvm)

fail() {
  printf 'publication verification failed: %s\n' "$*" >&2
  exit 1
}

require_file() {
  [[ -s "$1" ]] || fail "missing or empty file: $1"
}

require_archive_entry() {
  local archive="$1"
  local pattern="$2"
  jar tf "$archive" | grep -E "$pattern" >/dev/null || fail "${archive} does not contain ${pattern}"
}

for module in "${modules[@]}"; do
  root_dir="${group_path}/${module}/${version}"
  root_prefix="${root_dir}/${module}-${version}"
  require_file "${root_prefix}.pom"
  require_file "${root_prefix}.module"
  require_file "${root_prefix}-sources.jar"
  require_file "${root_prefix}-javadoc.jar"

  grep -Fq '<name>KMP Push Client' "${root_prefix}.pom" || fail "${module} POM is missing its name"
  grep -Fq '<name>The Apache License, Version 2.0</name>' "${root_prefix}.pom" || fail "${module} POM is missing Apache-2.0"
  grep -Fq '<id>sosmex</id>' "${root_prefix}.pom" || fail "${module} POM is missing the SOSMex developer"
  grep -Fq '<name>SOSMex</name>' "${root_prefix}.pom" || fail "${module} POM is missing the SOSMex organization"
  grep -Fq '<connection>scm:git:https://github.com/SOSMex/kmp-push-client.git</connection>' "${root_prefix}.pom" || fail "${module} POM is missing SCM metadata"

  require_archive_entry "${root_prefix}-sources.jar" '\.kt$'
  require_archive_entry "${root_prefix}-javadoc.jar" '^README\.md$'
  require_archive_entry "${root_prefix}-javadoc.jar" '^LICENSE$'
  require_archive_entry "${root_prefix}-javadoc.jar" '^docs/getting-started\.md$'

  for target in "${targets[@]}"; do
    target_module="${module}-${target}"
    target_dir="${group_path}/${target_module}/${version}"
    target_prefix="${target_dir}/${target_module}-${version}"
    require_file "${target_prefix}.pom"
    require_file "${target_prefix}.module"
    require_file "${target_prefix}-sources.jar"
    require_file "${target_prefix}-javadoc.jar"
    grep -Eq "\"module\"[[:space:]]*:[[:space:]]*\"${target_module}\"" "${root_prefix}.module" ||
      fail "${module} root metadata does not reference ${target_module}"
  done

  android_prefix="${group_path}/${module}-android/${version}/${module}-android-${version}"
  require_file "${android_prefix}.aar"
  require_archive_entry "${android_prefix}.aar" '^AndroidManifest\.xml$'
  require_archive_entry "${android_prefix}.aar" '^classes\.jar$'

  for apple_target in iosarm64 iossimulatorarm64; do
    klib="${group_path}/${module}-${apple_target}/${version}/${module}-${apple_target}-${version}.klib"
    require_file "${klib}"
    require_archive_entry "${klib}" '(^|/)manifest$'
  done
done

printf 'maven_publications=accepted version=%s modules=%s\n' "${version}" "${#modules[@]}"
