(ns bcljs.compiler.warnings
  (:require [cljs.analyzer]
            [bcljs.shared :as shared]
            [bcljs.compiler.sherlock :as source-scanner]))

(defmethod cljs.analyzer/error-message ::unknown-param [warning-type info]
  (let [{:keys [name suggestion]} info]
    (str "Encountered unknown parameter `" name "` to Blender API. " suggestion)))

(defn prepare-unknown-param-suggestion-str [name possible-names]
  (if (empty? possible-names)
    (str "this function does not accept any keyword parameters")
    (let [suggested-names (shared/suggest-names name possible-names)]
      (if (empty? suggested-names)
        (str "Expected one of " (shared/prepare-or-sentence (shared/clojure-names possible-names)) ".")
        (str "Didn't you mean " (shared/prepare-or-sentence (shared/clojure-names suggested-names)) "?")))))

(defn warn-on-unknown-param! [env location key name possible-names]
  (let [suggestion-str (prepare-unknown-param-suggestion-str name possible-names)
        env-with-updated-position (merge env (source-scanner/attempt-to-determine-source-position (str key) location))]
    (binding [cljs.analyzer/*cljs-warnings* (assoc cljs.analyzer/*cljs-warnings* ::unknown-param true)]
      (cljs.analyzer/warning ::unknown-param env-with-updated-position {:name       key
                                                                        :suggestion suggestion-str}))))
