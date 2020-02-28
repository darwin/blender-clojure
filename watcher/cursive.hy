; this makes cursive play nicer with hy files

; cursive does not play well with names containing forward slashes
(defmacro defmacro-g! [&rest a-rest]
  `(defmacro/g! ~@a-rest))

; def some symbols to get cursive indentation working
(defmacro def [name &rest a-rest]
  None)

(def None)
(def defclass)
(def defmain)
(def except)
(def defmacro)
(def defop)
