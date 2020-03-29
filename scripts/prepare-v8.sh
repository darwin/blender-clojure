#!/usr/bin/env bash

set -e -o pipefail
# shellcheck source=_config.sh
source "$(dirname "${BASH_SOURCE[0]}")/_config.sh"

DO_CLONE=1
DO_BREW=1
DO_V8=1
DO_STPYV8=1

STPYV8_EXTRA_OPTS=()

# poor man's bash flags parsing
# https://stackoverflow.com/a/14203146/84283
POSITIONAL_OPTS=()
while [[ $# -gt 0 ]]; do
  key="$1"

  case $key in
  -d | --debug)
    ENABLE_DEBUG=1
    STPYV8_EXTRA_OPTS+=("--debug")
    shift
    ;;
  --only-stpyv8)
    DO_CLONE=
    DO_BREW=
    DO_V8=
    DO_STPYV8=1
    shift
    ;;
  *) # unknown option
    POSITIONAL_OPTS+=("$1")
    shift
    ;;
  esac
done
set -- "${POSITIONAL_OPTS[@]}" # restore positional parameters

if [[ ! "$OSTYPE" == "darwin"* ]]; then
  echo "this v8 build script was tested under macOS only"
  echo "you will need to follow https://github.com/area1/stpyv8#building for your particular system"
  echo "you should end up '$VENV_PACKAGES_DIR' containing something like:"
  echo ""
  cat <<HEREDOC
> cd .build/stpyv8 && tree -ughsqD build/lib*
build/lib.macosx-10.15-x86_64-3.7
├── [darwin   staff    8.1K Mar  1 23:08]  STPyV8.py
└── [darwin   staff     26M Mar  2 01:19]  _STPyV8.cpython-37m-darwin.so

0 directories, 2 files
HEREDOC
  exit 1
fi

cd "$ROOT"

if [[ ! -d "$VENV_PACKAGES_DIR" ]]; then
  echo "directory '$VENV_PACKAGES_DIR' does not exist, you must successfully run ./scripts/install-deps.sh first"
  exit 1
fi

set -x
mkdir -p "$BUILD_DIR"
cd "$BUILD_DIR"

if [[ -n "$DO_CLONE" ]]; then
  if [[ -d "stpyv8" ]]; then
    cd stpyv8
    git pull
    cd ..
  else
    git clone https://github.com/area1/stpyv8.git
  fi
fi

cd stpyv8

unset V8_HOME

if [[ -n "$DO_BREW" ]]; then
  brew install python@2 python3 boost-python3
fi

if [[ -n "$DO_V8" ]]; then
  # force homebrew's python2 when working with depot
  PREV_PATH=$PATH
  export PATH=/usr/local/opt/python@2/bin:$PATH
  python2 setup.py v8
  export PATH=$PREV_PATH
fi


if [[ -n "$DO_STPYV8" ]]; then
  python3 -m venv env
  source env/bin/activate
  python3 setup.py stpyv8 "${STPYV8_EXTRA_OPTS[@]}"
fi

V8_PACKAGES_DIR_DIR="$VENV_PACKAGES_DIR"

# this is broken on my machine
# python setup.py install --prefix "$V8_PACKAGES_DIR_DIR"

if command -v tree; then
  tree -ughsqD build/lib*
fi

cp build/lib*/* "$V8_PACKAGES_DIR_DIR"
