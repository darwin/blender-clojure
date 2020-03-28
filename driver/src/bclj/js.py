import os
import sys
import logging
from bclj import log, v8, thug
import mathutils
import inspect

this_dir = os.path.abspath(os.path.dirname(__file__))
root_dir = os.path.abspath(os.path.join(this_dir, "..", "..", ".."))

# TODO: make this configurable
origin_dir = os.environ.get("BCLJ_JS_ORIGIN_DIR")
if origin_dir is None:
    raise Exception("fatal: BCLJ_JS_ORIGIN_DIR env variable is not set")

assets_dir = os.environ.get("BCLJ_JS_ASSETS_DIR")
if assets_dir is None:
    raise Exception("fatal: BCLJ_JS_ASSETS_DIR env variable is not set")

compiled_assets_path = os.path.join(origin_dir, assets_dir)

entry_script = os.environ.get("BCLJ_JS_ENTRY_SCRIPT")
if entry_script is None:
    raise Exception("fatal: BCLJ_JS_ENTRY_SCRIPT env variable is not set")

logger = logging.getLogger(__name__)

previous_root = None
current_root = None


def report_eval_error(e):
    logger.error(e.stack)


def read_script(path):
    with open(path, encoding='utf-8') as f:
        return f.read()


def indent_args(args):
    new_args = []
    for arg in args:
        if isinstance(arg, str):
            new_arg = arg.replace("\n", "\n            ")
            new_args.append(new_arg)
        else:
            new_args.append(arg)

    return new_args


def process_args_for_test_printing(args, style='out'):
    new_args = []
    for arg in args:
        if isinstance(arg, str):
            if style is 'err':
                new_arg = log.colorize_test_error(arg)
            else:
                new_arg = log.colorize_test(arg)
            new_args.append(new_arg)
        else:
            new_args.append(arg)

    return new_args


class Console(v8.JSClass):

    @staticmethod
    def log(*args):
        print(log.colorize_info("console.log"), *indent_args(args))

    @staticmethod
    def warn(*args):
        print(log.colorize_warning("console.wrn"), *indent_args(args))

    @staticmethod
    def error(*args):
        print(log.colorize_error("console.err"), *indent_args(args))


class BCLJ(v8.JSClass):

    @staticmethod
    def test_runner_print(*args):
        print(*process_args_for_test_printing(args), end='')

    def test_runner_print_err(*args):
        print(*process_args_for_test_printing(args, style='err'), end='')

    @staticmethod
    def pycall(f, pos_args, map_args):
        if map_args is not None:
            return f(*pos_args, **map_args)
        else:
            if pos_args is not None and len(pos_args) > 0:
                return f(*pos_args)
            else:
                return f()

    @staticmethod
    def repr(o):
        return repr(o)

    @staticmethod
    def len(o):
        return len(o)

    @staticmethod
    def mro(o):
        return o.__mro__


def import_scripts(path):
    full_path = os.path.join(compiled_assets_path, path)
    logger.debug("request to import '{}'".format(log.colorize_file(full_path)))
    code = read_script(full_path)
    js_eval(code, path)


def create_root():
    root = thug.Window("http://localhost/watcher.js")
    root.Node = thug.Node(root.doc)
    root.foreignConsole = Console()
    root.importScripts = import_scripts
    root.reportEvalError = report_eval_error
    assert sys.modules['bpy']
    assert sys.modules['mathutils']
    root.bpy = sys.modules['bpy']
    root.mathutils = sys.modules['mathutils']
    assert sys.modules['inspect']
    root.inspect = sys.modules['inspect']
    root.bclj = BCLJ()
    root.window = root
    return root


def wrap_code(js):
    return "try{" + js + "} catch (e) { reportEvalError(e) }"


def js_eval(js, name=""):
    with current_root.context as ctxt:
        try:
            code = wrap_code(js)
            return ctxt.eval(code, name)
        except Exception as e:
            logger.error(e)


def bootstrap():
    global current_root
    current_root = create_root()
    js_eval("this.console = foreignConsole")
    js_eval("window.location.origin = \"{}\"".format(origin_dir))
    js_eval('importScripts("{}")'.format(entry_script))


def reload_page():
    print(log.colorize_js("===== page reload ====="))
    global current_root
    global previous_root
    previous_root = current_root
    bootstrap()
