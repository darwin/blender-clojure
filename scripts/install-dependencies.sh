#!/usr/bin/env bash

set -e -o pipefail
# shellcheck source=_config.sh
source "$(dirname "${BASH_SOURCE[0]}")/_config.sh"

cd "$ROOT_DIR"

set -x
cd "$HYLC_BLENDER_PYTHON_PATH"

# this worked for me on macOS 10.15.4, blender 2.82
bin/python3.7m -m ensurepip
bin/python3.7m -m pip install -U pip
bin/pip3 install hy rply astor clint colorama funcparserlib -t "$ROOT_DIR/watcher/modules"
