(ns bcljs.compiler
  (:require [bcljs.shared :as shared]
            [bcljs.invariants :as invariants]
            [bcljs.compiler.warnings :as warnings]))

(def ^:dynamic *env*)
(def ^:dynamic *form*)

(declare marshal-val)

(defn marshal-kv-arg [[key val]]
  [(invariants/python-key key) (marshal-val val)])

(defn marshal-map-val [val]
  (assert (map? val))
  (let [args (mapcat marshal-kv-arg val)]
    `(~'js-obj ~@args)))

(defn marshal-vector-val [val]
  (assert (vector? val))
  (let [items (map marshal-val val)]
    `(~'array ~@items)))

(defn marshal-keyword-val [val]
  (shared/python-enum val))

(defn marshal-val [val]
  (cond
    (keyword? val) (marshal-keyword-val val)
    (vector? val) (marshal-vector-val val)
    (map? val) (marshal-map-val val)
    :else val))

(defn convert-value-statically [val spec]
  (cond
    ; TODO: here we should have a plug-able system for static value conversion
    (= spec "xxx") (identity val)
    :else val))

(defn apply-type-conversion-statically [type-specs [key val]]
  (let [python-name (invariants/python-key key)]
    (if-some [spec (shared/find-param-type-spec python-name type-specs)]
      [key (convert-value-statically val spec)]
      (let [possible-names (map first type-specs)]
        (warnings/warn-on-unknown-param! *env* (meta *form*) key python-name possible-names)
        [key val]))))

(defn gen-marshalled-kw-args-statically [kw-args param-specs]
  ; TODO: we can check for param names typos
  (binding [*form* kw-args]

    (assert (map? kw-args))
    (let [args (->> kw-args
                    (map (partial apply-type-conversion-statically param-specs))
                    (map marshal-kv-arg)
                    (mapcat identity))]
      `(~'js-obj ~@args))))

(defn gen-marshalled-kw-args-dynamically [kw-args fn-name module]
  ; TODO: we can check for param names typos
  (let [var-name (invariants/params-type-spec-var-name fn-name)
        ns-name (invariants/get-module-ns-name module)
        params-type-specs-sym (symbol ns-name var-name)]
    `(bcljs.runtime/marshal-kw-args ~kw-args ~params-type-specs-sym)))

(defn gen-marshalled-kw-args [kw-args fn-name module param-specs]
  (cond
    (nil? kw-args) nil
    (map? kw-args) (gen-marshalled-kw-args-statically kw-args param-specs)
    :else (gen-marshalled-kw-args-dynamically kw-args fn-name module)))

(defn gen-fn [module fn-name args]
  (let [module-name (invariants/get-module-name module)
        js-symbol (symbol "js" (str module-name "." fn-name))]
    `(~js-symbol ~@args)))

(defn gen-op-fn [module fn-name args]
  (let [[pos-args kw-args] args
        module-name (invariants/get-module-name module)
        py-call (symbol "js" "bclj.pycall")
        js-symbol (symbol "js" (str module-name "." fn-name))
        param-specs (get-in module [:params fn-name])
        marshalled-kw-args (gen-marshalled-kw-args kw-args fn-name module param-specs)]
    `(~py-call ~js-symbol (~'array ~@pos-args) ~marshalled-kw-args)))

(defn gen* [kind module name args]
  (case kind
    :fn (gen-fn module name args)
    :op-fn (gen-op-fn module name args)
    (throw (ex-info (str "bcljs emit: don't know how to emit " kind) {:name name :module module :args args}))))

(defn gen [env form kind module name & args]
  (binding [*env* env
            *form* form]
    (gen* kind module name args)))

; ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

(comment

  (def m {:py-name "bpy.ops.module"
          :ns-name "bcljs.bpy.ops.module"
          :params  {"add" [["radius" "float in [0,inf], (optional)"]
                           ["type"
                            "enum in ['MESH','CURVE','SURFACE','META','FONT','ARMATURE','LATTICE','EMPTY','GPENCIL','CAMERA','LIGHT','SPEAKER','LIGHT_PROBE'], (optional)"]
                           ["enter_editmode" "boolean, (optional)"]
                           ["align" "enum in ['WORLD','VIEW','CURSOR'], (optional)"]
                           ["location" "float array of 3 items in [-inf,inf], (optional)"]
                           ["rotation" "float array of 3 items in [-inf,inf], (optional)"]]}})

  (gen {} {} :fn m "add" 'a 'b 1)
  (gen {} {} :op-fn m "add" '["context"])

  (gen {} {} :op-fn m "add" '[] '{:align       :world
                                  :translation [1 2 3]})
  (gen {} {} :op-fn m "add" '["context"] '{:align :world
                                           :radix :xxx})

  (gen {} {} :op-fn m "add" '[] '(identity {:align       :world
                                            :translation [1 2 3]}))
  )
