import logging
import asyncio
from bclj import env_info, js, hy, blender, autils

logger = logging.getLogger(__name__)
main_event_loop = None


def print_welcome():
    print(env_info.describe_environment())


async def bootstrap_js():
    # give all other system some time before we boot js engine
    await asyncio.sleep(1)
    js.bootstrap()


def start():
    global main_event_loop

    print_welcome()
    hy.start_hyrepl()
    blender.register()

    main_event_loop = asyncio.get_event_loop()
    assert (main_event_loop is not None)
    autils.call_soon(main_event_loop, bootstrap_js)
