(ns bcljs.shared
  (:require [clojure.string :as string]))

(defn python-name [clojure-name]
  (string/replace clojure-name "-" "_"))

(defn clojure-name [python-name]
  (string/replace python-name "_" "-"))

(defn python-key [clojure-key]
  (-> (name clojure-key)
      (python-name)))

(defn python-enum [val]
  (assert (keyword? val))
  (-> (name val)
      (string/upper-case)))

(defn find-param-type-spec [name type-specs]
  (some (fn [[param-name type-spec]]
          (if (= name param-name)
            type-spec)) type-specs))
