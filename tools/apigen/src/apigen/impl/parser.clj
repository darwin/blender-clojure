(ns apigen.impl.parser
  (:require [clojure.string :as string]
            [clojure.walk :refer [postwalk]]
            [com.rpl.specter :as s]
            [apigen.impl.status :as status]
            [apigen.impl.helpers :refer :all]
            [apigen.impl.lexer :refer [parse-params-signature parse-param-soup]]
            [apigen.impl.reader :refer [list-xml-files filter-xml-files retain-xml-files read-xml-data]])
  (:import [clojure.data.xml.node Element]))

; -- basic xml element predicates ------------------------------------------------------------------------------------------

(defn is-el?
  ([o] (instance? Element o))
  ([o tag] (and (is-el? o) (= (:tag o) tag))))

(defn has-attr? [<el> attr val]
  (assert (is-el? <el>))
  (= (s/select-first [:attrs attr] <el>) val))

(defn is-obj-type? [<el> type]
  (assert (is-el? <el>))
  (has-attr? <el> :objtype type))

(defn is-function? [<el>]
  (assert (is-el? <el>))
  (is-obj-type? <el> "function"))

; -- xml traversal helpers --------------------------------------------------------------------------------------------------

(defn xml-path [& path]
  (let [drill-down (fn [path-segment]
                     (if (and (keyword? path-segment) (= (namespace path-segment) "tag"))
                       (let [tag (keyword (name path-segment))]
                         [:content s/ALL #(is-el? % tag)])
                       [path-segment]))
        navigation (mapcat drill-down path)]
    (apply s/comp-paths navigation)))

; navigates to all content values recursively
(def content-values (s/recursive-path [] p (s/if-path is-el? [:content s/ALL p] s/STAY)))

; -- document processing ----------------------------------------------------------------------------------------------------

(defn collect-content [<el>]
  (assert (is-el? <el>))
  (s/select content-values <el>))

(declare extract-docs)

(defn process-doc [<el>]
  (assert (is-el? <el>))
  (let [tag (:tag <el>)]
    (case tag
      :title [:title (collect-content <el>)]
      :paragraph [:paragraph (collect-content <el>)]
      :note [:note (extract-docs <el>)]
      :seealso [:see-also (collect-content <el>)]
      :literal_block [:literal-block (collect-content <el>)]
      :section [:section (extract-docs <el>)]
      nil)))

(defn extract-docs [<el>]
  (assert (is-el? <el>))
  (let [children (s/select [:content s/ALL is-el?] <el>)]
    (keep process-doc children)))

(defn find-document-section [<document>]
  (assert (is-el? <document> :document))
  (s/select-first (xml-path :tag/section) <document>))

(defn extract-function-docs [<desc>]
  (assert (is-el? <desc> :desc))
  (let [<desc-content> (s/select-first (xml-path :tag/desc_content) <desc>)]
    (extract-docs <desc-content>)))

; unfortunately xml contains generated html content for parameters
; we have to use some heuristics and parse this html content to get back structured information
(defn extract-param-info [<paragraph>]
  (assert (is-el? <paragraph> :paragraph))
  (let [content-soup (map string/trim (collect-content <paragraph>))]
    (parse-param-soup (string/join " " content-soup))))

(defn extract-function-params-from-soup [<desc>]
  (assert (is-el? <desc> :desc))
  (let [parameters? (xml-path :tag/field_name :content s/FIRST #(= % "Parameters"))
        <field> (s/select-first (xml-path :tag/desc_content :tag/field_list :tag/field (s/selected? parameters?))
                                <desc>)
        params (s/select (s/walker #(is-el? % :paragraph)) <field>)]
    (keep extract-param-info params)))

(defn extract-function-name [<desc>]
  (assert (is-el? <desc> :desc))
  (or (s/select-first (xml-path :tag/desc_signature :tag/desc_name :content s/FIRST) <desc>)
      (s/select-first (xml-path :tag/desc_signature :attrs :fullname) <desc>)))

(defn extract-function-params-from-signature [<desc-signature>]
  (assert (is-el? <desc-signature> :desc_signature))
  (let [contents (s/select (xml-path :tag/desc_parameterlist :tag/desc_parameter :content) <desc-signature>)
        signature (string/trim (string/join "," (flatten contents)))]
    (if-not (string/blank? signature)
      (parse-params-signature signature))))

(defn fill-in-holes [soup-param-info param]
  (let [name (:name param)]
    (cond-> param
            (= name "*") (merge soup-param-info))))

(defn reduce-params-soup [params soup]
  (let [* (fn [a soup-param-info]
            (let [name (:name soup-param-info)]
              (assert name)
              (s/transform [s/ALL (s/selected? #(= (:name %) name))]
                           #(merge % soup-param-info)
                           a)))]
    (->> (reduce * params soup)
         (map fill-in-holes soup))))

(def fn-name-re #"(.*)\((.*)\):")

(defn clean-function-name* [name]
  (if-some [m (re-matches fn-name-re name)]
    (get m 1)
    name))

(defn clean-function-name [name]
  (-> (clean-function-name* name)
      (string/trim)))

(defn prepare-param [name]
  {:name (string/trim name)})

(defn extract-function-params-from-name [name]
  (if-some [m (re-matches fn-name-re name)]
    (->> (string/split (get m 2) #",")
         (map prepare-param))))

(defn extract-function-params [<desc> fn-name]
  (assert (is-el? <desc> :desc))
  (try
    (let [<desc-signature> (s/select-first (xml-path :tag/desc_signature) <desc>)
          params-from-signature (extract-function-params-from-signature <desc-signature>)
          params-from-name (if (nil? params-from-signature)
                             (extract-function-params-from-name fn-name))
          params-from-soup (extract-function-params-from-soup <desc>)
          base-params (or params-from-signature params-from-name (list))]
      (try
        (realize-deep (reduce-params-soup base-params params-from-soup))
        (catch Throwable e
          (throw (ex-info (str "trouble reducing params soup") {:desc   <desc>
                                                                :soup   params-from-soup
                                                                :params base-params} e)))))
    (catch Throwable e
      (throw (ex-info (str "trouble extracting function params\n" (pprint-xml-element-data <desc>)) {:desc <desc>} e)))))

(defn process-function [<desc>]
  (assert (is-el? <desc> :desc))
  (let [name (extract-function-name <desc>)
        docs (extract-function-docs <desc>)
        params (extract-function-params <desc> name)]
    {:type   :function
     :name   (clean-function-name name)
     :docs   docs
     :params params}))

(defn find-descs [<section>]
  (assert (is-el? <section> :section))
  (s/select (s/walker #(is-el? % :desc)) <section>))

(defn extract-desc-id [<desc>]
  (or (s/select-first (xml-path :tag/desc_signature :attrs :ids) <desc>) "?"))

(defn process-desc [<desc>]
  (status/info (str "processing desc '" (extract-desc-id <desc>) "'"))
  (assert (is-el? <desc> :desc))
  (let [obj-type (s/select-first [:attrs :objtype] <desc>)]
    (case obj-type
      "function" (process-function <desc>)
      ; TODO: handle other desc types
      {:type obj-type
       :xml  (s/select-first (xml-path :tag/desc_signature :attrs) <desc>)})))

(defn extract-useful-info [<document>]
  (assert (is-el? <document> :document))
  (let [<section> (find-document-section <document>)
        descs (find-descs <section>)]
    {:docs  (extract-docs <section>)
     :descs (keep process-desc descs)}))

(defn parse-xml-data-xf [reporter]
  (let [* (fn [[file-key <document>]]
            (binding [status/*reporter* reporter]
              (status/info (str "processing xml document '" file-key "'"))
              (try
                (assert (is-el? <document> :document))
                [file-key (realize-deep (extract-useful-info <document>))]
                (catch Throwable e
                  (throw (ex-info (str "unable to parse document '" file-key "'") {:doc  <document>
                                                                                   :file file-key} e))))))]
    (keep *)))

(defn parse-xml-data [xml-data & [reporter]]
  (let [xf (parse-xml-data-xf reporter)]
    (into {} xf xml-data)))

; ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

(defn find-desc-by-name [data name]
  (let [all-descs (apply concat (map :descs (vals data)))]
    (some #(if (= (:name %) name) %) all-descs)))

(comment
  (namespace :tag/section)

  (do
    (def data (atom nil))
    (do
      (reset! data (read-xml-data (-> (list-xml-files "../.workspace/xml")
                                      ;(filter-xml-files #".*types.*")
                                      ;(filter-xml-files #".*bmesh\.utils.*")
                                      ;(retain-xml-files #".*ops.info.*")
                                      ;(retain-xml-files #".*ops.mesh.*")
                                      (retain-xml-files #".*bpy.utils.*")
                                      ;(retain-xml-files #".*bgl.*")
                                      )))
      (keys @data)
      )
    )
  (do
    (map type (vals @data)))
  (do
    (def data2 (atom nil))
    (do (reset! data2 (parse-xml-data @data))
        (find-desc-by-name @data2 "register_tool")))
  )
