(ns bpg.dev
  (:require [bcljs.bpy.ops.mesh :as mesh]
            [bcljs.bpy.ops.object :as object]
            [bpg.helpers :refer [try-silently]]))

(defn clear-scene! []
  (js/console.log "Clearing scene...")
  (try-silently (object/mode-set {:mode :object}))
  (object/select-all {:action :select})
  (object/delete))

(def PI js/Math.PI)

(defn deg [d]
  (* (/ PI 180) d))

(defn add-torus []
  (mesh/primitive-torus-add
    {:align    :world
     :location [0 0 0]
     :rotation [(deg 45) (deg 45) 0]}))

(clear-scene!)

(mesh/primitive-torus-add
  {:align    :cursor
   :location [0 -2 0]})

(mesh/primitive-torus-add
  {:align    :world
   :location [0 0 0]
   :rotation [(deg 45) (deg 45) 0]})

(mesh/primitive-torus-add
  (identity {:align    :world
             :location [-2 0 2]
             :rotation [(deg 45) (deg 45) 0]}))

;(doseq [i (range 40)]
;  (mesh/primitive-torus-add
;    {:location [(- i 20) (- i 20) i]}))

(comment
  (add-torus)

  )
