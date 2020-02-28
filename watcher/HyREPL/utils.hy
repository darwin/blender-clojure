(defn make-version [&optional [major 0] [minor 0] [incremental 0]]
  {"major" major
   "minor" minor
   "incremental" incremental
   "version-string" (.join "." (map str [major minor incremental]))})
