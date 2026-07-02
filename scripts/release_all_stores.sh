#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
KEYSTORE_DEFAULT="/Users/farshadasgharzadeh/Documents/Android/Key/keystore.jks"
BUNDLE_SIGNER_DEFAULT="/Users/farshadasgharzadeh/Desktop/Dektop/Project Util/bundlesigner-0.1.13.jar"

export SHECAN_RELEASE_KEYSTORE="${SHECAN_RELEASE_KEYSTORE:-$KEYSTORE_DEFAULT}"
export SHECAN_RELEASE_KEY_ALIAS="${SHECAN_RELEASE_KEY_ALIAS:-shecan}"
export SHECAN_RELEASE_UPLOAD_SENTRY="${SHECAN_RELEASE_UPLOAD_SENTRY:-true}"
BUNDLE_SIGNER_JAR="${BUNDLE_SIGNER_JAR:-$BUNDLE_SIGNER_DEFAULT}"

read_secret() {
    local variable_name="$1"
    local prompt="$2"
    local value="${!variable_name:-}"
    if [[ -z "$value" ]]; then
        read -r -s -p "$prompt" value
        printf '\n'
        export "$variable_name=$value"
    fi
}

require_file() {
    if [[ ! -f "$1" ]]; then
        echo "Missing required file: $1" >&2
        exit 1
    fi
}

copy_single() {
    local pattern="$1"
    local destination="$2"
    local matches=()
    while IFS= read -r file; do
        matches+=("$file")
    done < <(find "$(dirname "$pattern")" -maxdepth 1 -type f -name "$(basename "$pattern")" -print)

    if [[ "${#matches[@]}" -ne 1 ]]; then
        echo "Expected one file for $pattern, found ${#matches[@]}" >&2
        exit 1
    fi
    cp "${matches[0]}" "$destination"
}

cd "$ROOT_DIR"
require_file "$SHECAN_RELEASE_KEYSTORE"
require_file "$BUNDLE_SIGNER_JAR"
read_secret SHECAN_RELEASE_STORE_PASSWORD "Keystore password: "
read_secret SHECAN_RELEASE_KEY_PASSWORD "Key password: "

VERSION_NAME="$(sed -nE 's/^[[:space:]]*versionName[[:space:]]+"([^"]+)".*/\1/p' app/build.gradle | head -1)"
VERSION_CODE="$(sed -nE 's/^[[:space:]]*versionCode[[:space:]]+([0-9]+).*/\1/p' app/build.gradle | head -1)"
if [[ -z "$VERSION_NAME" || -z "$VERSION_CODE" ]]; then
    echo "Could not read versionName/versionCode from app/build.gradle" >&2
    exit 1
fi

OUTPUT_DIR="$ROOT_DIR/release-output/v${VERSION_CODE}-${VERSION_NAME}"
rm -rf "$OUTPUT_DIR"
mkdir -p "$OUTPUT_DIR/myket" "$OUTPUT_DIR/cafebazaar" "$OUTPUT_DIR/site"

echo "Building Myket ABI APKs..."
./gradlew --no-daemon -PMYKET_ABI_SPLITS assembleMyketRelease

MYKET_APK_DIR="$ROOT_DIR/app/build/outputs/apk/myket/release"
copy_single "$MYKET_APK_DIR/*armeabi-v7a*release.apk" \
    "$OUTPUT_DIR/myket/shecan-${VERSION_NAME}-${VERSION_CODE}-myket-armeabi-v7a.apk"
copy_single "$MYKET_APK_DIR/*arm64-v8a*release.apk" \
    "$OUTPUT_DIR/myket/shecan-${VERSION_NAME}-${VERSION_CODE}-myket-arm64-v8a.apk"

echo "Building CafeBazaar APK and AAB..."
./gradlew --no-daemon assembleCafeBazaarRelease bundleCafeBazaarRelease

BAZAAR_APK_DIR="$ROOT_DIR/app/build/outputs/apk/cafeBazaar/release"
BAZAAR_BUNDLE_DIR="$ROOT_DIR/app/build/outputs/bundle/cafeBazaarRelease"
BAZAAR_APK="$OUTPUT_DIR/cafebazaar/shecan-${VERSION_NAME}-${VERSION_CODE}-cafebazaar.apk"
BAZAAR_AAB="$OUTPUT_DIR/cafebazaar/shecan-${VERSION_NAME}-${VERSION_CODE}-cafebazaar.aab"
copy_single "$BAZAAR_APK_DIR/*release.apk" "$BAZAAR_APK"
copy_single "$BAZAAR_BUNDLE_DIR/*.aab" "$BAZAAR_AAB"

echo "Generating CafeBazaar BIN..."
java -jar "$BUNDLE_SIGNER_JAR" genbin \
    -v \
    --bundle "$BAZAAR_AAB" \
    --bin "$OUTPUT_DIR/cafebazaar" \
    --v2-signing-enabled true \
    --v3-signing-enabled false \
    --ks "$SHECAN_RELEASE_KEYSTORE" \
    --ks-key-alias "$SHECAN_RELEASE_KEY_ALIAS" \
    --ks-pass env:SHECAN_RELEASE_STORE_PASSWORD \
    --key-pass env:SHECAN_RELEASE_KEY_PASSWORD

BAZAAR_GENERATED_BIN="$(find "$OUTPUT_DIR/cafebazaar" -maxdepth 1 -type f -name '*.bin' -print | head -1)"
if [[ -z "$BAZAAR_GENERATED_BIN" ]]; then
    echo "CafeBazaar BIN was not generated" >&2
    exit 1
fi
BAZAAR_BIN="$OUTPUT_DIR/cafebazaar/shecan-${VERSION_NAME}-${VERSION_CODE}-cafebazaar.bin"
if [[ "$BAZAAR_GENERATED_BIN" != "$BAZAAR_BIN" ]]; then
    mv "$BAZAAR_GENERATED_BIN" "$BAZAAR_BIN"
fi

echo "Building Site APK..."
./gradlew --no-daemon assembleSiteRelease

SITE_APK_DIR="$ROOT_DIR/app/build/outputs/apk/site/release"
copy_single "$SITE_APK_DIR/*release.apk" \
    "$OUTPUT_DIR/site/shecan-${VERSION_NAME}-${VERSION_CODE}-site.apk"

APKSIGNER="$(find "${ANDROID_HOME:-$HOME/Library/Android/sdk}/build-tools" -name apksigner -type f | sort -V | tail -1)"
require_file "$APKSIGNER"

echo "Verifying APK signatures..."
while IFS= read -r apk; do
    "$APKSIGNER" verify --verbose --print-certs "$apk" >/dev/null
done < <(find "$OUTPUT_DIR" -type f -name '*.apk' -print)

echo
echo "Release outputs:"
find "$OUTPUT_DIR" -type f -maxdepth 2 -print | sort
