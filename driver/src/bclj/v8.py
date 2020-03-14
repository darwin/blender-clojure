import logging
from functools import wraps

try:
    import STPyV8
except ModuleNotFoundError as e:
    print("Unable to import STPyV8 module")
    print("Please make sure you have compiled STPyV8 see ./scripts/build_v8.sh and readme")
    print()
    raise e

from STPyV8 import JSFunction, JSContext

logger = logging.getLogger(__name__)


def execute_callback(context, code, *args):
    assert (isinstance(code, JSFunction))
    with context as ctx:
        try:
            code(*args)
        except Exception:
            logger.exception("Unhandled exception while executing a callback", stack_info=True)
            return None


def report_exceptions(f):
    @wraps(f)
    def wrapper(*args, **kw):
        try:
            return f(*args, **kw)
        except Exception as e:
            logger.exception("Unhandled exception during a call to {} with args={} kwargs={}".format(f, args, kw),
                             stack_info=True)

    return wrapper


class JSClass(STPyV8.JSClass):

    def isPrototypeOf(self, obj):
        pass
