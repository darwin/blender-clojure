(import sys)
(import [HyREPL.utils [make-version]])
(require [cursive [defmacro-g!]])

(setv ops {})

(defmacro-g! defop [name args desc &rest body]
  (if-not (instance? (, str HySymbol) name)
    (macro-error name "Name must be a symbol or a string."))
  (if-not (instance? HyList args)
    (macro-error args "Arguments must be a list."))
  (if-not (instance? HyDict desc)
    (macro-error desc "Description must be a dictionary."))
  (setv fn-checked
        `(fn ~args
           (setv g!failed False)
           (for [g!r (.keys (.get ~desc "requires" {}))]
             (unless (in g!r (second ~args))
               (.write (first ~args)
                       {"status"  ["done"]
                        "id"      (.get (second ~args) "id")
                        "missing" (str g!r)} (nth ~args 2))
               (setv g!failed True)
               (break)))
           (unless g!failed (do ~@body))))
  (setv n (str name))
  (setv o {:f fn-checked :desc desc})
  `(assoc ops ~n ~o))


(defn find-op [op]
  (if (in op ops)
    (:f (get ops op))
    (fn [s m t]
      (print (.format "Unknown op {} called" op) :file sys.stderr)
      (.write s {"status" ["done"] "id" (.get m "id")} t))))
