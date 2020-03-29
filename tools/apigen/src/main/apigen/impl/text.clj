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

(defn pad-right
  ([s w] (pad-right s w " "))
  ([s w ch]
   (let [n (- w (count s))]
     (if (pos? n)
       (apply str s (repeat n ch))
       s))))

(defn append-dot-if-missing [s]
  ; we want to respect trailing whitespace
  (if-some [m (re-matches #"(?s)(.*?)([^.])(\s*)" s)]
    (str (get m 1) (get m 2) "." (get m 3))
    s))

; ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

(comment
  (append-dot-if-missing "x")
  (append-dot-if-missing "abc")
  (append-dot-if-missing "abc  ")
  (append-dot-if-missing "abc\n  efg\n      ")
  )
