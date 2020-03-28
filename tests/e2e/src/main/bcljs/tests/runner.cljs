(ns bcljs.tests.runner
  (:require-macros [bcljs.tests.runner :refer [with-test-runner-printing]])
  (:require [cljs.test]
            [bcljs.tests.suites.base]))

(def ^:dynamic *exit-to-system* false)

(defmethod cljs.test/report [:cljs.test/default :end-run-tests] [m]
  (when *exit-to-system*
    (let [code (if (cljs.test/successful? m) 0 1)]
      (js/bclj.system_exit code))))

(defn run-tests []
  (with-test-runner-printing
    (println "~~~~~~~ RUNNING TESTS ~~~~~~~")
    (cljs.test/run-all-tests #"bcljs\.tests\.suites\..*")
    (println "~~~~~~ DONE WITH TESTS ~~~~~~")))

(defn -main []
  ; this code is executed from external test runner
  (binding [*exit-to-system* true]
    (run-tests)))

; ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

(comment
  (run-tests)
  )
