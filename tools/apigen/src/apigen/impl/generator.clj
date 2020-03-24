(ns apigen.impl.generator
  (:require [clojure.walk :refer [postwalk]]
            [clojure.string :as string]
            [camel-snake-kebab.core :refer :all]
            [apigen.impl.word-wrap :refer [wrap]]
            [apigen.impl.types]
            [apigen.impl.docstring :refer [format-docstring]]
            [apigen.impl.reader :refer [list-xml-files filter-xml-files retain-xml-files read-xml-data]]
            [apigen.impl.parser :refer [parse-xml-data]]
            [apigen.impl.writer :refer [write-sources!]]
            [apigen.impl.helpers :refer :all]
            [apigen.impl.status :as status])
  (:import (apigen.impl.types DocString)))

(def ns-prefix "bcljs")

; ---------------------------------------------------------------------------------------------------------------------------

(defn build-safe-ns-parts [ns]
  (->> (string/split ns #"\.")
       (map munge-if-reserved)
       (map kebab-case)
       (remove string/blank?)))

(defn build-safe-ns-file-path [ns ext]
  (str (string/join "/" (map snake-case (build-safe-ns-parts ns))) ext))

(defn build-safe-ns [ns]
  (symbol (string/join "." (build-safe-ns-parts ns))))

; ---------------------------------------------------------------------------------------------------------------------------

(defn safe-name [name]
  (symbol (kebab-case name)))

(defn format-docs [docs]
  (let [docstring (format-docstring docs)]
    (if-not (string/blank? docstring)
      (DocString. (format-docstring docs)))))

(defn gen-clj-ns [name docstring]
  `(~'ns ~name ::nl
     ~@(if (some? docstring) [docstring ::nl])
     (:refer-clojure :only ~'[def defmacro]) ::nl
     (:require ~'[bcljs.callgen :refer [emit]])))

(defn gen-cljs-ns [name]
  `(~'ns ~name ::nl
     (:require-macros [~name]) ::nl
     (:require ~'[bcljs.core])))

(defn gen-module [module-info]
  `(~'def ~'mod ~module-info))

(defn has-default? [param]
  (some? (:default param)))

(defn gen-function-arity [name params]
  (let [names (map safe-name (map :name params))]
    `([~@names] ::nl
      (~'emit :fn ~name ~'mod ~'&form ~@names))))

(defn gen-function [desc]
  (let [{:keys [name docs params]} desc
        max-arity (count params)
        min-arity (count (take-while (complement has-default?) params))
        arities (range min-arity (inc max-arity))
        param-arities (map #(take % params) arities)
        docstring (format-docs docs)]
    `(~'defmacro ~(safe-name name) ::nl
       ~@(if docstring [docstring ::nl])
       ~@(interpose ::nl (map (partial gen-function-arity name) param-arities)))))

(defn gen-desc [desc]
  (case (:type desc)
    :function (gen-function desc)
    (status/warn (str "skipping desc '" (:name desc) "'\n" (print-xml-element-data desc)))))

(defn try-gen-desc [desc]
  (try
    (realize-deep (gen-desc desc))
    (catch Throwable e
      (throw (ex-info (str "trouble generating desc '" (:name desc) "'\n" e) {:desc desc} e)))))

(defn gen-descs [descs]
  (keep try-gen-desc descs))

(defn gen-clj [api-table]
  (let [{:keys [descs docs module]} api-table
        ns (str ns-prefix "." module)
        file-path (build-safe-ns-file-path ns ".clj")
        ns-name (build-safe-ns ns)
        ns-docstring (format-docs docs)
        module-info {:name module}
        parts (concat [(gen-clj-ns ns-name ns-docstring)
                       (gen-module module-info)]
                      (gen-descs descs))]
    [file-path (emit parts)]))

(defn gen-cljs [api-table]
  (let [{:keys [module]} api-table
        ns (str ns-prefix "." module)
        file-path (build-safe-ns-file-path ns ".cljs")
        ns-name (build-safe-ns ns)]
    [file-path (emit [(gen-cljs-ns ns-name)])]))

; ---------------------------------------------------------------------------------------------------------------------------

(defn generate-files-for-table [api-table]
  (status/info (str "generating files for '" (:module api-table) "'"))
  [(gen-clj api-table)
   (gen-cljs api-table)])

; ---------------------------------------------------------------------------------------------------------------------------

(defn generate-files [[file-key api-table]]
  (generate-files-for-table (assoc api-table :module (name file-key))))

(defn generate [api-tables & [reporter]]
  (assert (map? api-tables))
  (binding [status/*reporter* reporter]
    (realize-deep (into {} (mapcat generate-files api-tables)))))

; ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

(defn find-desc-by-name [data name]
  (let [all-descs (apply concat (map :descs (vals data)))]
    (some #(if (= (:name %) name) %) all-descs)))

(comment
  (namespace :tag/section)

  (do
    (do
      (do
        (def data (atom nil))

        (let [working-set (-> (list-xml-files "../.workspace/xml")
                              ;(filter-xml-files #".*types.*")
                              ;(filter-xml-files #".*bmesh\.utils.*")
                              ;(retain-xml-files #".*ops.info.*")
                              (retain-xml-files #".*ops.mesh.*")
                              ;(retain-xml-files #".*bpy.utils.preview.*")
                              ;(retain-xml-files #".*bgl.*")
                              ;(retain-xml-files #".*gotcha.*")
                              )
              xml-data (read-xml-data working-set)]
          (reset! data xml-data)
          (keys @data)))


      (do
        (def data2 (atom nil))
        (reset! data2 (parse-xml-data @data))
        (find-desc-by-name @data2 "average_normals"))


      (do
        (def data3 (atom nil))
        (reset! data3 (generate @data2))
        (map println (interleave (keys @data3) (vals @data3)))

        )
      )
    (do
      (write-sources! "../.workspace/gen" @data3))

    )
  )

