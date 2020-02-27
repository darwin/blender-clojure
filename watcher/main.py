import os
import sys
import traceback

# make sure you have run ./scripts/install-dependencies.sh
# we prepend our modules to sys paths to avoid picking
# any possibly existing outdated libs from blender
this_dir = os.path.abspath(os.path.dirname(__file__))
modules_dir = os.path.join(this_dir, "modules")
sys.path.insert(0, modules_dir)
sys.path.insert(0, this_dir)

import hy
from hy.importer import runhy
from hy.errors import (filtered_hy_exceptions, hy_exc_handler)

# import blender
import bpy

entry = os.environ.get("HYLC_ENTRY_FILE", "entry.hy")
if not os.path.exists(entry):
    print("WARNING: watched file '%s' does not exists" % entry)


def run_hylang_file(path):
    print("Reloading '%s' " % path)
    try:
        with filtered_hy_exceptions():
            runhy.run_path(path, run_name='__main__')
    except:
        hy_exc_handler(*sys.exc_info())

    print(entry + " Done")


class ModalTimerOperator(bpy.types.Operator):
    """Operator which runs its self from a timer"""
    bl_idname = "wm.modal_timer_operator"
    bl_label = "Modal Timer Operator"
    last_check = 0

    _timer = None

    def modal(self, _context, event):
        if event.type == 'TIMER':
            if os.path.exists(entry):
                statbuf = os.stat(entry)
                if statbuf.st_mtime > self.last_check:
                    self.last_check = statbuf.st_mtime
                    run_hylang_file(entry)

        return {'PASS_THROUGH'}

    def execute(self, context):
        wm = context.window_manager
        self._timer = wm.event_timer_add(0.1, window=context.window)
        wm.modal_handler_add(self)
        print("Watching " + entry + " for changes and re-loading.")
        return {'RUNNING_MODAL'}

    def cancel(self, context):
        wm = context.window_manager
        wm.event_timer_remove(self._timer)
        print("Finished watching " + entry)


def frame_change_handler(_scene):
    run_hylang_file(entry)


def register():
    bpy.utils.register_class(ModalTimerOperator)
    bpy.app.handlers.frame_change_post.append(frame_change_handler)


def unregister():
    bpy.utils.unregister_class(ModalTimerOperator)
    bpy.app.handlers.frame_change_post.remove(frame_change_handler)


if __name__ == "__main__":
    register()
    # test call
    bpy.ops.wm.modal_timer_operator()
