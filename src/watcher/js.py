import inspect
import os
import sys
import thug
import bpy

import v8

this_dir = os.path.abspath(os.path.dirname(__file__))
root_dir = os.path.abspath(os.path.join(this_dir, "..", ".."))

# TODO: make this configurable
base_assets_path = os.path.join(root_dir, "projects", "shadow", "public", ".compiled")
entry_script = "sandbox.js"


def report_eval_error(e):
    print(e.stack)


def read_script(path):
    with open(path, encoding='utf-8') as f:
        return f.read()


class Console(v8.JSClass):

    @staticmethod
    def log(*args):
        print("log>", *args)

    @staticmethod
    def warn(*args):
        print("wrn>", *args)

    @staticmethod
    def error(*args):
        print("err>", *args)


def import_scripts(path):
    print("request to import '{}'".format(path))
    full_path = os.path.join(base_assets_path, path)
    code = read_script(full_path)
    js_eval(code, path)


root = thug.Window("http://localhost/watcher.js")
root.Node = thug.Node(root.doc)
root.hylcConsole = Console()
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
            print(e)


def bootstrap():
    js_eval("this.console = hylcConsole")
    js_eval('importScripts("{}")'.format(entry_script))
