(require [hy.contrib.walk [let]])
(import [bclj.env-info [describe-environment]])

(defn make-version [&optional [major 0] [minor 0] [incremental 0]]
  {"major"          major
   "minor"          minor
   "incremental"    incremental
   "version-string" (.join "." (map str [major minor incremental]))})

(defn prepare-version-and-env-info []
  (let [env-info (describe-environment)]
    (.format "HyREPL: {}" env-info)))
