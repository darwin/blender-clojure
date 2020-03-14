#!/usr/bin/env bash

set -e -o pipefail
# shellcheck source=_config.sh
source "$(dirname "${BASH_SOURCE[0]}")/_config.sh"

cd "$ROOT_DIR"

if [[ ! -d "$VENV_DIR" ]]; then
  # create a new clean venv
  python3 -m venv "$VENV_DIR"
fi
