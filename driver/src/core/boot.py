import copy
import sys
import logging


class LogFormatter(logging.Formatter):

    def format(self, record):
        rich_record = copy.copy(record)
        rich_record.shortlevelname = rich_record.levelname[0]
        return logging.Formatter.format(self, rich_record)


def init_logging():
    logger = logging.getLogger("bclj")
    # TODO: make this configurable
    logger.setLevel(logging.DEBUG)

    root_logger = logging.getLogger()
    formatter = LogFormatter('{shortlevelname}:{name} | {message}', style="{")
    handler = logging.StreamHandler(sys.stdout)
    handler.setLevel(logging.DEBUG)
    handler.setFormatter(formatter)
    root_logger.addHandler(handler)


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


init_logging()
install_unhandled_exceptions_handler()
