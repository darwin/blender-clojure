(import sys
        clojure
        bclj.hy
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
    (bclj.hy.repl_logger.debug (.format "out: {}" msg))
    (try
      (.sendall transport (bencode.encode msg))
      (except [e OSError]
        (bclj.hy.repl_logger.info (.format "Client gone: {}" e)))))
  (defn handle [self msg transport]
    (bclj.hy.repl_logger.debug (.format "in: {}" msg))
    (let [res (hack self msg)]
      (cond
        [(list? res) (for [r res]
                       (.write self r transport))]
        [(map? res) ((find-op (.get res "op")) self res transport)]
        [:else ((find-op (.get msg "op")) self msg transport)]))))
