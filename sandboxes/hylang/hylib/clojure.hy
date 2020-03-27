; our attempt to bend hy to look more like clojure
; TBD

; to include:
; (require [clojure [*]])
; (import [clojure [*]])
; TODO: figure a way how to put this into hy.core.STDLIB

(defn map? [x]
  (isinstance x dict))

(setv EXPORTS
  '[map?])
