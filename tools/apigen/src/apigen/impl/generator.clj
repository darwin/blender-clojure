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
  (:import (apigen.impl.types DocString CodeComment PrettyEDN)))

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

(def max-clojure-fn-arity 20)

(defn safe-name [name]
  (symbol (kebab-case name)))

(defn format-docs [docs]
  (let [docstring (format-docstring docs)]
    (if-not (string/blank? docstring)
      (DocString. (format-docstring docs)))))

(defn gen-clj-ns [name docstring]
  `(~'ns ~name ::nl
     ~@(if (some? docstring) [docstring ::nl])
     (:refer-clojure :only ~'[defmacro declare]) ::nl
     (:require ~'[bcljs.compiler :refer [emit]])))

(defn gen-cljs-ns [name]
  `(~'ns ~name ::nl
     (:require-macros [~name]) ::nl
     (:require ~'[bcljs.compiler])))

(defn gen-module-declaration []
  `(~'declare ~'mod))

(defn gen-module [module-info]
  `(~'def ~'mod ::nl
     ~(PrettyEDN. module-info)))

(defn has-default? [param]
  (some? (:default param)))

(defn gen-function-arity [name params]
  (let [names (map safe-name (map :name params))]
    `([~@names] ::nl
      (~'emit :fn ~name ~'mod ~'&form ~@names))))

(defn gen-function [desc]
  (let [{:keys [name docs params]} desc
        max-available-arity (- max-clojure-fn-arity 4)                                                                        ; see gen-function-arity
        max-arity (min max-available-arity (count params))
        min-arity (count (take-while (complement has-default?) params))
        arities (range min-arity (inc max-arity))
        param-arities (map #(take % params) arities)
        docstring (format-docs docs)
        macro-name (safe-name name)
        macro-name2 (str macro-name "+")]
    `(~'defmacro ~macro-name ::nl
       ~@(if docstring [docstring ::nl])
       ~@(interpose ::nl (map (partial gen-function-arity name) param-arities))
       ~@(if (> (count params) max-available-arity)
           [(CodeComment. (str "for more parameters use " macro-name2))]))))

(defn gen-ops-function [desc]
  (let [{:keys [name docs _params]} desc
        docstring (format-docs docs)
        macro-name (safe-name name)]
    `(~'defmacro ~macro-name ::nl
       ~@(if docstring [docstring ::nl])
       (~'[] ::nl
         (~'emit :op-fn ~name ~'mod ~'&form ~'[])) ::nl
       (~'[opts] ::nl
         (~'emit :op-fn ~name ~'mod ~'&form ~'[] ~'opts)) ::nl
       (~'[oc opts] ::nl
         (~'emit :op-fn ~name ~'mod ~'&form ~'[oc] ~'opts)) ::nl
       (~'[oc ec opts] ::nl
         (~'emit :op-fn ~name ~'mod ~'&form ~'[oc ec] ~'opts)) ::nl
       (~'[oc ec undo opts] ::nl
         (~'emit :op-fn ~name ~'mod ~'&form ~'[oc ec undo] ~'opts)))))

(defn is-ops-module? [module-info]
  (string/starts-with? (:name module-info) "bpy.ops."))

(defn gen-desc [module-info desc]
  (case (:type desc)
    :function (if (is-ops-module? module-info)
                (gen-ops-function desc)
                (gen-function desc))
    (status/warn (str "skipping desc '" (:name desc) "'\n" (print-xml-element-data desc)))))

(defn try-gen-desc [module-info desc]
  (try
    (realize-deep (gen-desc module-info desc))
    (catch Throwable e
      (throw (ex-info (str "trouble generating desc '" (:name desc) "'\n" e) {:desc desc} e)))))

(defn prepare-param-type-info [param]
  (let [{:keys [name type-spec]} param]
    [name type-spec]))

(defn prepare-fn-module-data [desc]
  (let [{:keys [name params]} desc]
    {name (mapv prepare-param-type-info params)}))

(defn prepare-module-data [desc]
  (case (:type desc)
    :function (prepare-fn-module-data desc)
    (status/warn (str "skipping module-data for desc '" (:name desc) "'\n" (print-xml-element-data desc)))))

(defn gen-descs [module-info descs]
  (keep (partial try-gen-desc module-info) descs))

(defn gen-module-data [descs]
  (apply merge (keep prepare-module-data descs)))

(defn gen-clj [api-table module-info]
  (let [{:keys [descs docs module]} api-table
        ns (str ns-prefix "." module)
        file-path (build-safe-ns-file-path ns ".clj")
        ns-name (build-safe-ns ns)
        ns-docstring (format-docs docs)
        generated-descs (gen-descs module-info descs)
        parts (concat [(gen-clj-ns ns-name ns-docstring)
                       (gen-module-declaration)]
                      generated-descs
                      [(gen-module module-info)])]
    [file-path (emit parts)]))

(defn gen-cljs [api-table]
  (let [{:keys [module]} api-table
        ns (str ns-prefix "." module)
        file-path (build-safe-ns-file-path ns ".cljs")
        ns-name (build-safe-ns ns)]
    [file-path (emit [(gen-cljs-ns ns-name)])]))

; ---------------------------------------------------------------------------------------------------------------------------

(defn generate-files-for-table [api-table]
  (let [{:keys [descs module]} api-table
        module-data (gen-module-data descs)
        module-info {:name   module
                     :params module-data}]
    [(gen-clj api-table module-info)
     (gen-cljs api-table)]))

; ---------------------------------------------------------------------------------------------------------------------------

(defn generate-xf [reporter]
  (let [* (fn [[file-key api-table]]
            (binding [status/*reporter* reporter]
              (status/info (str "generating files for '" (name file-key) "'"))
              (realize-deep (generate-files-for-table (assoc api-table :module (name file-key))))))]
    (mapcat *)))

(defn generate [api-tables & [reporter]]
  (let [xf (generate-xf reporter)]
    (into {} xf api-tables)))

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

