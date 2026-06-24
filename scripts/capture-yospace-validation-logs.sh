#!/usr/bin/env bash
set -euo pipefail

APP_ID="com.bitmovin.player.integration.yospacesample"
ACTIVITY="$APP_ID.MainActivity"
DEFAULT_OUTPUT_DIR="build/yospace-validation"
ADB="${ADB:-adb}"
BUILD=true
SUBMISSION=""
OUTPUT_DIR="$DEFAULT_OUTPUT_DIR"

usage() {
  cat <<USAGE
Usage: $0 --submission <vod|dvr-live-direct|dvr-live-proxy|all> [--output-dir <dir>] [--skip-build]

Generates the two log files required by one Yospace validation submission.
USAGE
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --submission)
      SUBMISSION="${2:-}"
      shift 2
      ;;
    --output-dir)
      OUTPUT_DIR="${2:-}"
      shift 2
      ;;
    --skip-build)
      BUILD=false
      shift
      ;;
    -h|--help)
      usage
      exit 0
      ;;
    *)
      echo "Unknown argument: $1" >&2
      usage >&2
      exit 2
      ;;
  esac
done

if [[ -z "$SUBMISSION" ]]; then
  echo "--submission is required" >&2
  usage >&2
  exit 2
fi

case "$SUBMISSION" in
  vod|dvr-live-direct|dvr-live-proxy|all) ;;
  *)
    echo "Unsupported submission: $SUBMISSION" >&2
    usage >&2
    exit 2
    ;;
esac

adb_cmd() {
  "$ADB" "$@"
}

submissions() {
  if [[ "$SUBMISSION" == "all" ]]; then
    printf '%s\n' vod dvr-live-direct dvr-live-proxy
  else
    printf '%s\n' "$SUBMISSION"
  fi
}

submission_asset() {
  case "$1" in
    vod) echo "VOD" ;;
    dvr-live-direct|dvr-live-proxy) echo "DVR_LIVE" ;;
  esac
}

submission_initialisation_type() {
  case "$1" in
    vod) echo "PROXY" ;;
    dvr-live-direct) echo "DIRECT" ;;
    dvr-live-proxy) echo "PROXY" ;;
  esac
}

submission_initialisation_label() {
  case "$1" in
    vod) echo "N/A" ;;
    dvr-live-direct) echo "DIRECT" ;;
    dvr-live-proxy) echo "PROXY" ;;
  esac
}

test_case_extra() {
  case "$1" in
    ad_break) echo "AD_BREAK" ;;
    two_sessions) echo "TWO_SESSIONS" ;;
  esac
}

test_case_timeout_seconds() {
  case "$1" in
    ad_break) echo 960 ;;
    two_sessions) echo 420 ;;
  esac
}

timestamp() {
  date -u +"%Y%m%dT%H%M%SZ"
}

commit_sha() {
  git rev-parse --short HEAD 2>/dev/null || echo "unknown"
}

wait_for_marker() {
  local log_file="$1"
  local timeout_seconds="$2"
  local deadline=$((SECONDS + timeout_seconds))

  while (( SECONDS < deadline )); do
    if grep -Eq "YospaceValidation.*PASS" "$log_file"; then
      return 0
    fi
    if grep -Eq "YospaceValidation.*FAIL" "$log_file"; then
      return 1
    fi
    sleep 2
  done

  echo "$(date '+%m-%d %H:%M:%S.000') E/YospaceValidation: FAIL reason=host-timeout" >> "$log_file"
  return 1
}

capture_case() {
  local submission="$1"
  local test_case="$2"
  local run_dir="$3"
  local failed_dir="$4"
  local asset
  local init_type
  local test_case_name
  local timeout_seconds
  local temp_log
  local final_log
  local logcat_pid=""

  asset="$(submission_asset "$submission")"
  init_type="$(submission_initialisation_type "$submission")"
  test_case_name="$(test_case_extra "$test_case")"
  timeout_seconds="$(test_case_timeout_seconds "$test_case")"
  temp_log="$(mktemp "/tmp/yospace-validation-${submission}-${test_case}.XXXXXX.log")"
  final_log="$run_dir/${submission}_${test_case}.log"

  echo "Capturing $submission / $test_case_name"
  adb_cmd shell am force-stop "$APP_ID" >/dev/null || true
  adb_cmd logcat -c
  adb_cmd logcat -v time > "$temp_log" &
  logcat_pid="$!"

  adb_cmd shell am start \
    -n "$APP_ID/$ACTIVITY" \
    --ez validationMode true \
    --es asset "$asset" \
    --es initialisationType "$init_type" \
    --es testCase "$test_case_name" >/dev/null

  if wait_for_marker "$temp_log" "$timeout_seconds"; then
    kill "$logcat_pid" 2>/dev/null || true
    wait "$logcat_pid" 2>/dev/null || true
    adb_cmd shell am force-stop "$APP_ID" >/dev/null || true
    cp "$temp_log" "$final_log"
    echo "Wrote $final_log"
    rm -f "$temp_log"
    return 0
  fi

  kill "$logcat_pid" 2>/dev/null || true
  wait "$logcat_pid" 2>/dev/null || true
  adb_cmd shell am force-stop "$APP_ID" >/dev/null || true
  mkdir -p "$failed_dir"
  cp "$temp_log" "$failed_dir/${submission}_${test_case}.failed.log"
  rm -f "$temp_log"
  echo "Validation run failed. Debug log: $failed_dir/${submission}_${test_case}.failed.log" >&2
  return 1
}

write_manifest() {
  local submission="$1"
  local run_dir="$2"
  local asset
  local init_type
  local init_label

  asset="$(submission_asset "$submission")"
  init_type="$(submission_initialisation_type "$submission")"
  init_label="$(submission_initialisation_label "$submission")"

  cat > "$run_dir/${submission}_manifest.txt" <<MANIFEST
Submission: $submission
Yospace validation selection: $(case "$submission" in
  vod) echo "VOD" ;;
  dvr-live-direct) echo "DVR Live with direct initialisation" ;;
  dvr-live-proxy) echo "DVR Live with proxy initialisation" ;;
esac)
Asset extra: $asset
Initialisation type: $init_label
Commit: $(commit_sha)
Created at: $(timestamp)
Upload files:
- ${submission}_ad_break.log
- ${submission}_two_sessions.log
MANIFEST
}

if [[ "$BUILD" == true ]]; then
  JAVA_HOME="${JAVA_HOME:-/Applications/Android Studio.app/Contents/jbr/Contents/Home}" ./gradlew :yospacesample:installDebug
fi

mkdir -p "$OUTPUT_DIR"

for submission in $(submissions); do
  run_dir="$OUTPUT_DIR/${submission}-$(timestamp)"
  failed_dir="$run_dir/failed"
  mkdir -p "$run_dir"

  if capture_case "$submission" ad_break "$run_dir" "$failed_dir" &&
    capture_case "$submission" two_sessions "$run_dir" "$failed_dir"; then
    write_manifest "$submission" "$run_dir"
    echo "Upload-ready logs: $run_dir"
  else
    echo "Submission $submission failed; not upload-ready." >&2
    exit 1
  fi
done
