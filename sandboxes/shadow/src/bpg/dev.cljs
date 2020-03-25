(ns bpg.dev
  (:require [bcljs.bpy.ops.mesh :as mesh]
            [bcljs.bpy.ops.object :as object]))

(defn clear-scene! []
  (js/console.log "Clearing scene...")
  (try
    (object/mode-set #js {:mode "OBJECT"})
    (catch :default _e))
  (object/select-all)
  (object/delete))

(def PI js/Math.PI)

(defn deg [d]
  (* (/ PI 180) d))

(defn add-torus []
  (mesh/primitive-torus-add
    #js {:align    "WORLD"
         :location #js [0 0 2]
         :rotation #js [0 (deg 45) 0]}))

(clear-scene!)
(add-torus)

(comment
  (add-torus)

  )
