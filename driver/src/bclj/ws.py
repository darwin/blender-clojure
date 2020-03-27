import asyncio
import threading
import time

import websockets
import logging

from bclj import v8, autils, js

logger = logging.getLogger(__name__)

async_loop: asyncio.AbstractEventLoop


def start_async_loop_if_needed():
    if "async_loop" not in globals():
        global async_loop
        async_loop = autils.start_async_loop(__name__)


class Event(v8.JSClass):

    def __init__(self):
        super().__init__()


class MessageEvent(Event):

    def __init__(self, data):
        super().__init__()
        self.data = data


class ErrorEvent(Event):

    def __init__(self, ex):
        super().__init__()
        self.exception = ex
        self.message = str(ex)

    def __str__(self):
        return self.message


def abbreviate_message_for_log(msg):
    if len(msg) > 300:
        return msg[:300] + "...}"
    else:
        return msg


global_next_ws_instance_id = 1


# note that this is not full WebSocket implementation,
# we implement only what is currently needed for shadow-cljs to work
class WebSocket(object):
    READY_STATE_CONNECTING = 0
    READY_STATE_OPEN = 1
    READY_STATE_CLOSING = 2
    READY_STATE_CLOSED = 3

    async def _reload_page(self):
        logger.debug("ws#{} finally calling js.reload_page()".format(self.id))
        js.reload_page()

    async def _run_client_loop(self):
        logger.info("Connecting via websockets to '{}'".format(self.url))
        try:
            async with websockets.connect(self.url) as ws:
                logger.debug("ws#{} client_loop: entering receive loop... {}".format(self.id, ws))
                self._ws = ws
                await autils.get_result(self._change_ready_state(self.READY_STATE_OPEN))
                done = False
                try:
                    while not done:
                        msg = await ws.recv()
                        logger.debug("ws#{} client_loop: got message len={}\n<< {}".format(self.id, len(msg),
                                                                                           abbreviate_message_for_log(msg)))

                        await autils.get_result(self._trigger_handler("onmessage", MessageEvent(msg)))

                        if "{:type :client/stale}" in msg:
                            logger.debug("ws#{} stale client detected - scheduling page reload".format(self.id))
                            logger.warning("Detected stale client - will reload...")
                            # give the whole system some time before we attempt to refresh
                            await asyncio.sleep(5)
                            autils.call_soon(self._main_loop, self._reload_page)
                            done = True

                except websockets.exceptions.ConnectionClosed as e:
                    logger.debug("ws#{} client_loop: connection closed {}".format(self.id, e))
                    logger.warning("Websockets connection lost")
                    self._trigger_handler("onerror", ErrorEvent(e))
                except Exception as e:
                    logger.debug("ws#{} client_loop: exception {}".format(self.id, e))
                    logger.error("Websocket ran into problems {}".format(e))
                    self._trigger_handler("onerror", ErrorEvent(e))
                logger.debug("ws#{} client_loop: leaving...".format(self.id))
                self._ws = None

        except Exception as e:
            self.readyState = self.READY_STATE_CLOSED
            await autils.get_result(self._trigger_handler("onerror", ErrorEvent(e)))

    async def _trigger_handler_async(self, handler_name, *args):
        logger.debug("ws#{} triggering handler {} with args={}".format(self.id, handler_name, args))
        handler = getattr(self, handler_name, None)
        if handler is not None:
            return v8.execute_callback(self._window.context, handler, *args)

    def _trigger_handler(self, handler_name, *args):
        return autils.call_soon(self._main_loop, self._trigger_handler_async, handler_name, *args)

    async def _send_message(self, msg):
        try:
            return await self._ws.send(msg)
        except asyncio.streams.IncompleteReadError as e:
            logger.error("Interrupted websocket send: {}".format(e))

    def _change_ready_state(self, new_state):
        self.readyState = new_state
        if new_state is self.READY_STATE_OPEN:
            return self._trigger_handler("onopen", Event())
        elif new_state is self.READY_STATE_CLOSED:
            return self._trigger_handler("onclose", Event())

    @v8.report_exceptions
    def __init__(self, url, protocols=None):
        assert (protocols is None)
        global global_next_ws_instance_id
        self.id = global_next_ws_instance_id
        global_next_ws_instance_id += 1
        logger.debug("ws#{} __init__".format(self.id))
        start_async_loop_if_needed()

        assert threading.current_thread() is threading.main_thread()
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

        autils.call_soon(async_loop, self._run_client_loop)

    @v8.report_exceptions
    def send(self, msg, *_):
        logger.debug("ws#{} send msg={}".format(self.id, abbreviate_message_for_log(msg)))
        autils.call_soon(async_loop, self._send_message, msg)

    @v8.report_exceptions
    def close(self, code=None, reason=None, *_):
        logger.debug("ws#{} close code={} reason={}".format(self.id, code, reason))
        self._change_ready_state(self.READY_STATE_CLOSED)
        self._ws.close()
        logger.debug("ws#{} closed".format(self.id))
