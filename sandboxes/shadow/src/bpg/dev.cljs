(ns bpg.dev
  (:require [bcljs.bpy.ops.mesh :as mesh]
            [bcljs.bpy.ops.object :as object]))

(defn clear-scene! []
  (js/console.log "Clearing scene...")
  (try
    (object/mode-set {:mode "OBJECT"})
    (catch :default _e))
  (object/select-all {:action "SELECT"})
  (object/delete))

(def PI js/Math.PI)

(defn deg [d]
  (* (/ PI 180) d))

(defn add-torus []
  (mesh/primitive-torus-add
    {:align    "WORLD"
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
             :location [0 0 2]
             :rotation [(deg 45) (deg 45) 0]}))

(comment
  (add-torus)

  )
