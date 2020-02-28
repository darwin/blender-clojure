(import time
        os
        [HyREPL.server :as repl-server]
        [HyREPL.middleware.eval :as repl-mw])

(import [hy.contrib.walk [*]])
(require [hy.contrib.walk [*]])

(setv (. repl-mw eval-module) (globals))

(defn parse-port-from-env [port-str]
  (try
    (int port-str)
    (except [_ ValueError])))

(defn start-server []
  (let [env-host (os.environ.get "HYLC_NREPL_HOST")
        env-port (os.environ.get "HYLC_NREPL_PORT")
        host (or env-host "127.0.0.1")
        port (or (parse-port-from-env env-port) 1337)
        nrepl-server (repl-server.start-server host port)
        tcp-server (second nrepl-server)]
    (print (.format "nREPL server listening on {}" (. tcp-server server-address)))
    nrepl-server))
