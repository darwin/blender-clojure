(ns bcljs.runtime
  (:require [bcljs.runtime.marshalling :as marshalling]))

; calls to these functions can be emitted by compiler macros

(def marshal-kw-args marshalling/marshal-kw-args)
