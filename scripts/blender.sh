#!/usr/bin/env bash

if [[ $1 == "-h" || $1 == "--help" ]]; then
  cat <<EOF
Usage: $0 [-d][-y][-l live.hy] [scene.blend] -- [blender opts]

Options:
  -d/--debug              Enable verbose debug printing.
  -l/--hylive file.hy     Watches hy file for changes and executes it in Blender on save or frame change.
  -y/--hyrepl             Open hylang nREPL server upon startup.
  -e/--entry              Run this js script upon startup. Must be relative to origin/assets.
  -a/--assets             Specifies directory to javascript assets. Must be relative to origin.
  -o/--origin             Specifies origin directory for javascript runtime.
  -u/--unattended         Run Blender in 'unattended' mode, used for running tests

Note:
  You can pass blender executable options after -- separator, e.g.

  $0 -d -- --no-window-focus

  You may additionally set BCLJ_BLENDER_OPTS as env variable.

Warning:
  You might want to set window position on startup via --window-geometry
  but this won't work with multi-monitor setup on macOS. Instead we provide a helper applescript to deal
  with that. You should set following environment vars:

    BCLJ_BLENDER_WINDOW_PX
    BCLJ_BLENDER_WINDOW_PY
    BCLJ_BLENDER_WINDOW_W
    BCLJ_BLENDER_WINDOW_H
EOF
  exit 0
fi

# poor man's bash flags parsing
# https://stackoverflow.com/a/14203146/84283
POSITIONAL_OPTS=()
COLLECT_BLENDER_OPTS=0
BLENDER_CLI_OPTS=()
while [[ $# -gt 0 ]]; do
  key="$1"

  case $key in
  -d | --debug)
    ENABLE_DEBUG=1
    shift
    ;;
  -l | --hylive)
    shift
    LIVE_FILE_PARAM="$1"
    shift
    ;;
  -y | --hyrepl)
    ENABLE_HYREPL=1
    shift
    ;;
  -e | --entry)
    shift
    BCLJ_JS_ENTRY_SCRIPT="$1"
    shift
    ;;
  -a | --assets)
    shift
    BCLJ_JS_ASSETS_DIR="$1"
    shift
    ;;
  -o | --origin)
    shift
    BCLJ_JS_ORIGIN_DIR="$1"
    shift
    ;;
  -u | --unattended)
    BCLJ_UNATTENDED=1
    shift
    ;;
  --)
    COLLECT_BLENDER_OPTS=1
    shift
    ;;
  *) # unknown option
    if [[ $COLLECT_BLENDER_OPTS == "1" ]]; then
      BLENDER_CLI_OPTS+=("$1")
    else
      POSITIONAL_OPTS+=("$1")
    fi
    shift
    ;;
  esac
done
set -- "${POSITIONAL_OPTS[@]}" # restore positional parameters

set -e -o pipefail
# shellcheck source=_config.sh
source "$(dirname "${BASH_SOURCE[0]}")/_config.sh"

if [[ -n "$LIVE_FILE_PARAM" ]]; then
  if [[ ! -f "$LIVE_FILE_PARAM" ]]; then
    echo "Specified live file does not exist: '$LIVE_FILE_PARAM'"
    exit 1
  fi
  BCLJ_LIVE_FILE=$(get_absolute_path_of_existing_file "$LIVE_FILE_PARAM")
  export BCLJ_LIVE_FILE
fi

cd "$ROOT_DIR"

if [[ -n "$DEBUG" || -n "$ENABLE_DEBUG" ]]; then
  export BCLJ_DEBUG=1
fi

if [[ -n "$ENABLE_HYREPL" ]]; then
  export BCLJ_HYLANG_NREPL=1
fi

if [[ -z "$BCLJ_JS_ENTRY_SCRIPT" ]]; then
  # use sandboxes/shadow project as default
  BCLJ_JS_ENTRY_SCRIPT="sandbox.js"
fi
export BCLJ_JS_ENTRY_SCRIPT

if [[ -z "$BCLJ_JS_ASSETS_DIR" ]]; then
  # use sandboxes/shadow project as default
  BCLJ_JS_ASSETS_DIR=".compiled-sandbox"
fi
export BCLJ_JS_ASSETS_DIR

if [[ -z "$BCLJ_JS_ORIGIN_DIR" ]]; then
  # use sandboxes/shadow project as default
  BCLJ_JS_ORIGIN_DIR="$ROOT_DIR/sandboxes/shadow/public"
fi
export BCLJ_JS_ORIGIN_DIR

BLENDER_FILE=${2:-assets/blank.blend}

export ENABLE_BACKTRACE=1
export BCLJ_PACKAGES_DIR
if [[ -n "$BCLJ_LIVE_FILE" || -n "$BCLJ_HYLANG_NREPL" ]]; then
  export BCLJ_HY_SUPPORT=1
  export BCLJ_HYLIB_DIR=${BCLJ_HYLIB_DIR:-"$ROOT_DIR/sandboxes/hylang/hylib"}
fi

echo "BCLJ_BLENDER_PATH=$BCLJ_BLENDER_PATH"
echo "BCLJ_BLENDER_PYTHON_PATH=$BCLJ_BLENDER_PYTHON_PATH"
env | grep BCLJ_ | grep -v BCLJ_BLENDER_PATH | grep -v BCLJ_BLENDER_PYTHON_PATH | sort || true

if [[ -n "$BCLJ_BLENDER_WINDOW_PX" ]]; then
  if [[ "$OSTYPE" == "darwin"* ]]; then
    if [[ -z "$BCLJ_UNATTENDED" ]]; then
      # macOS window customization via applescript, note: it will ask for granting permissions on first run
      set -x
      (
        osascript "./scripts/blender-startup.applescript" \
          "$BCLJ_BLENDER_WINDOW_PX" \
          "$BCLJ_BLENDER_WINDOW_PY" \
          "$BCLJ_BLENDER_WINDOW_W" \
          "$BCLJ_BLENDER_WINDOW_H"
      ) &
      set +x
    fi
  else
    # on other systems --window-geometry should map to external displays properly, I believe
    BCLJ_BLENDER_OPTS="$BCLJ_BLENDER_OPTS --window-geometry $BCLJ_BLENDER_WINDOW_PX $BCLJ_BLENDER_WINDOW_PY $BCLJ_BLENDER_WINDOW_W $BCLJ_BLENDER_WINDOW_H"
  fi
fi

if [[ -n "$BCLJ_UNATTENDED" ]]; then
  BCLJ_BLENDER_OPTS="$BCLJ_BLENDER_OPTS -noaudio --python-exit-code 13"
fi

set -x
# shellcheck disable=SC2086
exec "$BCLJ_BLENDER_PATH" "$BLENDER_FILE" --python "$DRIVER_ENTRY_POINT" $BCLJ_BLENDER_OPTS "${BLENDER_CLI_OPTS[@]}"
