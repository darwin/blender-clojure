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
hylib_dir = os.path.join(root_dir, "examples", "hylib")
sys.path.insert(0, packages_dir)
sys.path.insert(0, hylib_dir)
sys.path.insert(0, this_dir)

import bclj
