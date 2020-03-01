import inspect
import os
import sys
import thug
import bpy
import logging
import v8
import log

this_dir = os.path.abspath(os.path.dirname(__file__))
root_dir = os.path.abspath(os.path.join(this_dir, "..", "..", ".."))

# TODO: make this configurable
base_assets_path = os.path.join(root_dir, "sandboxes", "shadow", "public", ".compiled")
entry_script = "sandbox.js"

logger = logging.getLogger("bclj.js")


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


def import_scripts(path):
    full_path = os.path.join(base_assets_path, path)
    logger.debug("request to import '{}'".format(log.colorize_file(full_path)))
    code = read_script(full_path)
    js_eval(code, path)


root = thug.Window("http://localhost/watcher.js")
root.Node = thug.Node(root.doc)
root.foreignConsole = Console()
root.importScripts = import_scripts
root.reportEvalError = report_eval_error
root.bpy = sys.modules['bpy']
root.window = root


def wrap_code(js):
    return "try{" + js + "} catch (e) { reportEvalError(e) }"


def js_eval(js, name=""):
    with root.context as ctxt:
        try:
            code = wrap_code(js)
            return ctxt.eval(code, name)
        except Exception as e:
            logger.error(e)


def bootstrap():
    js_eval("this.console = foreignConsole")
    js_eval('importScripts("{}")'.format(entry_script))
