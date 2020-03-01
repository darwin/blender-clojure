import asyncio
import socket


# https://stackoverflow.com/a/48491563/84283

async def detect_iowait():
    loop = asyncio.get_event_loop()
    rsock, wsock = socket.socketpair()
    wsock.close()
    await loop.sock_recv(rsock, 1)
    rsock.close()


# stop loop.run_forever once iowait is detected
async def stop_on_iowait():
    await detect_iowait()
    asyncio.get_event_loop().stop()


def drain_asyncio_event_loop():
    loop = asyncio.get_event_loop()
    loop.create_task(stop_on_iowait())
    loop.run_forever()
