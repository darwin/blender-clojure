(ns apigen.impl.status)

(def ^:dynamic *reporter* nil)

; -- REPL detection ---------------------------------------------------------------------------------------------------------

(defn current-stack-trace []
  (.getStackTrace (Thread/currentThread)))

(defn is-repl-stack-element [stack-element]
  (and (= "clojure.main$repl" (.getClassName stack-element))
       (= "doInvoke" (.getMethodName stack-element))))

(defn check-is-in-repl? []
  (some is-repl-stack-element (current-stack-trace)))

(defn is-in-repl? []
  (memoize check-is-in-repl?))

; -- API --------------------------------------------------------------------------------------------------------------------

(defn info [s]
  (if *reporter*
    (*reporter* :info s)
    (if (is-in-repl?)
      (println "I:" s))))

(defn warn [s]
  (if *reporter*
    (*reporter* :warn s)
    (if (is-in-repl?)
      (println "W:" s))))
