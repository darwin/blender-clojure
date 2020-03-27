(ns bcljs.shared
  (:require [clojure.string :as string]
            [bcljs.shared.similarity :as similarity]
            [bcljs.invariants :as invariants]))

(defn python-enum [val]
  (assert (keyword? val))
  (-> (name val)
      (string/upper-case)))

(defn find-param-type-spec [name type-specs]
  (let [* (fn [[param-name type-spec]]
            (if (= name param-name)
              type-spec))]
    (some * type-specs)))

(defn prepare-sentence [coll sep last-sep]
  (cond
    (empty? coll) ""
    (= (count coll) 1) (first coll)
    :else (string/join (concat (interpose sep (butlast coll)) [last-sep (last coll)]))))

(defn prepare-or-sentence [coll]
  (prepare-sentence coll ", " " or "))

(defn clojure-names [names]
  (->> names
       (map invariants/clojure-name)))

(defn suggest-names [misspelled-python-name expected-python-names]
  (similarity/find-best-similar misspelled-python-name expected-python-names))

; ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

(comment

  (suggest-names "abc_defgh" ["abc_defxh" "axx_xefgh" "abc_dxxgh" "abc_defghx"])

  )
