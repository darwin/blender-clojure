(ns bcljs.runtime.marshalling
  (:require [bcljs.shared :as shared]
            [bcljs.invariants :as invariants]))

(declare marshal-val)

(defn marshal-kv-arg [[key val]]
  [(invariants/python-key key) (marshal-val val)])

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

(defn convert-value-dynamically [val spec]
  (cond
    ; TODO: here we should have a plug-able system for dynamic value conversion
    (= spec "xxx") (identity val)
    :else val))

(defn apply-type-conversion-dynamically [specs [key val]]
  (let [spec (shared/find-param-type-spec (invariants/python-key key) specs)]
    (assert (some? spec))
    [key (convert-value-dynamically val spec)]))

(defn marshal-kw-args [kw-args param-spec]
  (assert (map? kw-args))
  (let [args (->> kw-args
                  (map (partial apply-type-conversion-dynamically param-spec))
                  (map marshal-kv-arg)
                  (mapcat identity))]
    (apply js-obj args)))
