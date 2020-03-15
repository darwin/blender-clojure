import os

import logging
from bclj import jobs, env_info, log, worker, js

import bpy  # import blender

logger = logging.getLogger(__name__)

from bclj import hy


class ModalTimerOperator(bpy.types.Operator):
    """Operator which runs its self from a timer"""
    bl_idname = "wm.modal_timer_operator"
    bl_label = "Modal Timer Operator"

    _timer = None

    def modal(self, _context, event):
        if event.type == 'TIMER':
            worker.drain_asyncio_event_loop()
            jobs.process_pending_session_jobs()
            hy.check_live_file()
        return {'PASS_THROUGH'}

    def execute(self, context):
        wm = context.window_manager
        self._timer = wm.event_timer_add(0.1, window=context.window)
        wm.modal_handler_add(self)
        hy.log_live_file_watching_start()
        return {'RUNNING_MODAL'}

    def cancel(self, context):
        wm = context.window_manager
        wm.event_timer_remove(self._timer)
        hy.log_live_file_watching_stop()


def frame_change_handler(_scene):
    hy.run_live_file()


def register():
    bpy.utils.register_class(ModalTimerOperator)
    bpy.app.handlers.frame_change_post.append(frame_change_handler)


def unregister():
    bpy.utils.unregister_class(ModalTimerOperator)
    bpy.app.handlers.frame_change_post.remove(frame_change_handler)


def print_welcome():
    print(env_info.describe_environment())


def start():
    print_welcome()
    hy.start_hyrepl()
    register()
    # test call
    bpy.ops.wm.modal_timer_operator()
    js.bootstrap()
