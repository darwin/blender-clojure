import os
import sys


def brutal_exit(code):
    sys.stdout.flush()
    sys.stderr.flush()
    os._exit(code)
