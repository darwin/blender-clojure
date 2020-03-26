(ns apigen.impl.generator
  (:require [clojure.walk :refer [postwalk]]
            [clojure.string :as string]
            [camel-snake-kebab.core :refer :all]
            [bcljs.invariants :as invariants]
            [apigen.impl.word-wrap :refer [wrap]]
            [apigen.impl.types]
            [apigen.impl.docstring :refer [format-docstring]]
            [apigen.impl.reader :refer [list-xml-files filter-xml-files retain-xml-files read-xml-data]]
            [apigen.impl.parser :refer [parse-xml-data]]
            [apigen.impl.writer :refer [write-sources!]]
            [apigen.impl.helpers :refer :all]
            [apigen.impl.status :as status])
  (:import (apigen.impl.types DocString CodeComment PrettyEDN ReaderTag)))

; ---------------------------------------------------------------------------------------------------------------------------

(def max-clojure-fn-arity 20)

(defn format-docs [docs]
  (let [docstring (format-docstring docs)]
    (if-not (string/blank? docstring)
      (DocString. (format-docstring docs)))))

(defn gen-clj-ns [ns-name docstring]
  (let [ns-name-symbol (symbol ns-name)]
    `(~'ns ~ns-name-symbol ::nl
       ~@(if (some? docstring) [docstring ::nl])
       (:refer-clojure :only ~'[defmacro declare]) ::nl
       (:require ~'[bcljs.compiler :refer [emit]]))))

(defn gen-cljs-ns [ns-name]
  (let [ns-name-symbol (symbol ns-name)]
    `(~'ns ~ns-name-symbol ::nl
       (:require-macros [~ns-name-symbol]) ::nl
       (:require ~'[bcljs.compiler]))))

(defn gen-module-declaration []
  `(~'declare ~'mod))

(defn gen-module [module-info]
  `(~'def ~'mod ::nl
     ~(PrettyEDN. module-info)))

(defn has-default? [param]
  (some? (:default param)))

(defn gen-function-arity [name params]
  (let [names (map invariants/safe-clj-symbol (map :name params))]
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
        macro-name (invariants/safe-clj-symbol name)
        macro-name2 (str macro-name "+")]
    `(~'defmacro ~macro-name ::nl
       ~@(if docstring [docstring ::nl])
       ~@(interpose ::nl (map (partial gen-function-arity name) param-arities))
       ~@(if (> (count params) max-available-arity)
           [(CodeComment. (str "for more parameters use " macro-name2))]))))

(defn gen-ops-function [desc]
  (let [{:keys [name docs _params]} desc
        docstring (format-docs docs)
        macro-name (invariants/safe-clj-symbol name)]
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

(defn is-ops-module? [module]
  (-> (invariants/get-module-name module)
      (string/starts-with? "bpy.ops.")))

(defn gen-desc [module desc]
  (case (:type desc)
    :function (if (is-ops-module? module)
                (gen-ops-function desc)
                (gen-function desc))
    (status/warn (str "skipping desc '" (:name desc) "'\n" (print-xml-element-data desc)))))

(defn try-gen-desc [module desc]
  (try
    (realize-deep (gen-desc module desc))
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

(defn gen-descs [module descs]
  (keep (partial try-gen-desc module) descs))

(defn gen-module-data [descs]
  (apply merge (keep prepare-module-data descs)))

(defn gen-clj [api module]
  (let [{:keys [descs docs]} api
        ns-name (invariants/get-module-ns-name module)
        file-path (invariants/safe-ns-file-path ns-name ".clj")
        ns-docstring (format-docs docs)
        generated-descs (gen-descs module descs)
        parts (concat [(gen-clj-ns ns-name ns-docstring)
                       (gen-module-declaration)]
                      generated-descs
                      [(gen-module module)])]
    [file-path (emit parts)]))

(defn convert-nested-vectors-to-js [v]
  (if (vector? v)
    [(ReaderTag. "js") (vec (mapcat convert-nested-vectors-to-js v))]
    (do
      (assert (or (string? v) (nil? v)) (str "unexpected " (pr-str v)))
      [v])))

(defn gen-cljs-params-type-spec [[fn-name params-type-spec]]
  (let [name (symbol (invariants/params-type-spec-var-name fn-name))
        js-params-type-spec (convert-nested-vectors-to-js params-type-spec)]
    `(~'def ~name ~@js-params-type-spec)))

(defn gen-cljs-params-type-specs [params-data]
  (keep gen-cljs-params-type-spec params-data))

(defn gen-cljs [_api module]
  (let [ns-name (invariants/get-module-ns-name module)
        params (invariants/get-module-params module)
        file-path (invariants/safe-ns-file-path ns-name ".cljs")
        generated-param-type-specs (gen-cljs-params-type-specs params)
        parts (concat [(gen-cljs-ns ns-name)]
                      [(CodeComment. (str "note: in :advanced build unused params below should get elided"
                                          " by Google Closure Compiler"))]
                      generated-param-type-specs)]
    [file-path (emit parts)]))

; ---------------------------------------------------------------------------------------------------------------------------

(defn generate-files-for-table [api py-module-name]
  (let [{:keys [descs]} api
        ns-name (invariants/py-module-name->ns-name py-module-name)
        module-data (gen-module-data descs)
        module (invariants/build-module py-module-name ns-name module-data)]
    [(gen-clj api module)
     (gen-cljs api module)]))

; ---------------------------------------------------------------------------------------------------------------------------

(defn generate-xf [reporter]
  (let [* (fn [[file-key api]]
            (binding [status/*reporter* reporter]
              (status/info (str "generating files for '" (name file-key) "'"))
              (realize-deep (generate-files-for-table api (name file-key)))))]
    (mapcat *)))

(defn generate [apis & [reporter]]
  (let [xf (generate-xf reporter)]
    (into {} xf apis)))

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

