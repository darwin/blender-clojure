(ns apigen.impl.writer
  (:require [clojure.data.json :refer [pprint]]
            [clojure.java.io :as io]
            [apigen.impl.status :as status]))

; -- API --------------------------------------------------------------------------------------------------------------------

(defn write-sources! [dir files & [reporter]]
  (binding [status/*reporter* reporter]
    (doseq [[path file-content] files]
      (let [full-path (io/file dir path)]
        (status/info (str "writing '" full-path "'"))
        (io/make-parents full-path)
        (spit full-path file-content)))))
