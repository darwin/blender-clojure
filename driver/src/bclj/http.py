import logging
import asyncio
import threading

import aiohttp
from aiohttp import ClientResponse

from bclj import v8, autils

logger = logging.getLogger(__name__)

from threading import Thread


def async_loop_thread(loop):
    asyncio.set_event_loop(loop)
    loop.set_debug(True)
    logger.debug("Entering async loop {}".format(loop))
    loop.run_forever()


def start_async_loop():
    loop = asyncio.new_event_loop()
    t = Thread(target=async_loop_thread, args=(loop,))
    t.name = "{} [asyncio]".format(__name__)
    t.daemon = True
    t.start()
    return loop


async_loop: asyncio.AbstractEventLoop


def start_async_loop_if_needed():
    if "async_loop" not in globals():
        global async_loop
        async_loop = start_async_loop()


async def dispatch_http_method(session, method, *args, **kwargs):
    return await getattr(session, method.lower())(*args, **kwargs)


async def do_http_request(session, method, url, headers, data):
    response: ClientResponse = await dispatch_http_method(session, method, url, headers=headers, data=data)
    return response


def abbreviate_message_for_log(msg):
    if len(msg) > 300:
        return msg[:300] + "...}"
    else:
        return msg


# note that this is not full XMLHttpRequest implementation,
# we implement only what is currently needed for shadow-cljs to work
# noinspection PyPep8Naming
class XMLHttpRequest(object):
    READY_STATE_UNSENT = 0  # Client has been created. open() not called yet.
    READY_STATE_OPENED = 1  # open() has been called.
    READY_STATE_HEADERS_RECEIVED = 2  # send() has been called, and headers and status are available.
    READY_STATE_LOADING = 3  # Downloading; responseText holds partial data.
    READY_STATE_DONE = 4  # The operation is complete.

    async def _process_onreadystatechange(self):
        if self.onreadystatechange is not None:
            v8.execute_callback(self._window.context, self.onreadystatechange)

    def _change_ready_state(self, new_state):
        self.readyState = new_state
        autils.call_soon(self._main_loop, self._process_onreadystatechange)

    async def _send_request(self, body):
        url = self._url
        method = self._method
        headers = self._headers
        logger.debug("Sending HTTP {} request {} (body length={})\n{}".format(method, url, len(body),
                                                                              abbreviate_message_for_log(body)))
        async with aiohttp.ClientSession() as session:
            response = await do_http_request(session, method, url, headers, body)
            self.status = response.status
            self.statusText = response.reason
            self.responseURL = response.url

            self._change_ready_state(self.READY_STATE_HEADERS_RECEIVED)
            self._change_ready_state(self.READY_STATE_LOADING)

            self.responseText = await response.text()
            self.response = self.responseText
            logger.debug("got response {}".format(abbreviate_message_for_log(self.response)))
            self._change_ready_state(self.READY_STATE_DONE)

    @v8.report_exceptions
    def __init__(self):
        start_async_loop_if_needed()

        assert threading.current_thread() is threading.main_thread()
        self._main_loop = asyncio.get_event_loop()
        # noinspection PyUnresolvedReferences
        self._window = self.__class__.window
        self._headers = {}
        self._method = None
        self._url = None

        self.onreadystatechange = None
        self.status = 0
        self.statusText = ""
        self.readyState = self.READY_STATE_UNSENT
        self.responseType = "text"
        self.response = ""
        self.responseText = ""
        self.responseURL = ""
        self.withCredentials = False

    @v8.report_exceptions
    def open(self, method=None, url=None, asyn=None, user=None, password=None, *_):
        logger.debug("open method={} url={} asyn={}".format(method, url, asyn))
        assert asyn
        self._method = method
        self._url = url
        self._change_ready_state(self.READY_STATE_OPENED)

    @v8.report_exceptions
    def setRequestHeader(self, header=None, value=None, *_):
        logger.debug("setRequestHeader header={} value={}".format(header, value))
        self._headers[header] = value

    @v8.report_exceptions
    def send(self, body=None, *_):
        logger.debug("send {}".format(body))
        assert isinstance(body, str)
        autils.call_soon(async_loop, self._send_request, body)

    @v8.report_exceptions
    def abort(self, *_):
        logger.debug("abort")
