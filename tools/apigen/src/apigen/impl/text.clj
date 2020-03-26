(ns apigen.impl.text
  (:require [clojure.string :as string])
  (:import (java.util.regex Pattern)))

; ---------------------------------------------------------------------------------------------------------------------------

; some stuff stolen from cuerdas lib

(defn lines [s]
  (string/split s #"\n|\r\n"))

(defn unlines [s]
  (string/join "\n" s))

(defn trim
  ([s] (trim s "\n\t\f\r "))
  ([s chs]
   (when (string? s)
     (let [re-str (str "[" (Pattern/quote ^String chs) "]")
           re-str (str "^" re-str "+|" re-str "+$")]
       (as-> (re-pattern re-str) rx
             (string/replace s rx ""))))))
