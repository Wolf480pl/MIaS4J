#!/bin/bash

function die_with() { echo "$*" >&2; exit 1; }

function has_command() { if command -v $1 >/dev/null 2>&1; then return 0; else return 1; fi; }

function has_xmllint_with_xpath() { if [ "$(xmllint 2>&1 | grep xpath | wc -l)" = "0" ]; then return 1; else return 0; fi; }

function die_unless_xmllint_has_xpath() {
    has_command xmllint || die_with "Missing xmllint command, please install it (from libxml2)"
    has_xmllint_with_xpath || die_with "xmllint command is missing the --xpath option, please install the libxml2 version"
}

if [ "$1" == "-b" ]; then
    BATCH=true;
    shift
else
    BATCH=false;
fi

if [ "$1" != "-m" ]; then
    CUR_BRANCH=`git rev-parse --abbrev-ref HEAD`
    if [ -z $1 ]; then
        [ "$CUR_BRANCH" != "master" ] || die_with "You must either checkout the release branch or checkout master and specify release branch on commandline"
        RELEASE_BRANCH="$CUR_BRANCH"
        TARGET_BRANCH="master"
        git checkout master
    else
        [ "$1" != "$CUR_BRANCH" ] || die_with "The specified release can't be same as current branch."
        RELEASE_BRANCH="$1"
        TARGET_BRANCH="$CUR_BRANCH"
    fi
    echo "Merging $RELEASE_BRANCH to $TARGET_BRANCH"
    git merge --no-ff --no-edit "$RELEASE_BRANCH"
fi

echo "Getting current version from pom.xml"
if [ -z $CURRENT_VERSION ]; then die_unless_xmllint_has_xpath; CURRENT_VERSION=$(xmllint --xpath "/*[local-name() = 'project']/*[local-name() = 'version']/text()" pom.xml); fi
echo "Current version from pom.xml: $CURRENT_VERSION"

RELEASE_VERSION=$(echo $CURRENT_VERSION | perl -pe 's/-SNAPSHOT//')

if ! $BATCH; then
  read -e -p "Release version: " -i "$RELEASE_VERSION" RELEASE_VERSION
fi

if [ $RELEASE_VERSION = $CURRENT_VERSION ]; then die_with "Release version requested is exactly the same as the current pom.xml version ($CURRENT_VERSION)! Is the version in pom.xml definitely a -SNAPSHOT version?"; fi

TAG_NAME="v$RELEASE_VERSION"

if ! $BATCH; then
  read -e -p "Release tag: " -i "$TAG_NAME" TAG_NAME
fi

NEXT_VERSION="$(echo $RELEASE_VERSION | perl -pe 's{^(([0-9]+\.)+)?([0-9]+)$}{$1 . ($3 + 1)}e')" && NEXT_VERSION="$(echo $NEXT_VERSION | perl -pe 's/-SNAPSHOT//gi')-SNAPSHOT"

if ! $BATCH; then
  read -e -p "Next version: " -i "$NEXT_VERSION" NEXT_VERSION
fi

if [ $NEXT_VERSION = "${RELEASE_VERSION}-SNAPSHOT" ]; then die_with "Release version and next version are the same version!"; fi

echo "Using $RELEASE_VERSION for release, tag name: $TAG_NAME" && echo "Using $NEXT_VERSION for next development version"

git fetch --tags
if [ $(git tag -l $RELEASE_VERSION | wc -l) != "0" ]; then die_with "A tag already exists $CURRENT_VERSION for the release version $RELEASE_VERSION!"; fi

#git fetch origin refs/heads/$RELEASE_BRANCH:refs/heads/$RELEASE_BRANCH

echo "Updating project version and SCM information"
mvn -B release:clean release:prepare -DreleaseVersion=$RELEASE_VERSION -DdevelopementVersion=$NEXT_VERSION -DpushChanges=false -Darguments="-Dgpg.skip" -Dtag="$TAG_NAME" || die_with "Failed to prepare release!"
echo "Removing commit with the development version"
git reset --hard HEAD^

echo "Squashing the merge commit into the release commit"
TARGET_COMMIT=`git rev-parse HEAD` git filter-branch -f --commit-filter 'if [ "$GIT_COMMIT" = "$TARGET_COMMIT" ]; then git_commit_non_empty_tree "$@"; else skip_commit "$@"; fi' -- HEAD^^..HEAD --not $RELEASE_BRANCH

#echo "Building, generating, and deploying artifacts"
#mvn package -DbuildNumber=$TRAVIS_BUILD_NUMBER -DciSystem=travis -Dcommit=${TRAVIS_COMMIT:0:7} site javadoc:jar source:jar gpg:sign deploy --settings $HOME/build/flow/travis/settings.xml -Dgpg.name=ED997FF2 -Dgpg.passphrase=$SIGNING_PASSWORD -Dgpg.publicKeyring=$HOME/build/flow/travis/pubring.gpg -Dgpg.secretKeyring=$HOME/build/flow/travis/secring.gpg || die_with "Failed to build/deploy artifacts!"

echo "Updating the project version in build.gradle and README.md"
sed -ri "s/"`echo $CURRENT_VERSION | sed 's/\./\\\\./g'`"/$RELEASE_VERSION/g" README.md || die_with "Failed to update the project version in README.md!"
sed -ri "s/"`echo $CURRENT_VERSION | sed 's/\./\\\\./g'`"/$RELEASE_VERSION/g" build.gradle

echo "Renaming the commit to skip the CI build loop"
git add -u . && git commit --amend -m "Release version $RELEASE_VERSION [ci skip]" || die_with "Failed to rename the commit"

#echo "Force-pushing commit with git"
#git push -qf https://$GITHUB_TOKEN@github.com/$TRAVIS_REPO_SLUG.git HEAD:master || die_with "Failed to push the commit!"

echo "Tagging the release with git"
if $BATCH; then
  TAGOPTS=''
else
  TAGOPTS='-a'
fi
git tag $TAGOPTS -f $TAG_NAME || die_with "Failed to create tag $TAG_NAME!"
#git tag -f $RELEASE_VERSION && git push -q --tags https://$GITHUB_TOKEN@github.com/$TRAVIS_REPO_SLUG.git || die_with "Failed to create tag $RELEASE_VERSION!"
#echo $RELEASE_VERSION > $TRAVIS_BUILD_DIR/version.txt

echo "Release is prepared (just push it and deploy it). Project is now at version $NEXT_VERSION. Happy developing!"
