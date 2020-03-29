(ns apigen.cli
  (:refer-clojure :exclude [parse-opts])
  (:require [clojure.string :as string]
            [clojure.tools.cli :refer [parse-opts]]
            [clojure.pprint :refer [print-table]]
            [apigen.impl.reader :as reader]
            [apigen.impl.parser :as parser]
            [apigen.impl.generator :as generator]
            [apigen.impl.writer :as writer]
            [clojure.java.io :as io]))

(def cli-options
  [["-i" "--input " "Input API XML dir" :default ".workspace/xml"]
   ["-o" "--output " "Output dir for generated cljs sources" :default ".workspace/gen"]
   ["-l" "--logfile PATH" "Output log file"]
   [nil "--only SUBSTR" "Process only files containing a substring (or any from space-separated list of strings)"]
   [nil "--except SUBSTR" "Process only files not containing a substring (or any from space-separated list of strings)"]
   ["-h" "--help"]])

(defn usage [options-summary]
  (string/join \newline ["Takes API XML file and generates bcljs library files."
                         ""
                         "Usage: apigen [options]"
                         ""
                         "Options:"
                         options-summary]))

; -- helpers ----------------------------------------------------------------------------------------------------------------

(defn error-msg [errors]
  (str "The following errors occurred while parsing your command:\n\n"
       (string/join \newline errors)))

(defn exit [status msg]
  (println msg)
  (System/exit status))

; -- logging ----------------------------------------------------------------------------------------------------------------

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

; -- input filtering --------------------------------------------------------------------------------------------------------

(defn set-positive-filter [task]
  (assoc task :enabled true))

(defn set-negative-filter [task]
  (assoc task :enabled false))

(defn filter-via-only [task only]
  (let [task-name (:name task)
        candidates (string/split only #"\s+")
        match (some #(if (.contains task-name %) %) candidates)]
    (if match
      (assoc task :enabled true
                  :enabled-reason (if (not= match only)
                                    (str "enabled because matching '" match "' via --only '" only "'")
                                    (str "enabled because matching --only '" only "'")))
      task)))

(defn filter-via-except [task except]
  (let [task-name (:name task)
        candidates (string/split except #"\s+")
        match (some #(if (.contains task-name %) %) candidates)]
    (if match
      (assoc task :enabled false
                  :enabled-reason (if (not= match except)
                                    (str "disabled because matching '" match "' via --except '" except "'")
                                    (str "disabled because matching --except '" except "'")))
      task)))

(defn filter-via-include [task include]
  (let [task-name (:name task)
        matching? (some? (re-matches (re-pattern include) task-name))]
    (if matching?
      (assoc task :enabled true
                  :enabled-reason (str "enabled because matching regex --include '" include "'"))
      task)))

(defn filter-via-exclude [task exclude]
  (let [task-name (:name task)
        matching? (some? (re-matches (re-pattern exclude) task-name))]
    (if matching?
      (assoc task :enabled false
                  :enabled-reason (str "disabled because matching regex --exclude '" exclude "'"))
      task)))

(defn filter-xml-file [options file]
  (let [{:keys [only except include exclude]} options
        task {:name (str file)}
        filter (cond-> (set-positive-filter task)
                       (or (some? only) (some? include)) (set-negative-filter)
                       (some? only) (filter-via-only only)
                       (some? include) (filter-via-include include)
                       (some? except) (filter-via-except except)
                       (some? exclude) (filter-via-exclude exclude))]
    (if (:enabled filter)
      file)))

; -- worker -----------------------------------------------------------------------------------------------------------------

(defn get-worker-xf [out-dir]
  (comp (reader/read-xml-data-xf report!)
        (parser/parse-xml-data-xf report!)
        (generator/generate-xf report!)
        (writer/write-sources-xf out-dir report!)))

(defn overlook
  ([] :ok)
  ([v] v)
  ([a v] (if (and (= a :ok) (= v :ok))
           :ok
           :fail)))

(defn work! [options]
  (println)
  (let [{:keys [input output logfile]} options]
    (with-log logfile
      (report! :log (pr-str options))
      (report! :log "")
      (let [all-xml-files (reader/list-xml-files input)
            xml-files (keep (partial filter-xml-file options) all-xml-files)
            worker-xf (get-worker-xf output)
            result (transduce worker-xf overlook xml-files)]
        (report! :info (str "processed " (count xml-files) " file(s) with result " result))))))

; -- main -------------------------------------------------------------------------------------------------------------------

(defn -main [& args]
  (let [{:keys [options errors summary]} (parse-opts args cli-options)]
    (cond
      (:help options) (exit 0 (usage summary))
      errors (exit 1 (error-msg errors)))
    (work! options)))
