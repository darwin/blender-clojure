(import sys
        clojure
        [uuid [uuid4]])
(require [clojure [*]])
(import [clojure [*]])
(import [HyREPL [bencode]]
        [HyREPL.ops [find-op]]
        [HyREPL.hacks [hack]])
(require [hy.contrib.walk [let]])

(setv sessions {})

(defclass Session [object]
  (setv status "")
  (setv eval-id "")
  (setv repl None)
  (setv last-traceback None)

  (defn --init-- [self]
    (setv self.uuid (str (uuid4)))
    (assoc sessions self.uuid self)
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
      (cond
        [(list? res) (for [r res]
                       (.write self r transport))]
        [(map? res) ((find-op (.get res "op")) self res transport)]
        [:else ((find-op (.get msg "op")) self msg transport)]))))
