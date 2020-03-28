(ns bcljs.tests.runner)

(defmacro with-test-runner-printing [& body]
  `(binding [cljs.core/*print-newline* true
             cljs.core/*print-fn* js/bclj.test_runner_print
             cljs.core/*print-err-fn* js/bclj.test_runner_print_err]
     ~@body))
