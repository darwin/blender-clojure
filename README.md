Live-coding [Blender](http://blender3d.org/) with [Hylang](http://hylang.org/).

[![Quick screencast of Live-coding Blender with Hylang](docs/images/screencast.gif)](https://www.youtube.com/watch?v=vRBdqsaKmuU)

### Running it ###

Tested under macOS, should work under Linux as well.

```bash
# note: this is probably not needed under macOS if you put Blender to /Applications/Blender.app 
export HYLC_BLENDER_PATH="/path/to/your/blender"
export HYLC_BLENDER_PYTHON_PATH="/path/to/your/blender/and/its/python"
```

* Run `./scripts/install-dependencies.sh` to install the Hy-lang dependencies in a place where Blender's internal Python can find them.
* Run `./scripts/blender-livecode.sh examples/one-hundred-cubes.hy` to start Blender watching one-hundred-cubes.hy and re-loading it whenever it changes.

