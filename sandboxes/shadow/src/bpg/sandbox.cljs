(ns bpg.sandbox
  (:require [applied-science.js-interop :as j]))

(js/console.log "Hello from bpg.sandbox")

(defn ^:dev/after-load start []
  (js/console.log "reloaded"))

(defn init []
  (js/console.log "bpg.sandbox.init() called!"))

(defn active-object []
  js/bpy.context.active_object)

(defn add-torus []
  (js/bpy.ops.mesh.primitive_torus_add))

(defn add-text [s]
  (js/bpy.ops.object.text_add)
  (let [o (active-object)]
    (j/assoc-in! o [:data :body] s)
    o))

(defn throw! [x]
  (throw (ex-info x {:some "data"})))
