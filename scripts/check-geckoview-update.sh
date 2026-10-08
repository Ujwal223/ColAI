#!/usr/bin/env bash
# ==============================================================================
# ColAI GeckoView Security Update Checker
#
# Checks Mozilla Maven for new engine releases and notifies if an update
# is available. Does not modify local files or trigger automated builds.
# ==============================================================================

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
LIBS_FILE="${PROJECT_ROOT}/gradle/libs.versions.toml"

echo "=== Checking for Mozilla GeckoView Security Updates ==="

if [ ! -f "$LIBS_FILE" ]; then
    echo "Error: libs.versions.toml not found at $LIBS_FILE" >&2
    exit 1
fi

# Extract current version
CURRENT_VERSION=$(grep '^geckoview = ' "$LIBS_FILE" | sed -E 's/.*geckoview = "([^"]+)".*/\1/')
echo "Current ColAI GeckoView Version: $CURRENT_VERSION"

# Fetch latest version from Mozilla Maven metadata
MAVEN_METADATA_URL="https://maven.mozilla.org/maven2/org/mozilla/geckoview/geckoview-omni/maven-metadata.xml"
echo "Fetching repository metadata from: $MAVEN_METADATA_URL"

LATEST_VERSION=$(curl -sSL "$MAVEN_METADATA_URL" | grep -oPm1 "(?<=<release>)[^<]+" || true)

if [ -z "$LATEST_VERSION" ]; then
    # Fallback to <latest> tag
    LATEST_VERSION=$(curl -sSL "$MAVEN_METADATA_URL" | grep -oPm1 "(?<=<latest>)[^<]+" || true)
fi

if [ -z "$LATEST_VERSION" ]; then
    echo "Warning: Could not automatically parse latest version from Mozilla Maven metadata."
    echo "UPDATED=false" >> "${GITHUB_OUTPUT:-/dev/null}"
    exit 0
fi

echo "Latest Available GeckoView Version: $LATEST_VERSION"

if [ "$CURRENT_VERSION" = "$LATEST_VERSION" ]; then
    echo "GeckoView is up to date ($CURRENT_VERSION). Zero action needed."
    echo "UPDATED=false" >> "${GITHUB_OUTPUT:-/dev/null}"
    echo "CURRENT_VERSION=$CURRENT_VERSION" >> "${GITHUB_OUTPUT:-/dev/null}"
else
    echo "Newer GeckoView release detected: $LATEST_VERSION (Current: $CURRENT_VERSION)"
    echo "UPDATED=true" >> "${GITHUB_OUTPUT:-/dev/null}"
    echo "NEW_VERSION=$LATEST_VERSION" >> "${GITHUB_OUTPUT:-/dev/null}"
    echo "OLD_VERSION=$CURRENT_VERSION" >> "${GITHUB_OUTPUT:-/dev/null}"
fi
