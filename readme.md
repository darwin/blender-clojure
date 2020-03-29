# Clojure and Blender

This is an experimental attempt to enable Blender scripting in ClojureScript.

Technically we extend Blender with a Python3 script (see [driver](driver)) 
which embeds V8 Javascript engine and manages running ClojureScript-generated Javascript in there.

Crazy? Maybe, but it works surprisingly well.

Actually, there is more to it:

* We use [shadow-cljs](https://github.com/thheller/shadow-cljs) as our build tool, hot code reloading and REPL 
* We implement minimal set of web APIs for shadow-cljs to work (it thinks it talks to a browser)
* We expose Blender's [Python APIs](https://docs.blender.org/api/current/index.html) in the Javascript context
* We generate [bcljs library](bcljs) wrapping Python APIs for more idiomatic ClojureScript access

Please see examples in [sandboxes/shadow](sandboxes/shadow) to get the feel for it.

This project also supports writing your scripts in [hylang](https://github.com/hylang/hy). 
See [docs/hylang.md](docs/hylang.md) for details.  

### Initial setup

Tested under macOS, should work under Linux as well.

```bash
# note: this is probably not needed under macOS if you put Blender to /Applications/Blender.app 
# a pro tip: it is convenient to use direnv for this
export BCLJ_BLENDER_PATH="/path/to/your/blender"
export BCLJ_BLENDER_PYTHON_PATH="/path/to/your/blender/and/its/python"
```

#### Python dependencies

* Run `./scripts/install-deps.sh` to install our dependencies (creates Python virtual env under `.venv`).

#### Python V8 module

Unfortunately you have to compile V8 by hand. We provide a script which worked for us under macOS:

```bash
# this script should compile and install fresh v8 python module under venv/lib/.../site-packages
./scripts/prepare-v8.sh
```

#### NPM dependencies

You should keep your npm deps fresh as well:

```bash
cd sandboxes/shadow
npm install
```

#### Generate bcljs library

The generator is currently under development so to prevent churn we don't commit generated library.

You have to generate it yourself:

```bash
cd tools
./scripts/generate-xml.sh
./scripts/buidl-api.sh
```

It will ask to specify Blender binary path and Blender source code repo path on your machine.
Instead of passing it via command-line options you can set it via env variables, here are mine:

```text
❯ env | grep BLENDER
BLENDER_BINARY_PATH=/Applications/Blender.app/Contents/MacOS/Blender
BLENDER_REPO_DIR=/Users/darwin/lab/blender 
```

The generated source files are rsynced into [bcljs/src/gen](bcljs/src/gen).
See the [tools/readme.md](tools/readme.md) for more details.

## A typical workflow

In one terminal session, compile and watch shadow-cljs sandbox: 
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
BCLJ_PACKAGES_DIR=/Users/darwin/lab/blender-clojure/.venv/lib/python3.7/site-packages
+ exec /Applications/Blender.app/Contents/MacOS/Blender assets/blank.blend --python /Users/darwin/lab/blender-clojure/driver/src/entry.py
Read prefs: /Users/darwin/Library/Application Support/Blender/2.82/config/userpref.blend
found bundled python: /Applications/Blender.app/Contents/Resources/2.82/python
Read blend: /Users/darwin/lab/blender-clojure/assets/blank.blend

~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
Starting blender-clojure driver...
hy 0.18.0 using CPython(default) 3.7.4 on Darwin in Blender 2.82 (sub 7)
Connecting via websockets to 'ws://localhost:9630/ws/worker/sandbox/e452c1e2-aef2-4fc0-af73-6d8a0870e2f0/aed44a0b-5b5e-411b-977f-c70d7f60e120/browser'
console.log Hello from bpg.sandbox
console.log bpg.sandbox.init() called!
console.log shadow-cljs: WebSocket connected!
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

To start from scratch:
```bash
./scripts/nuke.sh
```
