#!/usr/bin/env bash
set -euo pipefail

property() {
  local key="$1"
  awk -F= -v key="$key" '$1 == key { sub(/^[^=]*=/, ""); print; exit }' gradle.properties
}

mod_id="$(property mod_id)"
mod_version="$(property mod_version)"
minecraft_version="$(property minecraft_version)"

if [[ -z "$mod_id" || -z "$mod_version" || -z "$minecraft_version" ]]; then
  echo "::error::mod_id, mod_version or minecraft_version is missing from gradle.properties."
  exit 1
fi

expected_tag="v${mod_version}"
if [[ -n "${RELEASE_TAG:-}" && "$RELEASE_TAG" != "$expected_tag" ]]; then
  echo "::error::Release tag '$RELEASE_TAG' does not match mod_version '$mod_version'. Expected '$expected_tag'."
  exit 1
fi

jar_path="build/libs/${mod_id}-${mod_version}.jar"
if [[ ! -f "$jar_path" ]]; then
  echo "::error::Expected release artifact '$jar_path' was not produced."
  find build/libs -maxdepth 1 -type f -print 2>/dev/null || true
  exit 1
fi

mods_toml="$(unzip -p "$jar_path" META-INF/mods.toml)"
if ! grep -Fq "modId=\"${mod_id}\"" <<<"$mods_toml"; then
  echo "::error::The built JAR does not contain the expected mod ID '$mod_id'."
  exit 1
fi
if ! grep -Fq "version=\"${mod_version}\"" <<<"$mods_toml"; then
  echo "::error::The built JAR does not contain the expected version '$mod_version'."
  exit 1
fi

{
  echo "jar_name=$(basename "$jar_path")"
  echo "jar_path=$jar_path"
  echo "minecraft_version=$minecraft_version"
  echo "mod_version=$mod_version"
} >> "$GITHUB_OUTPUT"

echo "Verified $jar_path (Minecraft $minecraft_version, version $mod_version)."
