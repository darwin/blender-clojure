(ns bcljs.tests.suites.pyv8
  (:require [cljs.test :refer-macros [deftest is testing run-tests]]
            [goog.object :as gobj]))

(deftest symbol-property-access
  ; this was crashing
  ; https://github.com/area1/stpyv8/issues/8
  (let [prop-name (.-iterator js/Symbol)
        pyobj (js/test.get_simple_python_sequence)]
    (is (= ::null (gobj/get pyobj prop-name ::null)))))
