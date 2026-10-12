#!/usr/bin/env bash
set -Eeuo pipefail

SDK_ROOT="${ANDROID_SDK_ROOT:-${ANDROID_HOME:-}}"
AVD_HOME="${ANDROID_AVD_HOME:-/tmp/ludopaws-avd}"
AVD_NAME="ludopaws-ci"
EMULATOR="$SDK_ROOT/emulator/emulator"
PID_FILE="/tmp/ludopaws-emulator.pid"

if [[ -z "$SDK_ROOT" || ! -x "$EMULATOR" ]]; then
  echo "Android emulator executable was not found under SDK_ROOT='$SDK_ROOT'." >&2
  exit 2
fi

boot_emulator() {
  local wipe_data="${1:-false}"
  local log_file="${2:-/tmp/ludopaws-emulator-retry.log}"

  adb kill-server >/dev/null 2>&1 || true
  sleep 2
  rm -f "$AVD_HOME/$AVD_NAME.avd"/*.lock

  # Match the initial boot configuration: software rendering with Vulkan disabled.
  # This avoids the SwiftShader/Vulkan emulator segfault observed on hosted Linux runners.
  local args=(
    -avd "$AVD_NAME"
    -no-window
    -noaudio
    -no-boot-anim
    -no-snapshot
    -camera-back none
    -camera-front none
    -gpu software
    -feature -Vulkan
  )
  if [[ "$wipe_data" == "true" ]]; then
    args+=(-wipe-data)
  fi

  nohup "$EMULATOR" "${args[@]}" > "$log_file" 2>&1 &
  local emulator_pid=$!
  echo "$emulator_pid" > "$PID_FILE"

  local device_ready=""
  for attempt in $(seq 1 90); do
    if ! kill -0 "$emulator_pid" 2>/dev/null; then
      echo "Android emulator exited before ADB became ready."
      cat "$log_file"
      return 1
    fi
    if adb get-state 2>/dev/null | grep -q '^device$'; then
      device_ready="1"
      break
    fi
    sleep 2
  done

  if [[ "$device_ready" != "1" ]]; then
    echo "Android emulator did not become visible to ADB within 180 seconds."
    cat "$log_file"
    adb devices -l || true
    return 1
  fi

  local booted=""
  for attempt in $(seq 1 120); do
    if ! kill -0 "$emulator_pid" 2>/dev/null; then
      echo "Android emulator exited before boot completed."
      cat "$log_file"
      return 1
    fi
    booted="$(adb shell getprop sys.boot_completed 2>/dev/null | tr -d '\r' || true)"
    if [[ "$booted" == "1" ]]; then
      break
    fi
    sleep 2
  done

  if [[ "$booted" != "1" ]]; then
    echo "Android emulator did not complete boot within 240 seconds after ADB connected."
    cat "$log_file"
    adb devices -l || true
    return 1
  fi

  adb shell input keyevent 82 || true
  adb shell settings put global window_animation_scale 0
  adb shell settings put global transition_animation_scale 0
  adb shell settings put global animator_duration_scale 0
  adb devices -l
}

run_instrumentation() {
  gradle -p android :app:connectedDebugAndroidTest --stacktrace
}

set +e
run_instrumentation
first_status=$?
set -e

if [[ "$first_status" -eq 0 ]]; then
  exit 0
fi

# Only recover from an emulator transport loss. If the device still responds,
# preserve real assertion/build failures instead of retrying or masking them.
if adb get-state 2>/dev/null | grep -q '^device$'; then
  echo "Instrumentation failed while the emulator is still connected; not retrying." >&2
  exit "$first_status"
fi

echo "Instrumentation failed after the emulator disappeared from ADB; restarting a clean emulator and retrying once." >&2
if [[ -f "$PID_FILE" ]]; then
  old_pid="$(cat "$PID_FILE")"
  kill "$old_pid" >/dev/null 2>&1 || true
  for attempt in $(seq 1 10); do
    if ! kill -0 "$old_pid" 2>/dev/null; then
      break
    fi
    sleep 1
  done
  kill -9 "$old_pid" >/dev/null 2>&1 || true
fi

boot_emulator true /tmp/ludopaws-emulator-retry.log
run_instrumentation
