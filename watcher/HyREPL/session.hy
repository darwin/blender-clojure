(import sys [uuid [uuid4]] [threading [Lock]])
(require [hy.contrib.walk [let]])
(import
  [HyREPL [bencode]]
  [HyREPL.ops [find-op]])


(setv sessions {})

(defn reply-cursive-1 [session msg]
  [{"id" (.get msg "id")
   "session" (.get msg "session")
   "value" "#'cursive.repl.runtime/print-last-error"}
   {"id" (.get msg "id")
   "session" (.get msg "session")
   "status" ["done"]}])

(defn reply-cursive-2 [session msg]
  [{"id" (.get msg "id")
   "session" (.get msg "session")
   "ns" "user"
   "value" "nil"}
   {"id" (.get msg "id")
   "session" (.get msg "session")
   "status" ["done"]}])

(defn reply-cursive-3 [session msg]
  [{"id" (.get msg "id")
   "session" (.get msg "session")
   "out" "hylang\n"}
   {"id" (.get msg "id")
   "session" (.get msg "session")
   "status" ["done"]}])

(defn reply-cursive-4 [session msg]
  [{"id" (.get msg "id")
   "session" (.get msg "session")
   "ns" "user"
   "value" "{}"}
   {"id" (.get msg "id")
   "session" (.get msg "session")
   "status" ["done"]}])

(defn reply-lein-intro [session msg]
  [{"id" (.get msg "id")
   "session" (.get msg "session")
   "out" "hy v0.18\n\n"}
   {"id" (.get msg "id")
   "session" (.get msg "session")
   "ns" "Hy"
   "value" "nil"}
   {"id" (.get msg "id")
   "session" (.get msg "session")
   "status" ["done"]}])

(defn reply-lein-eval-modes [session msg]
  [{"id" (.get msg "id")
   "session" (.get msg "session")
   "ns" "Hy"
   "value" "nil"}
   {"id" (.get msg "id")
   "session" (.get msg "session")
   "status" ["done"]}])

(defn hack [session msg]
  (let [op (.get msg "op")
        file (.get msg "file")
        code (.get msg "code")]
    (cond
      [(and (= op "load-file") (.startswith file "(ns cursive.repl.runtime"))
       (reply-cursive-1 session msg)]
      [(and (= op "eval") (= code "(get *compiler-options* :disable-locals-clearing)"))
       (reply-cursive-2 session msg)]
      [(and (= op "eval") (.startswith code "(do (clojure.core/println (clojure.core/str \"Clojure \" (clojure.core/clojure-version)))"))
       (reply-cursive-3 session msg)]
      [(and (= op "eval") (.startswith code "(cursive.repl.runtime/completions"))
       (reply-cursive-4 session msg)]
      [(and (= op "eval") (.startswith code "(do (do (do (clojure.core/println (clojure.core/str \"REPL-y \""))
       (reply-lein-intro session msg)]
      [(and (= op "eval") (.startswith code "(clojure.core/binding [clojure.core/*ns* (clojure.core/or (clojure.core/find-ns (clojure.core/symbol \"reply.eval-modes.nrepl\"))"))
       (reply-lein-eval-modes session msg)]
       )))

(defclass Session [object]
  (setv status "")
  (setv eval-id "")
  (setv repl None)
  (setv last-traceback None)

  (defn --init-- [self]
    (setv self.uuid (str (uuid4)))
    (assoc sessions self.uuid self)
    (setv self.lock (Lock))
    None)
  (defn --str-- [self]
    self.uuid)
  (defn --repr-- [self]
    self.uuid)
  (defn write [self msg transport]
    (assert (in "id" msg))
    (unless (in "session" msg)
      (assoc msg "session" self.uuid))
    (print "out:" msg :file sys.stderr)
    (try
      (.sendall transport (bencode.encode msg))
      (except [e OSError]
        (print (.format "Client gone: {}" e) :file sys.stderr))))
  (defn handle [self msg transport]
    (print "in:" msg :file sys.stderr)
    (let [res (hack self msg)]
      (if res
        (for [r res]
          (.write self r transport))
        ((find-op (.get msg "op")) self msg transport)))))
        
