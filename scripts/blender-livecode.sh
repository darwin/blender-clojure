#!/usr/bin/env bash

if [[ $# -eq 0 ]]; then
  echo "Usage: $0 HYLANGFILE [BLENDFILE.blend]"
  echo "Watches HYLANGFILE for changes in Blender and reloads on save, frame change:"
  echo "e.g. $0 entry.hy"
  exit 1
fi

set -e -o pipefail
# shellcheck source=_config.sh
source "$(dirname "${BASH_SOURCE[0]}")/_config.sh"

ENTRY_FILE=$(get_absolute_path_of_existing_file "$1")

cd "$ROOT_DIR"

BLENDER_FILE=${2:-assets/blank.blend}

export HYLC_ENTRY_FILE="$ENTRY_FILE"

echo "HYLC_BLENDER_PATH=$HYLC_BLENDER_PATH"
echo "HYLC_BLENDER_PYTHON_PATH=$HYLC_BLENDER_PYTHON_PATH"
echo "HYLC_ENTRY_FILE=$HYLC_ENTRY_FILE"

set -x
exec "$HYLC_BLENDER_PATH" "$BLENDER_FILE" --python watcher/main.py
