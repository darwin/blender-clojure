import os
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
