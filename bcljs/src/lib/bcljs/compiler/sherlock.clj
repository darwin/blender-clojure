(ns bcljs.compiler.sherlock
  (:require [clojure.string :as string]
            [clojure.java.io :as io]))

(defn attempt-to-read-resource [path]
  (if (some? path)
    (try
      (slurp (io/resource path))
      (catch Exception _e))))

(defn robust-subs
  ([s start]
   (try
     (subs s start)
     (catch StringIndexOutOfBoundsException _e
       s)))
  ([s start end]
   (try
     (subs s start end)
     (catch StringIndexOutOfBoundsException _e
       s))))

(defn chop-first-line [column lines]
  (cons (robust-subs (first lines) column) (rest lines)))

(defn chop-last-line [end-column lines]
  (concat (butlast lines) [(robust-subs (last lines) 0 end-column)]))

(defn attempt-to-find-source-position [content substr location]
  ; input lines and columns are 1-based
  (let [begin-line (:line location)
        begin-column (:column location)
        end-line (:end-line location)
        end-column (:end-column location)
        lines (inc (- end-line begin-line))
        source-lines (string/split content #"\n|\r\n")
        relevant-source-lines (->> source-lines
                                   (drop (dec begin-line))
                                   (take lines)
                                   (chop-first-line (dec begin-column))
                                   (chop-last-line (dec end-column)))
        ;_ (.println System/out (str "RELEVANT SOURCE LINES\n"
        ;                            (string/replace (string/join "\n" relevant-source-lines) " " ".")))
        * (fn [a line]
            (if-some [i (string/index-of line substr)]
              (let [line-num (:line a)
                    first-line? (= line-num line)]
                (reduced (assoc a :column (if first-line?
                                            (+ begin-column i)
                                            (inc i)))))
              (update a :line inc)))
        lookup (reduce * {:line begin-line} relevant-source-lines)]
    (if (:column lookup)
      lookup
      (select-keys location [:line :column]))))

(defn attempt-to-determine-source-position [substr location]
  (try
    ;(.println System/out (pr-str location))
    (let [source-file (:file location)
          source-content (attempt-to-read-resource source-file)
          source-position (attempt-to-find-source-position source-content substr location)]
      source-position)
    (catch Exception _e
      ; no big deal, this is just nice-to have feature
      (select-keys location [:line :column]))))

; ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

(comment

  (attempt-to-determine-source-position ":align" {:file "bpg/dev.cljs", :line 40, :column 3, :end-line 42, :end-column 36})

  )
