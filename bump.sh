#!/bin/bash

function die_with() { echo "$*" >&2; exit 1; }

if [ "$1" == "-b" ]; then
    BATCH=true;
    shift
else
    BATCH=false;
fi

NEW_VERSION="$1"

echo "Getting current version from pom.xml"
CURRENT_VERSION="`sed -n 's|.*<version>\(.*\)</version>.*|\1|p' pom.xml | awk '{ print $1; exit }'`"  && echo "Current version from pom.xml: $CURRENT_VERSION"

if [ -z "$NEW_VERSION" ]; then
    RELEASE_VERSION=$(echo $CURRENT_VERSION | perl -pe 's/-SNAPSHOT//')
    NEW_VERSION="$(echo $RELEASE_VERSION | perl -pe 's{^(([0-9]+\.)+)?([0-9]+)$}{$1 . ($3 + 1)}e')" && NEW_VERSION="$(echo $NEW_VERSION | perl -pe 's/-SNAPSHOT//gi')-SNAPSHOT"
fi

if ! $BATCH; then
    read -e -p "New version: " -i "$NEW_VERSION" NEW_VERSION || die_with "Prompt for new version failed"
fi

if ! echo $NEW_VERSION | grep -i -- '-SNAPSHOT' >/dev/null; then echo "WARNING: changing to a release version!"; fi

echo "Updating the project version in build.gradle, pom.xml and README.md to $NEW_VERSION"
sed -ri "s/"`echo $CURRENT_VERSION | sed 's/\./\\\\./g'`"/$NEW_VERSION/g" build.gradle pom.xml README.md || die_with "Failed to update the project version!"
chmod 644 build.gradle pom.xml README.md
