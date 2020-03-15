import asyncio
import os
import sys
import logging
import threading

from bclj import log, backtrace, autils

hy_enabled = os.environ.get("BCLJ_HY_SUPPORT") is not None

if hy_enabled:
    from hy.importer import runhy
    from bclj import hyrepl

hyrepl_enabled = hy_enabled and os.environ.get("BCLJ_HYLANG_NREPL") is not None

repl_logger = logging.getLogger("{} [HyREPL]".format(__name__))

logger = logging.getLogger(__name__)

if not hy_enabled:
    live_file_path = None
else:
    live_file_path = os.environ.get("BCLJ_LIVE_FILE")
    if live_file_path is not None:
        if not os.path.exists(live_file_path):
            logger.warning("live file '%s' does not exists" % live_file_path)

live_file_last_mtime = 0

hyrepl_server = None

assert threading.current_thread() is threading.main_thread()
main_loop = asyncio.get_event_loop()


async def process_session_message(session, msg, request):
    try:
        session.handle(msg, request)
    except Exception:
        logger.exception("Failed to handle HyREPL session message", stack_info=True)


def handle_session_message(session, msg, request):
    autils.call_soon(main_loop, process_session_message, session, msg, request)


def start_hyrepl():
    if hyrepl_enabled:
        global hyrepl_server

        hyrepl_server = hyrepl.start_server()


def stop_hyrepl():
    if hyrepl_enabled:
        global hyrepl_server

        if hyrepl_server is None:
            return None

        hyrepl.shutdown_server(hyrepl_server)
        hyrepl_server = None


def exec_hy_file(path):
    try:
        runhy.run_path(path, run_name='__main__')
    except Exception:
        backtrace.present_hy_exception(*sys.exc_info())


def run_hylang_file(path):
    logger.info("Reloading '{}' ".format(log.colorize_file(path)))
    exec_hy_file(path)
    logger.info("Done executing '{}'".format(path))


def has_live_file():
    return live_file_path is not None


def log_live_file_watching_start():
    if has_live_file():
        logger.info("Watching '{}' for changes and re-loading.".format(log.colorize_file(live_file_path)))


def log_live_file_watching_stop():
    if has_live_file():
        logger.info("Finished watching '{}'".format(log.colorize_file(live_file_path)))


def run_live_file():
    if has_live_file():
        run_hylang_file(live_file_path)


def check_live_file():
    if has_live_file():
        global live_file_last_mtime
        if os.path.exists(live_file_path):
            stat = os.stat(live_file_path)
            if stat.st_mtime > live_file_last_mtime:
                live_file_last_mtime = stat.st_mtime
                run_hylang_file(live_file_path)
