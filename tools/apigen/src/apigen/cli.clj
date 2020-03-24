(ns apigen.cli
  (:refer-clojure :exclude [parse-opts])
  (:require [clojure.string :as string]
            [clojure.tools.cli :refer [parse-opts]]
            [clojure.pprint :refer [print-table]]
            [apigen.impl.reader :refer [list-xml-files read-xml-data]]
            [apigen.impl.parser :refer [parse-xml-data]]
            [apigen.impl.generator :refer [generate]]
            [apigen.impl.writer :refer [write-sources!]]
            [clojure.java.io :as io]))

(def cli-options
  [["-i" "--input " "Input API XML dir" :default ".workspace/xml"]
   ["-o" "--output " "Output dir for generated cljs sources" :default ".workspace/gen"]
   ["-l" "--logfile PATH" "Output log file"]
   ["-h" "--help"]])

(defn usage [options-summary]
  (string/join \newline ["Takes API XML file and generates bcljs library files."
                         ""
                         "Usage: apigen [options]"
                         ""
                         "Options:"
                         options-summary]))

(defn error-msg [errors]
  (str "The following errors occurred while parsing your command:\n\n"
       (string/join \newline errors)))

(defn exit [status msg]
  (println msg)
  (System/exit status))

(def ^:dynamic *log*)

(def ansi-rewind "\033[1A\033[2K")

(defn report! [channel s]
  (case channel
    :info (println (str ansi-rewind s))
    (:warn :log) (binding [*out* *log*]
                   (println s))))

(defn open-log [name]
  (let [f (io/file name)]
    (io/make-parents f)
    (io/writer f)))

(defn close-log [log]
  (.close log))

(defmacro with-log [logfile & body]
  `(let [logfile# ~logfile]
     (binding [*log* (if logfile#
                       (open-log logfile#)
                       *out*)]
       ~@body
       (if logfile#
         (close-log *log*)))))

(defn run-job! [options]
  (println)
  (let [{:keys [input output logfile]} options]
    (with-log logfile
      (report! :log (pr-str options))
      (report! :log "")
      (let [all-xml-files (list-xml-files input)
            ; TODO: filter xml files via cli params
            xml-data (read-xml-data all-xml-files report!)
            api-data (parse-xml-data xml-data report!)
            generated-files (generate api-data report!)]
        (write-sources! output generated-files report!)
        (report! :info (str "processed " (count all-xml-files) " file(s)"))))))

; -- main -------------------------------------------------------------------------------------------------------------------

(defn -main [& args]
  (let [{:keys [options errors summary]} (parse-opts args cli-options)]
    (cond
      (:help options) (exit 0 (usage summary))
      errors (exit 1 (error-msg errors)))
    (run-job! options)))
