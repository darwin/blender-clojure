#!/usr/bin/env bash

if [[ $1 == "-h" || $1 == "--help" ]]; then
  cat <<EOF
Usage: $0 [-o out/dir]

Options:
  -o/--out              Output directory for cljs files.
  -x/--xml              Directory with input xml files.

EOF
  exit 0
fi

# poor man's bash flags parsing
# https://stackoverflow.com/a/14203146/84283
POSITIONAL=()
while [[ $# -gt 0 ]]; do
  key="$1"

  case $key in
  -o | --out)
    shift
    BLCJ_GEN_CLJS_DIR=$1
    shift
    ;;
  -x | --xml)
    shift
    BCLJ_XML_DIR=$1
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

if [[ -z "$BCLJ_XML_DIR" ]]; then
  BCLJ_XML_DIR="$WORKSPACE_DIR/xml"
fi

BLCJ_GEN_CLJS_DIR=${$BLCJ_GEN_CLJS_DIR:-"$ROOT_DIR/bcljs/src/gen"}
TMP_GEN_DIR="$WORKSPACE_DIR/gen"

if [[ $# != "0" ]]; then
  if [[ -d "TMP_GEN_DIR" ]]; then
    rm -rf "$TMP_GEN_DIR"
  fi
fi
mkdir -p "$TMP_GEN_DIR"


set -x
cd apigen
pwd
time clj -A:cli -- --input "$BCLJ_XML_DIR" --output "$TMP_GEN_DIR" --logfile "$WORKSPACE_DIR/build-api-log.txt" "$@"

set +x

cd "$TOOLS_DIR"
if [[ ! -d "$BLCJ_GEN_CLJS_DIR" ]]; then
  mkdir -p "$BLCJ_GEN_CLJS_DIR"
fi

rsync -ar --delete --stats "$TMP_GEN_DIR/" "$BLCJ_GEN_CLJS_DIR"
