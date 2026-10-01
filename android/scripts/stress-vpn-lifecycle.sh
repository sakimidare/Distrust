#!/usr/bin/env bash
set -euo pipefail

PACKAGE="idont.trust.atrust"
ITERATIONS="${1:-50}"
RUN_SECONDS="${RUN_SECONDS:-2}"
STOP_SECONDS="${STOP_SECONDS:-1}"
REPORT_DIR="${REPORT_DIR:-/tmp/opencode/distrust-vpn-stress-$(date +%Y%m%d-%H%M%S)}"

mkdir -p "$REPORT_DIR"
adb get-state >/dev/null
adb logcat -c

for ((iteration = 1; iteration <= ITERATIONS; iteration++)); do
    echo "[$iteration/$ITERATIONS] start"
    adb shell am broadcast -a "$PACKAGE.DEBUG_START_VPN" -p "$PACKAGE" >/dev/null
    sleep "$RUN_SECONDS"
    if ! adb shell pidof "$PACKAGE" >/dev/null; then
        echo "App process died after start at iteration $iteration" >&2
        adb logcat -d > "$REPORT_DIR/logcat.txt"
        exit 1
    fi

    echo "[$iteration/$ITERATIONS] stop"
    adb shell am broadcast -a "$PACKAGE.DEBUG_STOP_VPN" -p "$PACKAGE" >/dev/null
    sleep "$STOP_SECONDS"
    if adb logcat -d -b crash | grep -q "$PACKAGE"; then
        echo "Crash buffer contains $PACKAGE at iteration $iteration" >&2
        adb logcat -d > "$REPORT_DIR/logcat.txt"
        adb logcat -d -b crash > "$REPORT_DIR/crash.txt"
        exit 1
    fi
done

adb logcat -d > "$REPORT_DIR/logcat.txt"
adb logcat -d -b crash > "$REPORT_DIR/crash.txt"
echo "Completed $ITERATIONS iterations; reports: $REPORT_DIR"
