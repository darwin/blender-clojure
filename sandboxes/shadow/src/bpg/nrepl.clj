(ns bpg.nrepl
  ; this was copied from shadow.user
  ; https://github.com/thheller/shadow-cljs/blob/master/src/main/shadow/user.clj
  (:require
    [clojure.repl :refer (source apropos dir pst doc find-doc)]
    [clojure.java.javadoc :refer (javadoc)]
    [clojure.pprint :refer (pp pprint)]
    [shadow.cljs.devtools.api :as shadow :refer (help)]))

(defn dev-blender! []
  (shadow/repl :sandbox)
  (in-ns 'bpg.dev)
  nil)
