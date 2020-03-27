print()
print("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~")
print("Starting blender-clojure driver...")

# make sure you have run ./scripts/install-deps.sh
# we prepend our modules to sys paths to avoid picking
# any possibly existing outdated libs from blender
import os
import sys

this_dir = os.path.abspath(os.path.dirname(__file__))
root_dir = os.path.abspath(os.path.join(this_dir, "..", ".."))
packages_dir = os.environ.get("BCLJ_PACKAGES_DIR")
if packages_dir is None:
    raise Exception("fatal: BCLJ_PACKAGES_DIR env variable is not set")

sys.path.insert(0, packages_dir)

hy_support = os.environ.get("BCLJ_HY_SUPPORT")
if hy_support is not None:
    hylib_dir = os.environ.get("BCLJ_HYLIB_DIR")
    if hylib_dir is None:
        raise Exception("fatal: BCLJ_HYLIB_DIR env variable is not set")
    sys.path.insert(0, hylib_dir)

sys.path.insert(0, this_dir)

import bclj
