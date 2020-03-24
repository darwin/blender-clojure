(ns apigen.impl.reader
  (:require [clojure.data.xml :as xml]
            [clojure.java.io :as io]
            [clojure.string :as string]
            [apigen.impl.helpers :refer :all]
            [apigen.impl.status :as status]))

(def doctype-re #"<!DOCTYPE.*?>")

(defn patch-xml-content [xml-content]
  (string/replace-first xml-content doctype-re ""))

(defn is-xml-file? [file]
  (string/ends-with? (.getName file) ".xml"))

(defn strip-xml-file-extension [file]
  (assert (is-xml-file? file))
  (let [name (.getName file)]
    (subs name 0 (- (count name) 4))))

(defn read-xml [file]
  (-> (slurp file)
      (patch-xml-content)                                                                                                     ; DOCTYPE causing troubles for our XML reader, we delete it
      (xml/parse-str)))

; -- API --------------------------------------------------------------------------------------------------------------------

(defn list-xml-files [dir]
  (remove (complement is-xml-file?) (file-seq (io/file dir))))

(defn filter-xml-files [files filter]
  (let [matches? (fn [file]
                   (re-matches filter (.getName file)))]
    (remove matches? files)))

(defn retain-xml-files [files filter]
  (let [matches? (fn [file]
                   (re-matches filter (.getName file)))]
    (remove (complement matches?) files)))

(defn read-xml-data-xf [reporter]
  (let [read (fn [file]
               (binding [status/*reporter* reporter]
                 (status/info (str "reading xml '" file "'"))
                 (let [key (keyword (strip-xml-file-extension file))
                       data (realize-deep (read-xml file))]
                   [key data])))]
    (map read)))

(defn read-xml-data [files & [reporter]]
  (let [xform (read-xml-data-xf reporter)]
    (into {} xform files)))

; ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

(comment
  (list-xml-files "../.workspace/xml")

  (-> (list-xml-files "../.workspace/xml")
      (filter-xml-files #".*types.*")
      (retain-xml-files #".*bmesh.*"))

  (read-xml-data-xf nil)

  (let [working-set (-> (list-xml-files "../.workspace/xml")
                        (filter-xml-files #".*types.*")
                        (filter-xml-files #".*bmesh\.utils.*")
                        (retain-xml-files #".*bmesh.*"))]
    (keys (read-xml-data working-set)))

  )
