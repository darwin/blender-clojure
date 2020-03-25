(ns apigen.impl.helpers
  (:require [clojure.walk :refer [postwalk]]
            [clojure.string :as string]
            [clojure.pprint :as pprint]
            [camel-snake-kebab.core :refer :all]
            [cuerdas.core :as cuerdas]
            [apigen.impl.types]
            [apigen.impl.word-wrap :refer [wrap]]
            [cljfmt.core :as cljfmt]
            [com.rpl.specter :as s]
            [clojure.data.xml])
  (:import (clojure.data.xml.node Element)
           (java.io StringWriter)
           (apigen.impl.types DocString CodeComment PrettyEDN)))

(def ^:const INDENT-CHAR " ")

; ---------------------------------------------------------------------------------------------------------------------------

; taken from https://github.com/clojure/clojurescript/blob/master/src/main/clojure/cljs/analyzer.cljc
(def js-reserved
  #{"arguments" "abstract" "boolean" "break" "byte" "case"
    "catch" "char" "class" "const" "continue"
    "debugger" "default" "delete" "do" "double"
    "else" "enum" "export" "extends" "final"
    "finally" "float" "for" "function" "goto" "if"
    "implements" "import" "in" "instanceof" "int"
    "interface" "let" "long" "native" "new"
    "package" "private" "protected" "public"
    "return" "short" "static" "super" "switch"
    "synchronized" "this" "throw" "throws"
    "transient" "try" "typeof" "var" "void"
    "volatile" "while" "with" "yield" "methods"
    "null" "constructor"})

; to prevent :munged-namespace clojurescript compiler warnings
(defn munge-if-reserved [name]
  (if (js-reserved name)
    (str name "_api")
    name))

(defn pprint-edn-as-str [code columns]
  (binding [pprint/*print-right-margin* columns]
    (with-out-str
      (pprint/pprint code))))

(defn indent-str [indent]
  (apply str (repeat indent INDENT-CHAR)))

(defn prefix-block [prefix lines]
  (concat [(first lines)] (map #(if (empty? %) "" (str prefix %)) (rest lines))))

(defn prefix-text [prefix text]
  (->> text
       (cuerdas/lines)
       (prefix-block prefix)
       (cuerdas/unlines)))

(defn reformat [source]
  (try
    (cljfmt/reformat-string source {:remove-consecutive-blank-lines? false
                                    :indents                         cljfmt/default-indents})
    (catch Exception e
      (println "====== !!! formatting error !!! ======\n" source)
      (throw e))))

(defn remove-consecutive-blank-lines [s]
  (string/replace s #"[\n\r|\n]{2,}" "\n\n"))

(defn post-process [content]
  (-> content
      (reformat)
      (remove-consecutive-blank-lines)))

; this is ugly but pprint does not offer easy way how to determine current indentation level
(defn determine-current-out-str-indent [string-writer]
  ;(.print System/out (str (type string-writer)))
  (assert (instance? StringWriter string-writer))
  (let [buffer (.getBuffer string-writer)
        last-nl-index (.lastIndexOf buffer "\n")]
    (if (= last-nl-index -1)
      0
      (inc (- (.length buffer) last-nl-index)))))

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
    (print (pprint-edn-as-str (.-data v) 120))

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
    (str writer)))

(defn emit [t]
  (-> (string/join "\n" (map pprint-without-right-margin t))
      (reformat)))

(defn patch-snake-case [s]
  (string/replace s #"_([0-9])" "$1"))                                                                                        ; we don't want numbers to be treated as separate

(defn snake-case [s]
  (if (and (string? s) (not (empty? s)))
    (patch-snake-case (->snake_case s))
    (throw (Exception. (str "snake-case: expected non-empty string: '" s "'" (type s))))))

(defn patch-kebab-case [s]
  (string/replace s #"-([0-9])" "$1"))                                                                                        ; we don't want numbers to be treated as separate

(defn kebab-case [s]
  (if (and (string? s) (not (empty? s)))
    (patch-kebab-case (->kebab-case s))
    (throw (Exception. (str "kebab-case: expected non-empty string: '" s "'" (type s))))))

(defn has-any? [coll]
  (not (empty? coll)))

(defn realize [v]
  (doall v))

(defn realize-deep [v]
  (s/transform (s/walker #(and (not (instance? Element %)) (seqable? %)))
               realize
               v))

(defn print-xml-element-data [v]
  (let [data (s/transform (s/walker string?)
                          #(if (string/blank? (string/trim %)) s/NONE %)
                          v)]
    (pprint-edn-as-str data 126)))
