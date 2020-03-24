#!/usr/bin/env bash

if [[ $1 == "-h" || $1 == "--help" ]]; then
  cat <<EOF
Usage: $0 [-b /path/to/blender][-r /path/to/blender-checkout][-o output/dir]

Options:
  -b/--binary           Blender binary path.
  -r/--repo             Directory with blender checkout.
  -o/--out              Output directory for xml files.
  -t/--tmp              Temporary directory.

EOF
  exit 0
fi

# poor man's bash flags parsing
# https://stackoverflow.com/a/14203146/84283
POSITIONAL=()
while [[ $# -gt 0 ]]; do
  key="$1"

  case $key in
  -b | --binary)
    shift
    BLENDER_BINARY_PATH=$1
    shift
    ;;
  -r | --repo)
    shift
    BLENDER_REPO_DIR=$1
    shift
    ;;
  -o | --out)
    shift
    BCLJ_XML_DIR=$1
    shift
    ;;
  -t | --tmp)
    shift
    BCLJ_TMP_DIR=$1
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

cd "$TOOLS_DIR"

if [[ -z "$BLENDER_BINARY_PATH" ]]; then
  echo "please specify --binary option or provide BLENDER_BINARY_PATH env variable"
  exit 1
fi

if [[ ! -x "$BLENDER_BINARY_PATH" ]]; then
  echo "blender executable does not seem to exist at '$BLENDER_BINARY_PATH'"
  exit 1
fi

if [[ -z "$BLENDER_REPO_DIR" ]]; then
  echo "please specify --repo option or provide BLENDER_REPO_DIR env variable"
  exit 1
fi

if [[ ! -d "$BLENDER_REPO_DIR" ]]; then
  echo "blender repo directory does not seem to exist at '$BLENDER_REPO_DIR'"
  exit 1
fi

if [[ -z "$BCLJ_XML_DIR" ]]; then
  BCLJ_XML_DIR="$WORKSPACE_DIR/xml"
fi

if [[ -z "$BCLJ_TMP_DIR" ]]; then
  BCLJ_TMP_DIR="$WORKSPACE_DIR/_tmp"
fi

PY_API_DIR="$BCLJ_TMP_DIR/python_api"

if [[ -d "$PY_API_DIR" ]]; then
  rm -rf "$PY_API_DIR"
fi
mkdir -p "$PY_API_DIR"

if [[ -d "$BCLJ_XML_DIR" ]]; then
  rm -rf "$BCLJ_XML_DIR"
fi
mkdir -p "$BCLJ_XML_DIR"

set -x

cd "$BLENDER_REPO_DIR"
"$BLENDER_BINARY_PATH" --background --factory-startup -noaudio --python doc/python_api/sphinx_doc_gen.py -- \
  --output "$PY_API_DIR"

sphinx-build -b xml "$PY_API_DIR/sphinx-in" "$BCLJ_XML_DIR"
