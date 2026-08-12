#!/bin/sh
# ---------------------------------------------------------------------------
# Maven Wrapper for the Blood Donation Card Management System (POSIX shells).
#
# Resolves Apache Maven into ~/.m2/wrapper/dists, downloading it once if it is
# not already there, then runs it. No Maven installation on PATH is required -
# only a JDK.
#
# Usage:  ./mvnw clean test
#         ./mvnw clean package
#         ./mvnw clean package cargo:run
# ---------------------------------------------------------------------------
set -e

BASE_DIR=$(cd "$(dirname "$0")" && pwd)
PROPS="$BASE_DIR/.mvn/wrapper/maven-wrapper.properties"

DIST_URL=$(grep '^distributionUrl=' "$PROPS" | cut -d'=' -f2-)
DIST_PATH=$(grep '^distributionPath=' "$PROPS" | cut -d'=' -f2-)
MAVEN_HOME="$HOME/$DIST_PATH"

if [ ! -x "$MAVEN_HOME/bin/mvn" ]; then
    echo "[mvnw] Apache Maven not found locally. Downloading once..."
    echo "[mvnw]   from $DIST_URL"
    echo "[mvnw]   into $MAVEN_HOME"
    DIST_PARENT=$(dirname "$MAVEN_HOME")
    mkdir -p "$DIST_PARENT"
    TMP_ZIP="${TMPDIR:-/tmp}/bdcms-maven-dist.zip"
    if command -v curl >/dev/null 2>&1; then
        curl -fsSL -o "$TMP_ZIP" "$DIST_URL"
    else
        wget -q -O "$TMP_ZIP" "$DIST_URL"
    fi
    unzip -q -o "$TMP_ZIP" -d "$DIST_PARENT"
    rm -f "$TMP_ZIP"
    chmod +x "$MAVEN_HOME/bin/mvn"
fi

exec "$MAVEN_HOME/bin/mvn" "$@"
