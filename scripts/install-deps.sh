#!/usr/bin/env bash

set -e -o pipefail
# shellcheck source=_config.sh
source "$(dirname "${BASH_SOURCE[0]}")/_config.sh"

BACKTRACE_DEP=git+https://github.com/erzoe/backtrace.git@7e58cff1a8584ec783ba92956e329465c27243d8

KNOWN_PYTHON_DEPS=(
  hy
  rply
  astor
  colorama
  funcparserlib
  beautifulsoup4
  six
  lxml
  websockets
  "$BACKTRACE_DEP"
)

cd "$ROOT_DIR"

set -x

# delete old venv
if [[ -d "$VENV_DIR" ]]; then
  rm -rf "$VENV_DIR"
fi

# create a new clean venv
python3 -m venv venv
source venv/bin/activate
pip3 install -U pip
pip3 install --upgrade "${KNOWN_PYTHON_DEPS[@]}"
