import copy
import os
import sys

from bclj import log


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
            sys.stdout.flush()
            sys.stderr.flush()
            sys.exit(1)
        else:
            orig_excepthook(exc_type, exc_value, exc_traceback)

    sys.excepthook = handle_unhandled_exceptions


def init():
    log.init()
    install_unhandled_exceptions_handler()
