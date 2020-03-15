import logging
import bpy  # import blender

from bclj import worker, hy

logger = logging.getLogger(__name__)


# noinspection PyMethodMayBeStatic
class ModalTimerOperator(bpy.types.Operator):
    """Operator which runs its self from a timer, we use it to implement our event loop"""
    bl_idname = "wm.modal_timer_operator"
    bl_label = "BCLJ Event Loop Operator"

    _timer = None

    def modal(self, _context, event):
        if event.type == 'TIMER':
            worker.drain_asyncio_event_loop()
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
    # this is important to instantiate our operator and kick off the timer
    bpy.ops.wm.modal_timer_operator()


def unregister():
    bpy.utils.unregister_class(ModalTimerOperator)
    bpy.app.handlers.frame_change_post.remove(frame_change_handler)
