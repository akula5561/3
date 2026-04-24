#!/usr/bin/env bash
set -euo pipefail

OUT_DIR="${1:-keys}"
ALIAS="${2:-app-signing}"
STORE_PASS="${3:-changeit}"
KEY_PASS="${4:-$STORE_PASS}"
DNAME="${5:-CN=App Signing, OU=RBPO, O=University, L=Moscow, C=RU}"

mkdir -p "$OUT_DIR"
KEYSTORE_PATH="$OUT_DIR/signing.jks"
CERT_PATH="$OUT_DIR/signing-public.crt"

keytool -genkeypair \
  -alias "$ALIAS" \
  -keyalg RSA \
  -keysize 2048 \
  -sigalg SHA256withRSA \
  -validity 3650 \
  -keystore "$KEYSTORE_PATH" \
  -storetype JKS \
  -storepass "$STORE_PASS" \
  -keypass "$KEY_PASS" \
  -dname "$DNAME" \
  -noprompt

keytool -exportcert \
  -rfc \
  -alias "$ALIAS" \
  -keystore "$KEYSTORE_PATH" \
  -storepass "$STORE_PASS" \
  -file "$CERT_PATH"

B64=$(base64 < "$KEYSTORE_PATH" | tr -d '\n')

cat <<MSG
Created:
- $KEYSTORE_PATH
- $CERT_PATH

Use in GitHub Secrets:
- SIGNING_KEYSTORE_B64 (value below)
- SIGNING_KEYSTORE_PASSWORD=$STORE_PASS
- SIGNING_KEY_ALIAS=$ALIAS
- SIGNING_KEY_PASSWORD=$KEY_PASS

SIGNING_KEYSTORE_B64:
$B64
MSG
