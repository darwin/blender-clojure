import os
import sys
import logging
from bclj import log, v8, thug

this_dir = os.path.abspath(os.path.dirname(__file__))
root_dir = os.path.abspath(os.path.join(this_dir, "..", "..", ".."))

# TODO: make this configurable
public_path = os.path.join(root_dir, "sandboxes", "shadow", "public")
compiled_assets_path = os.path.join(public_path, ".compiled")
entry_script = "sandbox.js"

logger = logging.getLogger(__name__)

previous_root = None
current_root = None


def report_eval_error(e):
    logger.error(e.stack)


def read_script(path):
    with open(path, encoding='utf-8') as f:
        return f.read()


class Console(v8.JSClass):

    @staticmethod
    def log(*args):
        print(log.colorize_info("console.log"), *args)

    @staticmethod
    def warn(*args):
        print(log.colorize_warning("console.warn"), *args)

    @staticmethod
    def error(*args):
        print(log.colorize_error("console.error"), *args)


class BCLJ(v8.JSClass):

    @staticmethod
    def pycall(f, pos_args, map_args):
        if map_args is not None:
            f(*pos_args, **map_args)
        else:
            if pos_args is not None and len(pos_args) > 0:
                f(*pos_args)
            else:
                f()


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
    root.bclj = BCLJ()
    root.bpy = sys.modules['bpy']
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
    js_eval("window.location.origin = \"{}\"".format(public_path))
    js_eval('importScripts("{}")'.format(entry_script))


def reload_page():
    print(log.colorize_js("===== page reload ====="))
    global current_root
    global previous_root
    previous_root = current_root
    bootstrap()
