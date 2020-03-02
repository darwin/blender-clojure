#!/usr/bin/env bash

if [[ $1 == "-h" || $1 == "--help" ]]; then
  echo "Usage: $0 [LIVE_HYLANG_FILE] [BLENDFILE.blend]"
  echo "Watches LIVE_HYLANG_FILE for changes and executes it in Blender on save or frame change:"
  echo "e.g. $0 entry.hy"
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

if [[ -n "$1" ]]; then
  if [[ ! -f "$1" ]]; then
    echo "Specified live file does not exist: '$1'"
    exit 1
  fi
  LIVE_FILE=$(get_absolute_path_of_existing_file "$1")
  export BCLJ_LIVE_FILE="$LIVE_FILE"
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

echo "BCLJ_BLENDER_PATH=$BCLJ_BLENDER_PATH"
echo "BCLJ_BLENDER_PYTHON_PATH=$BCLJ_BLENDER_PYTHON_PATH"
env | grep BCLJ_ | grep -v BCLJ_BLENDER_PATH | grep -v BCLJ_BLENDER_PYTHON_PATH || true

set -x
exec "$BCLJ_BLENDER_PATH" "$BLENDER_FILE" --python "$DRIVER_ENTRY_POINT"
