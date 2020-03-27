## Hy support

We focus on ClojureScript support, but if you want to be closer to bare Python then you can opt-in to use [hylang](https://github.com/hylang/hy).

You might want to:

  * specify a live hylang file via --hylive (it will be reloaded on every change)
  * connect to HyREPL (nREPL) and drive your interaction with Blender via REPL
  
## A bit of historical context

I wanted to use a lisp to script Blender and found [chr15m/blender-hylang-live-code](https://github.com/chr15m/blender-hylang-live-code).

I prefer to do REPL-driven development with Clojure and ClojureScript. It looked like a good fit.  

I implemented HyREPL support to enable nREPL connection and started playing with some scripting. Unfortunately I realized that 
hylang is not the way to go for me. It wasn't mature enough and it seemed like a lipstick on a "pyg". That is why I embarked 
on the journey of embedding V8 and using ClojureScript with all the tooling I'm familiar with.

For interactive creative coding I'm not that concerned about performance. If I needed to squeeze some performance
I would drop directly to Python or write a C extension to Blender.    

### Using live file

```text
> ./scripts/blender.sh --hylive sandboxes/hylang/examples/one-hundred-cubes.hy
BCLJ_BLENDER_PATH=/Applications/Blender.app/Contents/MacOS/Blender
...
BCLJ_HY_SUPPORT=1
BCLJ_HYLIB_DIR=/Users/darwin/lab/blender-clojure/sandboxes/hylang/hylib
BCLJ_LIVE_FILE=/Users/darwin/lab/blender-clojure/sandboxes/hylang/examples/one-hundred-cubes.hy
BCLJ_PACKAGES_DIR=/Users/darwin/lab/blender-clojure/.venv/lib/python3.7/site-packages
+ exec /Applications/Blender.app/Contents/MacOS/Blender assets/blank.blend --python /Users/darwin/lab/blender-clojure/driver/src/entry.py --no-window-focus
Read prefs: /Users/darwin/Library/Application Support/Blender/2.82/config/userpref.blend
found bundled python: /Applications/Blender.app/Contents/Resources/2.82/python
Read blend: /Users/darwin/lab/blender-clojure/assets/blank.blend

~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
Starting blender-clojure driver...
hy 0.18.0 using CPython(default) 3.7.4 on Darwin in Blender 2.82 (sub 7)
Watching '/Users/darwin/lab/blender-clojure/sandboxes/hylang/examples/one-hundred-cubes.hy' for changes and re-loading.
Reloading '/Users/darwin/lab/blender-clojure/sandboxes/hylang/examples/one-hundred-cubes.hy'
Info: Deleted 0 object(s)
Done executing '/Users/darwin/lab/blender-clojure/sandboxes/hylang/examples/one-hundred-cubes.hy'
```

### Connecting to HyREPL

First you have to enable it via env: 

```bash
export BCLJ_HYLANG_NREPL=1

# optionally export:
#export BCLJ_HYLANG_NREPL_HOST=localhost
#export BCLJ_HYLANG_NREPL_PORT=1338
```

Then running `blender.sh` should display `hylang nREPL server started on 127.0.0.1:1337`, see below:

```text
> ./scripts/blender.sh
BCLJ_BLENDER_PATH=/Applications/Blender.app/Contents/MacOS/Blender
...
BCLJ_HY_SUPPORT=1
BCLJ_HYLIB_DIR=/Users/darwin/lab/blender-clojure/sandboxes/hylang/hylib
BCLJ_PACKAGES_DIR=/Users/darwin/lab/blender-clojure/.venv/lib/python3.7/site-packages
+ exec /Applications/Blender.app/Contents/MacOS/Blender assets/blank.blend --python /Users/darwin/lab/blender-clojure/driver/src/entry.py --no-window-focus
Read prefs: /Users/darwin/Library/Application Support/Blender/2.82/config/userpref.blend
found bundled python: /Applications/Blender.app/Contents/Resources/2.82/python
Read blend: /Users/darwin/lab/blender-clojure/assets/blank.blend

~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
Starting blender-clojure driver...
hy 0.18.0 using CPython(default) 3.7.4 on Darwin in Blender 2.82 (sub 7)
hylang nREPL server started on 127.0.0.1:1337
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
