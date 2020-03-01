This is a highly experimental attempt to enable Clojure REPL-driven development for Blender live-coding.

### Running it

Tested under macOS, should work under Linux as well.

```bash
# note: this is probably not needed under macOS if you put Blender to /Applications/Blender.app 
# a pro tip: it is convenient to use direnv for this
export BCLJ_BLENDER_PATH="/path/to/your/blender"
export BCLJ_BLENDER_PYTHON_PATH="/path/to/your/blender/and/its/python"
```

* Run `./scripts/install-deps.sh` to install the Hy-lang dependencies in a place where Blender's internal Python can find them.
* Run `./scripts/blender.sh examples/one-hundred-cubes.hy` to start Blender watching one-hundred-cubes.hy and re-loading it whenever it changes.

## ClojureScript support

Unfortunately installation might be quite involved because you have to compile V8 engine:
```bash
# this script should compile and install fresh v8 python module under _modules_v8
./scripts/build_v8.sh
```

```bash
cd sandboxes/shadow
npm install
```

In another terminal session, compile and watch shadow-cljs sandbox: 
```bash
cd sandboxes/shadow
shadow-cljs watch sandbox
```
```text
shadow-cljs - config: /Users/darwin/lab/blender-clojure/sandboxes/shadow/shadow-cljs.edn  cli version: 2.8.90  node: v13.8.0
shadow-cljs - starting via "clojure"
shadow-cljs - server version: 2.8.90 running at http://localhost:9630
shadow-cljs - nREPL server started on port 64170
shadow-cljs - watching build :sandbox
[:sandbox] Configuring build.
[:sandbox] Compiling ...
[:sandbox] Build completed. (137 files, 1 compiled, 0 warnings, 1.72s)
```

In another terminal session, start Blender with blender-clojure driver:
```bash
./scripts/blender.sh
```
```text
BCLJ_BLENDER_PATH=/Applications/Blender.app/Contents/MacOS/Blender
BCLJ_BLENDER_PYTHON_PATH=/Applications/Blender.app/Contents/Resources/2.82/python
BCLJ_HYLANG_NREPL=1
+ exec /Applications/Blender.app/Contents/MacOS/Blender assets/blank.blend --python /Users/darwin/lab/blender-clojure/driver/src/entry.py
Read prefs: /Users/darwin/Library/Application Support/Blender/2.82/config/userpref.blend
found bundled python: /Applications/Blender.app/Contents/Resources/2.82/python
Read blend: /Users/darwin/lab/blender-clojure/assets/blank.blend

~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
Starting blender-clojure driver...
hy 0.18.0 using CPython(default) 3.7.4 on Darwin in Blender 2.82 (sub 7)
hylang nREPL server started on 127.0.0.1:1337
Watching 'None' for changes and re-loading.
MainThread: ~js | request to import '/Users/darwin/lab/blender-clojure/sandboxes/shadow/public/.compiled/sandbox.js'
ws-asyncio: ~websockets | Entering websockets server loop <_UnixSelectorEventLoop running=False closed=False debug=True>
Connecting via websockets to 'ws://localhost:9630/ws/worker/sandbox/653fee06-84dc-4d31-82ad-bf0de1b8c8ed/d5925c9c-171f-4365-b915-91657a96b29b/browser'
console.log Hello from bpg.sandbox
console.log bpg.sandbox.init() called!
ws-asyncio: ~websockets | client_loop: entering receive loop... <websockets.client.WebSocketClientProtocol object at 0x1209d9a50>
ws-asyncio: ~websockets | client_loop: got message len=10835
<< {:type :repl/init, :repl-state {:shadow.cljs.repl/repl-state true, :current-ns bpg.sandbox, :repl-sources [{:resource-id [:shadow.build.classpath/resource "goog/base.js"], :type :goog, :resource-name "goog/base.js", :output-name "goog.base.js", :from-jar true, :provides #{goog}} {:resource-id [:shad...}
ws-asyncio: ~websockets | client_loop: got message len=46
<< {:type :repl/ping, :time-server 1583103908025}
console.log shadow-cljs: REPL session start successful
```

In another terminal session, connect to running Blender via REPL:
```bash
cd sandboxes/shadow
shadow-cljs cljs-repl sandbox
```
```text
shadow-cljs - config: /Users/darwin/lab/blender-clojure/sandboxes/shadow/shadow-cljs.edn  cli version: 2.8.90  node: v13.8.0
shadow-cljs - connected to server
bpg.sandbox=> (+ 1 2)
3

>
```

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
