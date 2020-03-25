(ns bcljs.compiler
  (:require-macros [bcljs.compiler])
  (:require [bcljs.shared :as shared]))

(declare marshal-val)

(defn marshal-kv-arg [[key val]]
  [(shared/python-key key) (marshal-val val)])

(defn marshal-map-val [val]
  (assert (map? val))
  ; TODO: optimize this
  (let [args (mapcat marshal-kv-arg val)]
    (apply js-obj args)))

(defn marshal-vector-val [val]
  (assert (vector? val))
  ; TODO: optimize this
  (let [items (map marshal-val val)]
    (into-array items)))

(defn marshal-keyword-val [val]
  (shared/python-enum val))

(defn marshal-val [val]
  (cond
    (keyword? val) (marshal-keyword-val val)
    (vector? val) (marshal-vector-val val)
    (map? val) (marshal-map-val val)
    :else val))

(defn marshal-kw-args [kw-args]
  (assert (map? kw-args))
  (let [args (mapcat marshal-kv-arg kw-args)]
    (apply js-obj args)))

