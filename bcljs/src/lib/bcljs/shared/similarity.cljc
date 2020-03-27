(ns bcljs.shared.similarity)

;; this is a simple step function to determine the threshold
;; no need to figure out the numeric function
(defn length->threshold [len]
  (+ (int (/ len 3)) 2))

; ----------------------------------------------------------------------
; similar strings
; taken from https://github.com/bhauman/spell-spec

(defn next-row
  [previous current other-seq]
  (reduce
    (fn [row [diagonal above other]]
      (let [update-val (if (= other current)
                         diagonal
                         (inc (min diagonal above (peek row))))]
        (conj row update-val)))
    [(inc (first previous))]
    (map vector previous (next previous) other-seq)))

(defn levenshtein
  "Compute the levenshtein distance between two sequences."
  [sequence1 sequence2]
  (peek
    (reduce (fn [previous current] (next-row previous current sequence2))
            (map #(identity %2) (cons nil sequence2) (range))
            sequence1)))

(defn similarity-under-threshold [thresh s1 s2]
  (let [dist (levenshtein (str s1) (str s2))]
    (if (<= dist thresh)
      dist)))

(defn similarity-for-strings [s1 s2]
  (let [min-len (apply min (map count [s1 s2]))]
    (similarity-under-threshold (length->threshold min-len) s1 s2)))

(defn find-best-similar [misspelled possible]
  (let [sorted-candidates (->> (map (partial similarity-for-strings misspelled) possible)
                               (map (fn [name similarity] [name similarity]) possible)
                               (filter (comp some? second))
                               (sort-by second))]
    (if-some [[_ first-candidate-similarity] (first sorted-candidates)]
      (->> sorted-candidates
           (take-while (fn [[_ similarity]]
                         (= similarity first-candidate-similarity)))
           (map first))
      (list))))

; ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

(comment

  (map length->threshold (range 50))

  (similarity-for-strings "abcdabc" "abcdaxc")

  (find-best-similar "radix" ["radius" "type" "enter_editmode" "align" "location" "rotation"])
  (find-best-similar "abcdefgh" ["abcdefxh" "axxxefgh" "abcdxxgh" "abcdefghx"])

  )
