import asyncio
import threading

from bclj import autils

assert threading.current_thread() is threading.main_thread()
main_loop = asyncio.get_event_loop()


async def process_job(session, msg, request):
    session.handle(msg, request)


def handle_session_message(session, msg, request):
    autils.call_soon(main_loop, process_job, session, msg, request)
