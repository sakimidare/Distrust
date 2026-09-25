#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
CORE="$ROOT/core"
OUTPUT="$CORE/build/distrust-core.aar"
TARGET="${DISTRUST_CORE_TARGET:-android}"
LDFLAGS="${DISTRUST_CORE_LDFLAGS:-}"
TRIMPATH="${DISTRUST_CORE_TRIMPATH:-false}"

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
    args=(bind -target="$TARGET" -androidapi 26 -o "$OUTPUT")
    if [[ "$TRIMPATH" == "true" ]]; then
        args+=(-trimpath)
    fi
    if [[ -n "$LDFLAGS" ]]; then
        args+=(-ldflags="$LDFLAGS")
    fi
    gomobile "${args[@]}" ./mobile
)

echo "Built $OUTPUT target=$TARGET trimpath=$TRIMPATH"
