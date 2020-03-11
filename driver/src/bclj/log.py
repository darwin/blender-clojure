import copy
import os
import re
import sys
import logging
import colorama

colorize_output = True


def colorize(color, s):
    if colorize_output and s:
        return "{}{}{}".format(color, s, colorama.Fore.RESET)
    else:
        return ""


def colorize_red(s):
    return colorize(colorama.Fore.RED, s)


def colorize_yellow(s):
    return colorize(colorama.Fore.YELLOW, s)


def colorize_blue(s):
    return colorize(colorama.Fore.BLUE, s)


def colorize_gray(s):
    return colorize(colorama.Fore.BLACK, s)


def colorize_file(s):
    return colorize(colorama.Fore.CYAN, s)


def colorize_url(s):
    return colorize(colorama.Fore.CYAN, s)


def colorize_js(s):
    return colorize(colorama.Fore.MAGENTA, s)


def massage_log_name(name):
    return re.sub('^bclj.', "~", name)


colorize_error = colorize_red
colorize_info = colorize_blue
colorize_warning = colorize_yellow


class LogFormatter(logging.Formatter):

    def __init__(self):
        logging.Formatter.__init__(self, '{threadName}: {shortname} | {message}', style="{")

    def format_extra(self, record):
        s = ""
        if record.exc_info:
            # Cache the traceback text to avoid converting it multiple times
            # (it's constant anyway)
            if not record.exc_text:
                record.exc_text = self.formatException(record.exc_info)
        if record.exc_text:
            if s[-1:] != "\n":
                s = s + "\n"
            s = s + record.exc_text
        if record.stack_info:
            if s[-1:] != "\n":
                s = s + "\n"
            s = s + self.formatStack(record.stack_info)
        return s

    def format(self, record):
        message = record.getMessage() + colorize_gray(self.format_extra(record))
        if record.levelno >= logging.ERROR:
            return colorize_error(message)
        elif record.levelno >= logging.WARN:
            return colorize_warning(message)
        elif record.levelno >= logging.INFO:
            return colorize_info(message)
        else:
            rich_record = copy.copy(record)
            rich_record.shortlevelname = rich_record.levelname[0]
            rich_record.shortname = massage_log_name(rich_record.name)
            return colorize_gray(logging.Formatter.format(self, rich_record))


def init():
    global colorize_output
    is_tty = sys.stdout.isatty()
    no_color = os.environ.get("BCLJ_NO_COLOR")
    colorize_output = is_tty and no_color is None

    logger = logging.getLogger("bclj")
    debug = os.environ.get("BCLJ_DEBUG")
    if debug is None:
        logger.setLevel(logging.INFO)
    else:
        logger.setLevel(logging.DEBUG)

    root_logger = logging.getLogger()
    handler = logging.StreamHandler(sys.stdout)
    handler.setLevel(logging.DEBUG)
    handler.setFormatter(LogFormatter())
    root_logger.addHandler(handler)
