import copy
import sys

from bclj import log, os


def install_unhandled_exceptions_handler():
    orig_excepthook = sys.excepthook
    last_good_stdout = sys.stdout
    last_good_stderr = sys.stderr

    def handle_unhandled_exceptions(exc_type, exc_value, exc_traceback):
        if exc_type is KeyboardInterrupt:
            # hard exit
            sys.stdout = last_good_stdout
            sys.stderr = last_good_stderr
            print("got KeyboardInterrupt")
            # stop_nrepl()
            os.brutal_exit(42)
        else:
            orig_excepthook(exc_type, exc_value, exc_traceback)

    sys.excepthook = handle_unhandled_exceptions


def init():
    log.init()
    install_unhandled_exceptions_handler()
