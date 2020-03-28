(ns bcljs.tests.suites.base
  (:require [cljs.test :refer-macros [deftest is testing run-tests]]))

(deftest test-numbers
  (is (= 1 1)))

(deftest test-numbers2
  (is (= 2 2)))
