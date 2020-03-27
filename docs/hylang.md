## Hy support

If you want to be closer to bare python you can opt-in to use hylang.

### Connecting via nREPL

First you have to enable it via env: 

```bash
export BCLJ_HYLANG_NREPL=1

# optionally export:
#export BCLJ_HYLANG_NREPL_HOST=localhost
#export BCLJ_HYLANG_NREPL_PORT=1338
```

Then running `blender.sh` should display `nREPL server listening on ('127.0.0.1', 1337)`, see below:

```text
> ./scripts/blender.sh examples/one-hundred-cubes.hy
BCLJ_BLENDER_PATH=/Applications/Blender.app/Contents/MacOS/Blender
BCLJ_BLENDER_PYTHON_PATH=/Applications/Blender.app/Contents/Resources/2.82/python
BCLJ_LIVE_FILE=/Users/darwin/lab/blender-hylang-live-code/examples/one-hundred-cubes.hy
BCLJ_HYLANG_NREPL=1
+ exec /Applications/Blender.app/Contents/MacOS/Blender assets/blank.blend --python /Users/darwin/lab/blender-hylang-live-code/src/watcher/main.py
Read prefs: /Users/darwin/Library/Application Support/Blender/2.82/config/userpref.blend
found bundled python: /Applications/Blender.app/Contents/Resources/2.82/python
Read blend: /Users/darwin/lab/blender-hylang-live-code/assets/blank.blend

==== hylc watcher =====
hy 0.18.0 using CPython(default) 3.7.4 on Darwin in Blender 2.82 (sub 7)
nREPL server listening on ('127.0.0.1', 1337)

Watching '/Users/darwin/lab/blender-hylang-live-code/examples/one-hundred-cubes.hy' for changes and re-loading.
Reloading '/Users/darwin/lab/blender-hylang-live-code/examples/one-hundred-cubes.hy'
Info: Deleted 0 object(s)
Done executing '/Users/darwin/lab/blender-hylang-live-code/examples/one-hundred-cubes.hy'
...
```

Then in another terminal session you can try to connect with `lein repl` and eval something:

```bash
> lein repl :connect 1337
Connecting to nREPL at 127.0.0.1:1337
HyREPL: hy 0.18.0 using CPython(default) 3.7.4 on Darwin in Blender 2.82 (sub 7)

Hy=> (+ 1 2)
3
Hy=>
```
