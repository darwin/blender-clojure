(ns apigen.impl.helpers
  (:require [clojure.string :as string]
            [clojure.pprint :as pprint]
            [apigen.impl.word-wrap :refer [wrap]]
            [com.rpl.specter :as s]))

; ---------------------------------------------------------------------------------------------------------------------------

(defn pprint-edn-as-str [code columns]
  (binding [pprint/*print-right-margin* columns]
    (with-out-str
      (pprint/pprint code))))

(defn realize [v]
  (doall v))

(defn should-realize? [o]
  (or (map? o)
      (vector? o)
      (list? o)
      (set? o)))

(defn realize-deep [v]
  (s/transform (s/walker should-realize?) realize v))

(defn pprint-xml-element-data [v]
  (let [data (s/transform (s/walker string?)
                          #(if (string/blank? (string/trim %)) s/NONE %)
                          v)]
    (pprint-edn-as-str data 126)))
