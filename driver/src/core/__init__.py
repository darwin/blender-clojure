import os
import sys

this_dir = os.path.abspath(os.path.dirname(__file__))
sys.path.insert(0, this_dir)

import main

main.start()
