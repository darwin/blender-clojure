import asyncio
import websockets
import logging
from bclj import v8

logger = logging.getLogger('bclj.websockets')

from threading import Thread

server_loop: asyncio.AbstractEventLoop


def server_loop_thread(loop):
    asyncio.set_event_loop(loop)
    loop.set_debug(True)
    logger.debug("Entering websockets server loop {}".format(loop))
    loop.run_forever()


def start_server_loop():
    loop = asyncio.new_event_loop()
    t = Thread(target=server_loop_thread, args=(loop,))
    t.name = "bclj.ws-asyncio"
    t.daemon = True
    t.start()
    return loop


def start_server_loop_if_needed():
    if "server_loop" not in globals():
        global server_loop
        server_loop = start_server_loop()


class Event(v8.JSClass):

    def __init__(self):
        super().__init__()


class MessageEvent(Event):

    def __init__(self, data):
        super().__init__()
        self.data = data


def abbreviate_message_for_log(msg):
    if len(msg) > 300:
        return msg[:300] + "...}"
    else:
        return msg


# note that this is not full WebSocket implementation,
# we implement only what is currently needed for shadow-cljs to work
class WebSocket(object):
    READY_STATE_CONNECTING = 0
    READY_STATE_OPEN = 1
    READY_STATE_CLOSING = 2
    READY_STATE_CLOSED = 3

    async def _run_client_loop(self):
        self._change_ready_state(self.READY_STATE_CONNECTING)
        logger.info("Connecting via websockets to '{}'".format(self.url))
        async with websockets.connect(self.url) as ws:
            logger.debug("client_loop: entering receive loop... {}".format(ws))
            self.ws = ws
            self._change_ready_state(self.READY_STATE_OPEN)
            try:
                while True:
                    msg = await ws.recv()
                    logger.debug("client_loop: got message len={}\n<< {}".format(len(msg), abbreviate_message_for_log(msg)))
                    self._trigger_handler("onmessage", MessageEvent(msg))
            except Exception as e:
                logger.error("client_loop: ran into problems {}".format(e))
                event = Event()
                event.message = str(e)
                self._trigger_handler("onerror", event)
            self.ws = None
            logger.debug("client_loop: leaving...")

    async def _trigger_handler_async(self, handler_name, *args):
        logger.debug("triggering handler {} with args={}".format(handler_name, args))
        handler = getattr(self, handler_name, None)
        if handler is not None:
            return v8.execute_callback(self._window.context, handler, *args)

    def _trigger_handler(self, handler_name, *args):
        asyncio.run_coroutine_threadsafe(self._trigger_handler_async(handler_name, *args), self._main_loop)

    async def _send_message(self, msg):
        return await self._ws.send(msg)

    def _change_ready_state(self, new_state):
        self.readyState = new_state
        if new_state is self.READY_STATE_OPEN:
            self._trigger_handler("onopen", Event())
        elif new_state is self.READY_STATE_CLOSED:
            self._trigger_handler("onclose", Event())

    @v8.report_exceptions
    def __init__(self, url, protocols=None):
        assert (protocols is None)
        start_server_loop_if_needed()

        self._main_loop = asyncio.get_event_loop()
        # noinspection PyUnresolvedReferences
        self._window = self.__class__.window
        self._ws = None

        self.protocol = ""
        self.readyState = self.READY_STATE_CONNECTING
        self.url = url
        self.onmessage = None
        self.onopen = None
        self.onclose = None
        self.onerror = None

        def start_client_loop():
            asyncio.ensure_future(self._run_client_loop())

        server_loop.call_soon_threadsafe(start_client_loop)

    @v8.report_exceptions
    def send(self, msg, *_):
        logger.debug("send msg={}".format(abbreviate_message_for_log(msg)))
        asyncio.run_coroutine_threadsafe(self._send_message(msg), server_loop)

    @v8.report_exceptions
    def close(self, code=None, reason=None, *_):
        logger.debug("close code={} reason={}".format(code, reason))
        self.ws.close()
        self._change_ready_state(self.READY_STATE_CLOSED)
