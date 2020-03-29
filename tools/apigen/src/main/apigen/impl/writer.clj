(ns apigen.impl.writer
  (:require [clojure.data.json :refer [pprint]]
            [clojure.java.io :as io]
            [apigen.impl.status :as status]))

; -- API --------------------------------------------------------------------------------------------------------------------

(defn write-sources-xf [dir reporter]
  (let [* (fn [[path file-content]]
            (binding [status/*reporter* reporter]
              (let [full-path (io/file dir path)]
                (status/info (str "writing '" full-path "'"))
                (io/make-parents full-path)
                (spit full-path file-content)
                :ok)))]
    (map *)))

(defn write-sources! [dir files & [reporter]]
  (let [xf (write-sources-xf dir reporter)]
    (into [] xf files)))

