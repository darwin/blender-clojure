(ns apigen.impl.docstring
  (:require [clojure.string :as string]
            [apigen.impl.word-wrap :refer [wrap]]
            [apigen.impl.text :as text]))

(def ^:dynamic *indent* 0)
(def ^:dynamic *columns* nil)
(def indent-atom "  ")

(defn format-nl []
  "\n")

(defn format-indent
  ([] (format-indent *indent*))
  ([indent] (apply str (repeat indent indent-atom))))

(defn prefix-lines [prefix lines]
  (concat [(first lines)]
          (map #(if (empty? %) "" (str prefix %)) (rest lines))))

(defn prefix-text [text prefix]
  (->> text
       (text/lines)
       (prefix-lines prefix)
       (text/unlines)))

(defn reflow-text [text indent & [max-columns]]
  (cond-> text
          (some? max-columns) (wrap (- max-columns indent))
          true (prefix-text (format-indent indent))))

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
      :section (format-section data)
      :note (format-note data)
      :see-also (format-see-also data)
      (throw (ex-info (str "unable to format doc of kind '" kind "'") {:doc-item item})))))

; -- API --------------------------------------------------------------------------------------------------------------------

(defn format-docstring [docs & [indent columns]]
  (try
    (binding [*indent* (or indent 0)
              *columns* (or columns 120)]
      (string/trim (format-docs docs)))
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

  )
