#!/usr/bin/env bash
# Runs the same Strix scan as .github/workflows/strix-scan.yml, locally.
# Pins are duplicated from that workflow - keep them in sync on a reviewed Strix upgrade.
set -euo pipefail

: "${STRIX_LLM:?Set STRIX_LLM (see .github/workflows/strix-scan.yml secrets)}"
: "${LLM_API_KEY:?Set LLM_API_KEY}"
: "${LLM_API_BASE:?Set LLM_API_BASE}"

STRIX_VERSION="1.7.0"
STRIX_ASSET_SHA256="b8cd7e84d63cbeff6726301a9e08fddedf40fc6a4edcdb4edda8a853b3312de6"
STRIX_SANDBOX_IMAGE="ghcr.io/usestrix/strix-sandbox:1.3.1"
STRIX_SANDBOX_DIGEST="sha256:efd7f18980c126700ddf55f6d0c0c86436d8d20908f4e1252cb761212749ded5"

strix_bin="$HOME/.strix/bin/strix-${STRIX_VERSION}"
if [ ! -x "$strix_bin" ]; then
    echo "Installing Strix ${STRIX_VERSION}..."
    tmp="$(mktemp -d)"
    curl -fsSL -o "$tmp/strix.tar.gz" \
        "https://github.com/usestrix/strix/releases/download/v${STRIX_VERSION}/strix-${STRIX_VERSION}-linux-x86_64.tar.gz"
    echo "${STRIX_ASSET_SHA256}  $tmp/strix.tar.gz" | sha256sum --check -
    mkdir -p "$HOME/.strix/bin"
    tar -xzf "$tmp/strix.tar.gz" -C "$HOME/.strix/bin"
    mv "$HOME/.strix/bin/strix-${STRIX_VERSION}-linux-x86_64" "$strix_bin"
    chmod +x "$strix_bin"
    rm -rf "$tmp"
fi

# docker keeps pulled layers in its local cache, so after the first run this is a no-op.
docker pull --platform linux/amd64 "${STRIX_SANDBOX_IMAGE%:*}@${STRIX_SANDBOX_DIGEST}"
docker tag "${STRIX_SANDBOX_IMAGE%:*}@${STRIX_SANDBOX_DIGEST}" "${STRIX_SANDBOX_IMAGE}"

"$strix_bin" -n -t ./ --scan-mode quick --scope-mode diff --fail-on high
