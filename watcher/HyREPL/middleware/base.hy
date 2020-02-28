; https://github.com/clojure/tools.nrepl/blob/master/doc/ops.md
(import sys)
(require [hy.contrib.walk [let]])
(import [HyREPL.utils [make-version]]
        [HyREPL.ops [ops]])
(require [HyREPL.ops [defop]])

(defop clone [session msg transport]
       {"doc" "Clones a session"
       "requires" {}
       "optional" {"session" "The session to be cloned. If this is left out, the current session is cloned"}
       "returns" {"new-session" "The ID of the new session"}}
       (import [HyREPL.session [Session]]) ; Imported here to avoid circ. dependency
       (let [s (Session)]
         (.write session {"status" ["done"] "id" (.get msg "id") "new-session" (str s)} transport)))


(defop close [session msg transport]
       {"doc" "Closes the specified session"
        "requires" {"session" "The session to close"}
        "optional" {}
        "returns" {}}
       (.write session
               {"status" ["done"]
                "id" (.get msg "id")
                "session" session.uuid}
               transport)
       (import [HyREPL.session [sessions]]) ; Imported here to avoid circ. dependency
       (try
         (del (get sessions (.get msg "session" "")))
         (except [e KeyError]))
       (.close transport))


(defop describe [session msg transport]
       {"doc" "Describe available commands"
       "requires" {}
       "optional" {"verbose?" "True if more verbose information is requested"}
       "returns" {"aux" "Map of auxiliary data"
                 "ops" "Map of operations supported by this nREPL server"
                 "versions" "Map containing version maps, for example of the nREPL protocol supported by this server"}}
       ; TODO: don't ignore verbose argument
       ; TODO: more versions: Python, Hy
       (.write session
               {"status" ["done"]
               "id" (.get msg "id")
               "versions" {"nrepl" (make-version 0 2 7)
                           "java" (make-version)
                           "clojure" (make-version)}
                "ops" (dfor [k v] (.items ops) [k (:desc v)])
                "session" (.get msg "session")}
               transport))


(defop stdin [session msg transport]
       {"doc" "Feeds value to stdin"
       "requires" { "value" "value to feed in" }
       "optional" {}
       "returns" {"status" "\"need-input\" if more input is needed"}}
       (.put sys.stdin (get msg "value"))
       (.task-done sys.stdin))


(defop "ls-sessions" [session msg transport]
       {"doc" "Lists running sessions"
        "requires" {}
        "optional" {}
        "returns" {"sessions" "A list of running sessions"}}
       (import [HyREPL.session [sessions]]) ; Imported here to avoid circ. dependency
       (.write session
               {"status" ["done"]
                "sessions" (list-comp
                             (. s uuid)
                             [s (.values sessions)])
                "id" (.get msg "id")
                "session" session.uuid}
               transport))
