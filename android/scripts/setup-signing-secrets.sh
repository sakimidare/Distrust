#!/usr/bin/env bash
#
# Uploads the release keystore and its credentials to GitHub Actions secrets.
#
# Run this yourself: it reads every password with a hidden prompt and pipes
# them straight into `gh`. Nothing is echoed, written to disk (unless you ask
# for the local keystore.properties), or sent anywhere else.
#
# Usage:
#   ./android/scripts/setup-signing-secrets.sh [owner/repo]
#
set -euo pipefail

REPO="${1:-sakimidare/Distrust}"
DEFAULT_KEYSTORE="${HOME}/AndroidStudioProjects/distruct-keystore.jks"
REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"

command -v gh >/dev/null || { echo "gh CLI not found" >&2; exit 1; }
gh auth status >/dev/null 2>&1 || { echo "gh is not authenticated; run: gh auth login" >&2; exit 1; }

read -r -p "Keystore path [${DEFAULT_KEYSTORE}]: " KEYSTORE
KEYSTORE="${KEYSTORE:-$DEFAULT_KEYSTORE}"
[[ -f "$KEYSTORE" ]] || { echo "Keystore not found: $KEYSTORE" >&2; exit 1; }

read -r -s -p "Keystore (store) password: " STORE_PASSWORD; echo
read -r -s -p "Key password [Enter = same as store password]: " KEY_PASSWORD; echo
KEY_PASSWORD="${KEY_PASSWORD:-$STORE_PASSWORD}"

# keytool localizes its output, so force a stable English locale. "-J-Duser.language"
# also covers JDKs that ignore LC_ALL for message bundles.
keytool_en() {
    LANG=C LC_ALL=C keytool -J-Duser.language=en -J-Duser.country=US "$@"
}

if ! keytool_en -list -keystore "$KEYSTORE" -storepass "$STORE_PASSWORD" >/dev/null 2>&1; then
    echo "Cannot open the keystore with that password." >&2
    exit 1
fi

mapfile -t ALIASES < <(
    keytool_en -list -v -keystore "$KEYSTORE" -storepass "$STORE_PASSWORD" 2>/dev/null \
        | sed -nE 's/^(Alias name|别名)[:：][[:space:]]*//p'
)
if [[ ${#ALIASES[@]} -eq 0 ]]; then
    echo "No key aliases found in the keystore." >&2
    echo "keytool output was:" >&2
    keytool_en -list -v -keystore "$KEYSTORE" -storepass "$STORE_PASSWORD" 2>&1 | head -n 20 >&2
    exit 1
fi

echo "Aliases found: ${ALIASES[*]}"
read -r -p "Key alias [${ALIASES[0]}]: " KEY_ALIAS
KEY_ALIAS="${KEY_ALIAS:-${ALIASES[0]}}"

if [[ ! " ${ALIASES[*]} " == *" ${KEY_ALIAS} "* ]]; then
    echo "Alias '${KEY_ALIAS}' is not present in the keystore." >&2
    exit 1
fi

echo
echo "Uploading secrets to ${REPO} ..."
if command -v base64 >/dev/null && base64 -w0 </dev/null >/dev/null 2>&1; then
    KEYSTORE_BASE64="$(base64 -w0 "$KEYSTORE")"
else
    KEYSTORE_BASE64="$(base64 < "$KEYSTORE" | tr -d '\n')"
fi

printf '%s' "$KEYSTORE_BASE64"  | gh secret set ANDROID_KEYSTORE_BASE64   -R "$REPO"
printf '%s' "$STORE_PASSWORD"   | gh secret set ANDROID_KEYSTORE_PASSWORD -R "$REPO"
printf '%s' "$KEY_ALIAS"        | gh secret set ANDROID_KEY_ALIAS         -R "$REPO"
printf '%s' "$KEY_PASSWORD"     | gh secret set ANDROID_KEY_PASSWORD      -R "$REPO"

echo "Secrets uploaded:"
gh secret list -R "$REPO" | grep '^ANDROID_' || true

echo
read -r -p "Also write android/keystore.properties for local release builds? [y/N]: " WRITE_LOCAL
if [[ "${WRITE_LOCAL,,}" == "y" ]]; then
    PROPERTIES="$REPO_ROOT/android/keystore.properties"
    umask 077
    cat > "$PROPERTIES" <<EOF
storeFile=$KEYSTORE
storePassword=$STORE_PASSWORD
keyAlias=$KEY_ALIAS
keyPassword=$KEY_PASSWORD
EOF
    chmod 600 "$PROPERTIES"
    echo "Wrote $PROPERTIES (git-ignored)."
fi

echo "Done."
