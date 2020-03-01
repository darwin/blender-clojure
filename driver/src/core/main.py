import boot
import os
import sys
import time
import worker

import hy
from hy.importer import runhy

import hylc.backtrace
import hylc.env_info
import hylc.jobs
import repl

# import blender
import bpy

nrepl_enabled = os.environ.get("BCLJ_HYLANG_NREPL")
nrepl_server = None

import js

live_file_path = os.environ.get("BCLJ_LIVE_FILE")
if live_file_path is None:
    raise Exception("BCLJ_LIVE_FILE not specified")

if not os.path.exists(live_file_path):
    print("WARNING: watched file '%s' does not exists" % live_file_path)


def exec_hy_file(path):
    try:
        runhy.run_path(path, run_name='__main__')
    except:
        hylc.backtrace.present_hy_exception(*sys.exc_info())


def run_hylang_file(path):
    print("Reloading '%s' " % path)
    exec_hy_file(path)
    print("Done executing '%s'" % path)


class ModalTimerOperator(bpy.types.Operator):
    """Operator which runs its self from a timer"""
    bl_idname = "wm.modal_timer_operator"
    bl_label = "Modal Timer Operator"
    last_check = 0
    watched_file_path = live_file_path

    _timer = None

    def modal(self, _context, event):
        if event.type == 'TIMER':
            worker.drain_asyncio_event_loop()
            if nrepl_enabled is not None:
                hylc.jobs.process_pending_session_jobs()
            path = self.watched_file_path
            if os.path.exists(path):
                statbuf = os.stat(path)
                if statbuf.st_mtime > self.last_check:
                    self.last_check = statbuf.st_mtime
                    run_hylang_file(path)

        return {'PASS_THROUGH'}

    def execute(self, context):
        wm = context.window_manager
        self._timer = wm.event_timer_add(0.1, window=context.window)
        wm.modal_handler_add(self)
        print("Watching '%s' for changes and re-loading." % self.watched_file_path)
        return {'RUNNING_MODAL'}

    def cancel(self, context):
        wm = context.window_manager
        wm.event_timer_remove(self._timer)
        print("Finished watching '%s'" % self.watched_file_path)


def frame_change_handler(_scene):
    run_hylang_file(live_file_path)


def register():
    bpy.utils.register_class(ModalTimerOperator)
    bpy.app.handlers.frame_change_post.append(frame_change_handler)


def unregister():
    bpy.utils.unregister_class(ModalTimerOperator)
    bpy.app.handlers.frame_change_post.remove(frame_change_handler)


def start_nrepl():
    global nrepl_server

    if nrepl_enabled is None:
        return None

    nrepl_server = repl.start_server()
    print()


def stop_nrepl():
    global nrepl_server

    if nrepl_enabled is None:
        return None

    if nrepl_server is None:
        return None

    repl.shutdown_server(nrepl_server)
    nrepl_server = None


def print_welcome():
    print()
    print("==== hylc watcher =====")
    print(hylc.env_info.describe_environment())


def start():
    print_welcome()
    start_nrepl()
    register()
    # test call
    bpy.ops.wm.modal_timer_operator()
    js.bootstrap()
