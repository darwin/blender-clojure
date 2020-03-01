(import time
        os
        [HyREPL.server :as repl-server]
        [HyREPL.middleware.eval :as repl-mw])

(require [hy.contrib.walk [let]])

(setv (. repl-mw eval-module) (globals))

(defn parse-port-from-env [port-str]
  (if port-str
    (try
      (int port-str)
      (except [_ ValueError]))))

(defn format-server-address [address]
  (let [host (first address)
        port (second address)]
    (.format "{}:{}" host port)))

(defn start-server []
  (let [env-host (os.environ.get "BCLJ_HYLANG_NREPL_HOST")
        env-port (os.environ.get "BCLJ_HYLANG_NREPL_PORT")
        host (or env-host "127.0.0.1")
        port (or (parse-port-from-env env-port) 1337)
        nrepl-server (repl-server.start-server host port)
        tcp-server (second nrepl-server)
        server-address (. tcp-server server-address)]
    (print (.format "nREPL server listening on {}" (format-server-address server-address)))
    nrepl-server))

(defn shutdown-server [nrepl-server]
  (let [tcp-server (second nrepl-server)
        server-address (. tcp-server server-address)]
    (print (.format "Shutting down nREPL server {}..." (format-server-address server-address)))
    (repl-server.shutdown-server nrepl-server)))
