#!/usr/bin/env bash

set -e -o pipefail
# shellcheck source=_config.sh
source "$(dirname "${BASH_SOURCE[0]}")/_config.sh"

if [[ ! "$OSTYPE" == "darwin"* ]]; then
  echo "this v8 build script was tested under macOS only"
  echo "you will need to follow https://github.com/area1/stpyv8#building for your particular system"
  echo "you should end up with _modules_v8 in root folder containing something like:"
  echo ""
  cat <<HEREDOC
> tree _modules_v8/
_modules_v8/
├── STPyV8.py
└── _STPyV8.cpython-37m-darwin.so

0 directories, 2 files
HEREDOC
  exit 1
fi

cd "$ROOT"

set -x
mkdir -p "$BUILD_DIR"
cd "$BUILD_DIR"

if [[ -d "stpyv8" ]]; then
  cd stpyv8
  git pull
  cd ..
else
  git clone https://github.com/area1/stpyv8.git
fi

cd stpyv8

unset V8_HOME

brew install python@2 python3 boost-python3

# force homebrew's python2 when working with depot
PREV_PATH=$PATH
export PATH=/usr/local/opt/python@2/bin:$PATH
python2 setup.py v8
export PATH=$PREV_PATH

python3 -m venv env
source env/bin/activate
python3 setup.py stpyv8

V8_MODULES_DIR="$ROOT_DIR/_modules_v8"

# this is broken on my machine
# python setup.py install --prefix "$V8_MODULES_DIR"

# let's do it manually
if [[ -d "$V8_MODULES_DIR" ]]; then
  rm -rf "$V8_MODULES_DIR"
fi

mkdir -p "$V8_MODULES_DIR"
cp build/lib*/* "$V8_MODULES_DIR"

if command -v tree; then
  tree -ughsqD "$V8_MODULES_DIR"
fi
