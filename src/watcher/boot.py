import sys
import logging

last_good_stdout = sys.stdout
last_good_stderr = sys.stderr


def install_unhandled_exceptions_handler():
    orig_excepthook = sys.excepthook

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


install_unhandled_exceptions_handler()

logger = logging.getLogger("hylc")
logger.setLevel(logging.DEBUG)

handler = logging.StreamHandler(sys.stdout)
handler.setLevel(logging.DEBUG)
formatter = logging.Formatter('%(name)s - %(levelname)s - %(message)s')
handler.setFormatter(formatter)
logger.addHandler(handler)

logger.info("log")
logger.debug("debug")
