(ns apigen.impl.types
  (:require [clojure.data.json :refer [pprint]]))

(deftype DocString [text])

(deftype CodeComment [text])

(deftype PrettyEDN [data])

(deftype ReaderTag [tag])
