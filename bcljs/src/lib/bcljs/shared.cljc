(ns bcljs.shared
  (:require [clojure.string :as string]
            [bcljs.invariants :as invariants]))

(defn python-enum [val]
  (assert (keyword? val))
  (-> (name val)
      (string/upper-case)))

(defn find-param-type-spec [name type-specs]
  (some (fn [[param-name type-spec]]
          (if (= name param-name)
            type-spec)) type-specs))
