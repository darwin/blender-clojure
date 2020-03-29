(ns apigen.impl.docstring
  (:require [clojure.string :as string]
            [apigen.impl.word-wrap :refer [wrap]]
            [apigen.impl.text :as text]
            [bcljs.invariants :as invariants])
  (:import (java.net URLEncoder)))

(def ^:dynamic *indent* 0)
(def ^:dynamic *columns* nil)
(def indent-atom " ")

(defn format-nl []
  "\n")

(defn format-indent
  ([] (format-indent *indent*))
  ([indent] (apply str (repeat indent indent-atom))))

(defn prefix-lines-except-blank-and-first [prefix lines]
  (let [* (fn [s]
            (if-not (string/blank? s)
              (str prefix s)
              ""))]
    (cons (first lines) (map * (rest lines)))))

(defn prefix-text-except-first-line [text prefix]
  (->> text
       (text/lines)
       (prefix-lines-except-blank-and-first prefix)
       (text/unlines)))

(defn reflow-text [text indent & [max-columns]]
  (cond-> text
          (some? max-columns) (wrap (- max-columns indent))
          true (prefix-text-except-first-line (format-indent indent))))

(defn replace-quotes [s]
  (string/replace s #"\"" "'"))

(defn replace-new-lines [s]
  (string/replace s "\n" " "))

(defn format-text-as-is [v]
  (assert (vector? v))
  (assert (every? string? v))
  (->> (string/join "" v)
       (replace-quotes)))

(defn cleanup-text-snippet [s]
  (-> s
      (string/replace #"\s+" " ")
      (string/trim)))

(defn format-text [v]
  (assert (vector? v))
  (assert (every? string? v))
  (->> (map cleanup-text-snippet v)
       (string/join "")
       (replace-quotes)))

(defn format-sentence [v]
  (assert (vector? v))
  (assert (every? string? v))
  (->> (map cleanup-text-snippet v)
       (string/join " ")
       (replace-quotes)
       (replace-new-lines)
       (string/trim)))

(defn format-title [v]
  (let [title (format-sentence v)
        title-prefix (apply str (repeat (inc *indent*) "#"))]
    (str title-prefix " " title (format-nl))))

(defn format-paragraph [v]
  (let [text (-> (format-sentence v)
                 (reflow-text *indent* *columns*))]
    (str (format-nl)
         (format-indent) text (format-nl))))

(defn format-literal-block [v]
  (let [text (-> (format-text-as-is v)
                 (reflow-text *indent*))]
    (str (format-nl)
         (format-indent) "```" (format-nl)
         (format-indent) text (format-nl)
         (format-indent) "```" (format-nl))))

(defn format-doc [v]
  (let [text (-> (format-text-as-is v)
                 (reflow-text *indent*))]
    (str (format-nl)
         (format-indent) text (format-nl))))

(declare format-doc-item)

(defn format-docs [data]
  (string/join (map format-doc-item data)))

(defn format-section [data]
  (assert (sequential? data))
  (binding [*indent* (inc *indent*)]
    (str (format-nl)
         (format-indent) (format-docs data))))

(defn format-note [data]
  (let [simple-note? (every? string? data)
        content (if simple-note?
                  (format-paragraph data)
                  (format-section data))]
    (str (format-nl)
         (format-indent) "Note:" content)))

(defn format-see-also [v]
  (let [sentence (format-sentence v)]
    (str (format-nl)
         (format-indent) "See also: " sentence (format-nl))))

(defn format-doc-item [item]
  (let [[kind data] item]
    (case kind
      :title (format-title data)
      :paragraph (format-paragraph data)
      :literal-block (format-literal-block data)
      :doc (format-doc data)
      :section (format-section data)
      :note (format-note data)
      :see-also (format-see-also data)
      (throw (ex-info (str "unable to format doc of kind '" kind "'") {:doc-item item})))))

(defn format-blender-api-fn-link [module fn-name]
  (let [module-name (invariants/get-module-name module)
        hash-value (str module-name "." fn-name)
        encoded-hash-value (URLEncoder/encode hash-value "utf-8")
        encoded-module-name (URLEncoder/encode module-name "utf-8")]
    (str "https://docs.blender.org/api/current/" encoded-module-name ".html#" encoded-hash-value)))

; -- param docs -------------------------------------------------------------------------------------------------------------

(defn re-seq-pos [pattern string]
  (let [m (re-matcher pattern string)]
    ((fn step []
       (when (. m find)
         (cons {:start (. m start) :end (. m end) :group (. m group)}
               (lazy-seq (step))))))))

(defn collect-bold-words [text]
  (re-seq-pos #" [A-Z][A-Z][A-Z0-9_]+ " text))

(defn split-into-chunks [s words]
  (let [* (fn [a word]
            (let [meat (if (some? word)
                         (subs s (:pos a) (:start word))
                         (subs s (:pos a)))]
              {:pos   (:end word)
               :shelf (:group word)
               :res   (conj (:res a) [(:shelf a) meat])}))
        r (reduce * {:pos   0
                     :shelf nil
                     :res   []} (concat words [nil]))]
    (:res r)))

(defn max-len [coll]
  (if (empty? coll)
    0
    (apply max (map (comp count string/trim) coll))))

(defn format-param-desc [indent column-size param]
  (let [text (or (:doc param) "")
        bold-words (collect-bold-words text)
        bold-word-names (keep :group bold-words)
        names-column-size (max-len bold-word-names)
        chunks (split-into-chunks text bold-words)
        indent-str (text/pad-right "" indent)
        prefix "  "
        sep " - "
        inner-indent (+ indent (count prefix) names-column-size (count sep))
        * (fn [[word doc]]
            (let [clean-doc (text/append-dot-if-missing (string/trim doc))]
              (if (some? word)
                (str indent-str prefix (text/pad-right (string/trim word) names-column-size) sep
                     (reflow-text clean-doc inner-indent column-size))
                (reflow-text clean-doc indent column-size))))
        ]
    (text/unlines (map * chunks))))

(defn format-param-name [param]
  (str ":" (invariants/clojure-name (:name param))))

(defn format-params-doc [params]
  (if (empty? params)
    ""
    (let [names (map format-param-name params)
          names-column-size (max-len names)
          column-separator " - "
          column-prefix "  "
          desc-column-indent (+ (count column-prefix) names-column-size (count column-separator))
          descs (map (partial format-param-desc desc-column-indent 120) params)
          * (fn [name desc]
              (str column-prefix (text/pad-right name names-column-size) column-separator desc))
          lines (map * names descs)]
      (string/join "\n" lines))))

; -- API --------------------------------------------------------------------------------------------------------------------

(defn format-docstring [docs & [indent columns]]
  (try
    (binding [*indent* (or indent 0)
              *columns* (or columns 120)]
      (format-docs docs))
    (catch Throwable e
      (throw (ex-info (str "unable to format docstring") {:docs docs} e)))))

; ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

(comment
  (format-text ["      assas    \n    sasdasdas  \n ooo"])
  (do
    (println (format-docstring [[:title ["abc"]]
                                [:paragraph ["lorem ipsum" "more dasdas"]]
                                [:literal-block ["some python" "code"]]
                                [:section [[:title ["xyz"]]
                                           [:paragraph ["zzz lorem \" ipsum\"" "more dasdas"]]
                                           [:literal-block ["aaa some python" "code"]]
                                           [:note [[:paragraph ["zzz lorem \" ipsum\"" "more dasdas"]]
                                                   [:literal-block ["aaa some python" "code"]]]]]]])))

  (format-params-doc [{:name      "average_type",
                       :default   "CUSTOM_NORMAL",
                       :type-spec "enum in ['CUSTOM_NORMAL','FACE_AREA','CORNER_ANGLE'], (optional)",
                       :doc       "Type, Averaging method CUSTOM_NORMAL Custom Normal, Take Average of vert Normals. FACE_AREA Face Area, Set all vert normals by Face Area. CORNER_ANGLE Corner Angle, Set all vert normals by Corner Angle."}
                      {:name "weight", :default 50, :type-spec "int in [1,100], (optional)", :doc "Weight, Weight applied per face"}
                      {:name      "threshold",
                       :default   0.01,
                       :type-spec "float in [0,10], (optional)",
                       :doc       "Threshold, Threshold value for different weights to be considered equal"}])

  (format-param-desc 10 100 {:doc "Type, Averaging method CUSTOM_NORMAL Custom Normal, Take Average of vert Normals. FACE_AREA Face Area, Set all vert normals by Face Area. CORNER_ANGLE Corner Angle, Set all vert normals by Corner Angle."})

  (reflow-text "Type, Averaging method CUSTOM_NORMAL Custom Normal, Take Average of vert Normals. FACE_AREA Face Area, Set all vert normals by Face Area. CORNER_ANGLE Corner Angle, Set all vert normals by Corner Angle." 10 100)

  (prefix-text-except-first-line "abc\nefg\nijk" "----")

  (format-blender-api-link-fn {:py-name "bpy.ops.object"} "add_named")

  (format-param-desc 10 120 {:name      "average_type",
                             :default   "CUSTOM_NORMAL",
                             :type-spec "enum in ['CUSTOM_NORMAL','FACE_AREA','CORNER_ANGLE'], (optional)",
                             :doc       "Type, Averaging method CUSTOM_NORMAL Custom Normal, Take Average of vert Normals. FACE_AREA Face Area, Set all vert normals by Face Area. CORNER_ANGLE Corner Angle, Set all vert normals by Corner Angle."})

  )
