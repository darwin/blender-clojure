(ns bpg.helpers)

(defmacro try-silently [& body]
  `(try
     ~@body
     (catch :default _e#)))
