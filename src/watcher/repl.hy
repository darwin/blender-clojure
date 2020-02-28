(import time
        [HyREPL.server :as repl]
        [HyREPL.middleware.eval :as repl-mw])

(import [hy.contrib.walk [*]])
(require [hy.contrib.walk [*]])

(setv (. repl-mw eval-module) (globals))

(defn start-server []
  (let [s (repl.start-server)]
    (print (.format "nREPL server listening on {}" (. (second s) server-address)))))
