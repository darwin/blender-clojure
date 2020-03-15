#!/usr/bin/env bash

if [[ $1 == "-h" || $1 == "--help" ]]; then
  cat <<EOF
Usage: $0 [-d][-y][-l live.hy] [scene.blend]

Options:
  -d/--debug              Enable verbose debug printing.
  -l/--hylive file.hy     Watches hy file for changes and executes it in Blender on save or frame change.
  -y/--hyrepl             Open hylang nREPL server upon startup.

EOF
  exit 0
fi

# poor man's bash flags parsing
# https://stackoverflow.com/a/14203146/84283
POSITIONAL=()
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
  *) # unknown option
    POSITIONAL+=("$1") # save it in an array for later
    shift              # past argument
    ;;
  esac
done
set -- "${POSITIONAL[@]}" # restore positional parameters

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

BLENDER_FILE=${2:-assets/blank.blend}

export ENABLE_BACKTRACE=1
export BCLJ_PACKAGES_DIR
if [[ -n "$BCLJ_LIVE_FILE" || -n "$BCLJ_HYLANG_NREPL" ]]; then
  export BCLJ_HY_SUPPORT=1
fi

echo "BCLJ_BLENDER_PATH=$BCLJ_BLENDER_PATH"
echo "BCLJ_BLENDER_PYTHON_PATH=$BCLJ_BLENDER_PYTHON_PATH"
env | grep BCLJ_ | grep -v BCLJ_BLENDER_PATH | grep -v BCLJ_BLENDER_PYTHON_PATH || true

set -x
exec "$BCLJ_BLENDER_PATH" "$BLENDER_FILE" --python "$DRIVER_ENTRY_POINT"
