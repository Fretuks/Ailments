#!/usr/bin/env bash
set -euo pipefail

required_variables=(
  CHANGELOG_FILE
  CURSEFORGE_API_TOKEN
  CURSEFORGE_PROJECT_ID
  DISPLAY_NAME
  JAR_PATH
  MINECRAFT_VERSION
  RELEASE_TYPE
)

for variable in "${required_variables[@]}"; do
  if [[ -z "${!variable:-}" ]]; then
    echo "::error::$variable is required for the CurseForge upload."
    exit 1
  fi
done

if [[ ! "$CURSEFORGE_PROJECT_ID" =~ ^[0-9]+$ ]]; then
  echo "::error::CURSEFORGE_PROJECT_ID must be the numeric project ID from the CurseForge project page."
  exit 1
fi
if [[ ! -f "$JAR_PATH" || ! -f "$CHANGELOG_FILE" ]]; then
  echo "::error::The release JAR or changelog file is missing."
  exit 1
fi
if [[ "$RELEASE_TYPE" != "alpha" && "$RELEASE_TYPE" != "beta" && "$RELEASE_TYPE" != "release" ]]; then
  echo "::error::RELEASE_TYPE must be alpha, beta, or release."
  exit 1
fi

metadata_file="$(mktemp)"
response_file="$(mktemp)"
trap 'rm -f "$metadata_file" "$response_file"' EXIT

jq -n \
  --rawfile changelog "$CHANGELOG_FILE" \
  --arg display_name "$DISPLAY_NAME" \
  --arg minecraft_version "$MINECRAFT_VERSION" \
  --arg release_type "$RELEASE_TYPE" \
  '{
    changelog: $changelog,
    changelogType: "markdown",
    displayName: $display_name,
    gameVersionNames: [$minecraft_version, "Forge", "Client", "Server"],
    releaseType: $release_type,
    relations: {
      projects: [
        {
          slug: "ascend",
          projectID: "1383429",
          type: "optionalDependency"
        }
      ]
    }
  }' > "$metadata_file"

upload_url="https://minecraft.curseforge.com/api/projects/${CURSEFORGE_PROJECT_ID}/upload-file"
if ! curl \
  --silent \
  --show-error \
  --fail-with-body \
  --retry 3 \
  --retry-delay 2 \
  --header "X-Api-Token: ${CURSEFORGE_API_TOKEN}" \
  --form "metadata=<${metadata_file};type=application/json" \
  --form "file=@${JAR_PATH};type=application/java-archive" \
  --output "$response_file" \
  "$upload_url"; then
  echo "::error::CurseForge rejected the upload. Response:"
  cat "$response_file" >&2
  exit 1
fi

file_id="$(jq -er '.id' "$response_file")"
echo "Uploaded $(basename "$JAR_PATH") to CurseForge as file ID $file_id."
