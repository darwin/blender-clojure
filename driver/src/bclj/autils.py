import inspect
import logging
import asyncio
from functools import wraps
from threading import Thread

logger = logging.getLogger(__name__)


def async_loop_thread(loop):
    asyncio.set_event_loop(loop)
    loop.set_debug(True)
    logger.debug("Entering async loop {}".format(loop))
    loop.run_forever()


def start_async_loop(name):
    loop = asyncio.new_event_loop()
    t = Thread(target=async_loop_thread, args=(loop,))
    t.name = "{} [async loop]".format(name)
    t.daemon = True
    t.start()
    return loop


def wrap_coroutine_with_exceptions_reporting(coro, *args, **kwargs):
    @wraps(coro)
    async def coroutine_wrapper():
        try:
            return await coro(*args, **kwargs)
        except Exception:
            logger.exception("Unhandled exception in async call to {} with args={} kwargs={}".format(coro, args, kwargs),
                             stack_info=True)

    return coroutine_wrapper


def call_soon(loop, coro, *args, **kwargs):
    assert inspect.iscoroutinefunction(coro)

    def callback():
        wrapped_coro = wrap_coroutine_with_exceptions_reporting(coro, *args, **kwargs)
        asyncio.ensure_future(wrapped_coro(), loop=loop)

    loop.call_soon_threadsafe(callback)
