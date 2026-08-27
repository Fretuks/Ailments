#!/usr/bin/env bash
set -euo pipefail

release_name="$(jq -r '.release.name // ""' "$GITHUB_EVENT_PATH")"
jq -r '.release.body // ""' "$GITHUB_EVENT_PATH" > release-artifacts/changelog.md

if [[ -z "${release_name//[[:space:]]/}" ]]; then
  release_name="Ascend: Ailments ${RELEASE_TAG}"
fi

if ! grep -q '[^[:space:]]' release-artifacts/changelog.md; then
  echo "::error::The GitHub release needs a non-empty description; it is used as the CurseForge changelog."
  exit 1
fi

case "${RELEASE_TAG,,}" in
  *-alpha*) release_type="alpha" ;;
  *)
    if [[ "${RELEASE_PRERELEASE,,}" == "true" ]]; then
      release_type="beta"
    else
      release_type="release"
    fi
    ;;
esac

delimiter="release_name_$(date +%s%N)"
{
  echo "display_name<<$delimiter"
  echo "$release_name"
  echo "$delimiter"
  echo "release_type=$release_type"
} >> "$GITHUB_OUTPUT"

echo "Prepared $release_type metadata for $RELEASE_TAG."
