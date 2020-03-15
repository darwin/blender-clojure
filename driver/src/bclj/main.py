import logging
from bclj import env_info, js, hy, blender

logger = logging.getLogger(__name__)


def print_welcome():
    print(env_info.describe_environment())


def start():
    print_welcome()
    js.bootstrap()
    hy.start_hyrepl()
    blender.register()
