import asyncio
import websockets
import STPyV8
import logging
import v8

logger = logging.getLogger('hylc.websockets')

from threading import Thread


def start_server_loop(loop):
    logger.debug("starting websockets server loop {}".format(loop))
    asyncio.set_event_loop(loop)
    loop.set_debug(True)
    loop.run_forever()


server_loop = asyncio.new_event_loop()
t = Thread(target=start_server_loop, args=(server_loop,))
t.daemon = True
t.start()


class MessageEvent(v8.JSClass):

    def __init__(self, data):
        self.data = data


def abbreviate_message_for_log(msg):
    if len(msg) > 300:
        return msg[:300] + "...}"
    else:
        return msg


def execute_callback(context, code, *args):
    with context as ctx:
        try:
            assert (isinstance(code, STPyV8.JSFunction))
            code(*args)
        except Exception as e:
            print("Error while handling websocket callback", e)
            return None


async def process_onmessage(self, msg):
    if self.onmessage is not None:
        execute_callback(self.window.context, self.onmessage, MessageEvent(msg))


async def client_loop(self):
    logger.info("Connecting via websockets to '{}'".format(self.url))
    async with websockets.connect(self.url) as ws:
        logger.debug("client_loop: entering receive loop... {}".format(ws))
        self.ws = ws
        try:
            while True:
                msg = await ws.recv()
                logger.debug("client_loop: got message len={}\n<< {}".format(len(msg), abbreviate_message_for_log(msg)))
                asyncio.run_coroutine_threadsafe(process_onmessage(self, msg), self.main_loop)
        except Exception as e:
            logger.error("client_loop: ran into problems {}".format(e))
        self.ws = None
        logger.debug("client_loop: leaving...")


async def client_send(self, msg):
    logger.debug("client_send len={}\n>> {}".format(len(msg), abbreviate_message_for_log(msg)))
    await self.ws.send(msg)


class WebSocket(object):

    def __init__(self, url):
        self.main_loop = asyncio.get_event_loop()
        self.url = url
        self.ws = None
        self.onmessage = None
        self.onopen = None
        self.onclose = None
        self.onerror = None
        # noinspection PyUnresolvedReferences
        self.window = self.__class__.window
        asyncio.run_coroutine_threadsafe(client_loop(self), server_loop)

    def send(self, msg):
        asyncio.run_coroutine_threadsafe(client_send(self, msg), server_loop)

    def close(self):
        self.ws.close()
