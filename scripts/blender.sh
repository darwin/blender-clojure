#!/usr/bin/env bash

if [[ $1 == "-h" || $1 == "--help" ]]; then
  echo "Usage: $0 [LIVE_HYLANG_FILE] [BLENDFILE.blend]"
  echo "Watches LIVE_HYLANG_FILE for changes and executes it in Blender on save or frame change:"
  echo "e.g. $0 entry.hy"
  exit 0
fi

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

BLENDER_FILE=${2:-assets/blank.blend}

export ENABLE_BACKTRACE=1

echo "BCLJ_BLENDER_PATH=$BCLJ_BLENDER_PATH"
echo "BCLJ_BLENDER_PYTHON_PATH=$BCLJ_BLENDER_PYTHON_PATH"
env | grep BCLJ_ | grep -v BCLJ_BLENDER_PATH | grep -v BCLJ_BLENDER_PYTHON_PATH

set -x
exec "$BCLJ_BLENDER_PATH" "$BLENDER_FILE" --python "$DRIVER_ENTRY_POINT"
