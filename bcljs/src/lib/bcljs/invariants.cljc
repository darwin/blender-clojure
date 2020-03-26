(ns bcljs.invariants
  (:require [clojure.string :as string]))

; here comes shared code between apigen and bcljs compiler
; mostly we want to ensure same naming conventions
; so that they don't disagree if work has to be done on both sides

(def ns-prefix "bcljs")

; -- munging ----------------------------------------------------------------------------------------------------------------

; taken from https://github.com/clojure/clojurescript/blob/master/src/main/clojure/cljs/analyzer.cljc
(def js-reserved-set
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
(defn munge-if-js-reserved [name]
  (if (contains? js-reserved-set name)
    (str name "_")
    name))

; TODO: revisit this
(def clj-reserved-set
  #{})

(defn munge-if-clj-reserved [name]
  (if (contains? clj-reserved-set name)
    (str name "_")
    name))

(defn safe-clj-ns-file-name [clojure-name]
  ; TODO: look into clojure/clojurescript source code how they munge this
  (string/replace clojure-name "-" "_"))

(defn clojure-name [python-name]
  (-> python-name
      (string/replace "_" "-")
      (munge-if-clj-reserved)))

;(defn js-name [name]
;  (munge-if-js-reserved name))

(defn safe-ns-part [ns-part]
  (->> ns-part
       (clojure-name)
       (munge-if-clj-reserved)
       (munge-if-js-reserved)))

(defn safe-ns-name [ns-name]
  (->> (string/split ns-name #"\.")
       (map safe-ns-part)
       (string/join ".")))

(defn safe-ns-file-path* [ns-name]
  (->> (string/split ns-name #"\.")
       (map safe-ns-part)
       (map safe-clj-ns-file-name)
       (string/join "/")))

(defn safe-ns-file-path [ns-name ext]
  (str (safe-ns-file-path* ns-name) ext))

(defn py-module-name->ns-name [py-module-name]
  (safe-ns-name (str ns-prefix "." py-module-name)))

(defn safe-clj-symbol [name]
  ; TODO: we should be more defensive here
  (symbol (clojure-name name)))

; -- type specs -------------------------------------------------------------------------------------------------------------

(defn params-type-spec-var-name [fn-name]
  (str "*" (safe-clj-symbol fn-name) "-params"))

; -- module -----------------------------------------------------------------------------------------------------------------

(defn build-module [py-module-name ns-name module-data]
  {:py-name py-module-name
   :ns-name ns-name
   :params  module-data})

(defn get-module-name [module]
  (:py-name module))

(defn get-module-ns-name [module]
  (:ns-name module))

(defn get-module-params [module]
  (:params module))
