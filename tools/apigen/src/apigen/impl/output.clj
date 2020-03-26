(ns apigen.impl.output
  (:require [clojure.string :as string]
            [clojure.pprint :as pprint]
            [apigen.impl.types]
            [apigen.impl.helpers :as helpers]
            [apigen.impl.word-wrap :refer [wrap]]
            [cljfmt.core :as cljfmt])
  (:import (java.io StringWriter)
           (apigen.impl.types DocString CodeComment PrettyEDN ReaderTag)))

(def ^:const INDENT-CHAR " ")

; ---------------------------------------------------------------------------------------------------------------------------

(defn reformat [source]
  (try
    (cljfmt/reformat-string source {:remove-consecutive-blank-lines? false
                                    :indents                         cljfmt/default-indents})
    (catch Exception e
      (println "====== !!! formatting error !!! ======\n" source)
      (throw e))))

; this is ugly but pprint does not offer easy way how to determine current indentation level
(defn determine-current-out-str-indent [string-writer]
  ;(.print System/out (str (type string-writer)))
  (assert (instance? StringWriter string-writer))
  (let [buffer (.getBuffer string-writer)
        last-nl-index (.lastIndexOf buffer "\n")]
    (if (= last-nl-index -1)
      0
      (inc (- (.length buffer) last-nl-index)))))

(defn prefix-block [prefix lines]
  (concat [(first lines)] (map #(if (empty? %) "" (str prefix %)) (rest lines))))

(defn lines [s]
  (string/split s #"\n|\r\n"))

(defn unlines [s]
  (string/join "\n" s))

(defn prefix-text [prefix text]
  (->> text
       (lines)
       (prefix-block prefix)
       (unlines)))

(defn indent-str [indent]
  (apply str (repeat indent INDENT-CHAR)))

(defn enhanced-pprint-dispatch [writer v]
  (cond
    (instance? DocString v)
    (let [current-indent (determine-current-out-str-indent writer)
          effective-indent (+ current-indent 1)
          prefixed-text (prefix-text (indent-str effective-indent) (.-text v))]
      (print (str \" prefixed-text \")))

    (instance? CodeComment v)
    (print (str "\n; " (.-text v) "\n"))

    (instance? PrettyEDN v)
    (print (helpers/pprint-edn-as-str (.-data v) 120))

    (instance? ReaderTag v)
    (print (str "#" (.-tag v)))

    (= v :apigen.impl.generator/nl)
    (print "\n")

    :else
    (pprint/simple-dispatch v)))

(defn pprint-without-right-margin [code]
  (let [writer (StringWriter.)]
    (binding [*out* writer
              pprint/*print-right-margin* nil
              pprint/*print-pprint-dispatch* (partial enhanced-pprint-dispatch writer)]
      (pprint/pprint code))
    (-> (str writer))))

(defn remove-consecutive-blank-lines [s]
  (string/replace s #"[\n\r|\n]{2,}" "\n\n"))

; -- API --------------------------------------------------------------------------------------------------------------------

(defn pprint [stuff]
  (-> (string/join "\n" (map pprint-without-right-margin stuff))
      (reformat)
      (remove-consecutive-blank-lines)))
