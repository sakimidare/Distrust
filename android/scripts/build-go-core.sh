#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
CORE="$ROOT/core"
OUTPUT="$CORE/build/distrust-core.aar"

if [[ ! -f "$CORE/go.mod" ]]; then
    echo "DistrustCore submodule is missing. Run: git submodule update --init --recursive" >&2
    exit 1
fi

mkdir -p "$(dirname "$OUTPUT")"
export PATH="$(go env GOPATH)/bin:$PATH"
export GOPROXY="${GOPROXY:-https://goproxy.cn,direct}"

if ! command -v gomobile >/dev/null 2>&1; then
    go install golang.org/x/mobile/cmd/gomobile@latest
fi

(
    cd "$CORE"
    gomobile init
    gomobile bind -target=android -androidapi 26 -o "$OUTPUT" ./mobile
)

echo "Built $OUTPUT"
