#!/usr/bin/env bash

set -e -o pipefail
# shellcheck source=_config.sh
source "$(dirname "${BASH_SOURCE[0]}")/_config.sh"

cd "$ROOT_DIR"

set -x
cd "sandboxes/shadow"
shadow-cljs compile tests

cd "$ROOT_DIR"
./scripts/blender.sh --assets ".compiled-tests" --entry "tests.js" --unattended
