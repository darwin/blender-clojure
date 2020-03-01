# make sure you have run ./scripts/install-deps.sh or provide your custom BCLJ_MODULES_DIR
# we prepend our modules to sys paths to avoid picking
# any possibly existing outdated libs from blender
import os
import sys

this_dir = os.path.abspath(os.path.dirname(__file__))
root_dir = os.path.abspath(os.path.join(this_dir, "..", ".."))
modules_dir = os.environ.get("BCLJ_MODULES_DIR") or os.path.join(root_dir, "_modules")
hylib_dir = os.path.join(root_dir, "examples", "hylib")
sys.path.insert(0, modules_dir)
sys.path.insert(0, hylib_dir)
sys.path.insert(0, this_dir)

import core
