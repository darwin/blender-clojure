import os
import time
import sys
import hy
import bpy

import traceback
import backtrace

backtrace_opts = {
    'reverse': False,
    'align': True,
    'strip_path': True,
    'enable_on_envvar_only': True,
    'on_tty': True,
    'conservative': False,
    'styles': {}
}

from hy.errors import (_tb_hidden_modules)


def filter_hy_traceback(exc_traceback):
    # frame = (filename, line number, function name*, text)
    new_tb = []
    for frame in traceback.extract_tb(exc_traceback):
        if not (frame[0].replace('.pyc', '.py') in _tb_hidden_modules or
                os.path.dirname(frame[0]) in _tb_hidden_modules):
            new_tb += [frame]
    return new_tb


def present_hy_exception(exc_type, value, hy_traceback):
    filtered_traceback = filter_hy_traceback(hy_traceback)
    backtrace.hook(tpe=exc_type, value=value, tb=filtered_traceback, **backtrace_opts)


pending_session_jobs = []


def process_session_job(data, fut):
    result = data["session"].handle(data["msg"], data["request"])
    fut["result"] = result
    fut["resolved"] = True


def process_pending_session_jobs():
    while len(pending_session_jobs) > 0:
        work_item = pending_session_jobs.pop(0)
        process_session_job(work_item["data"], work_item["fut"])


def request_session_job(data):
    fut = {"resolved": False}
    pending_session_jobs.append({"data": data,
                                 "fut": fut})
    return fut


def handle_session_message(session, msg, request):
    fut = request_session_job({"session": session,
                               "msg": msg,
                               "request": request})

    # TODO: is there a better way how to wait for work to be completed on other thread?
    while fut["resolved"] is False:
        time.sleep(0.03)

    return fut["result"]


def describe_environment():
    import platform
    return "{appname} {version} using {py}({build}) {pyversion} on {os} in Blender {blender}".format(
        appname=hy.__appname__,
        version=hy.__version__,
        py=platform.python_implementation(),
        build=platform.python_build()[0],
        pyversion=platform.python_version(),
        os=platform.system(),
        blender=bpy.app.version_string)
