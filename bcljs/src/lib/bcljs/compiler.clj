(ns bcljs.compiler
  (:require [bcljs.shared :as shared]
            [bcljs.invariants :as invariants]))

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
  (let [spec (shared/find-param-type-spec (invariants/python-key key) type-specs)]
    (assert (some? spec))
    [key (convert-value-statically val spec)]))

(defn gen-marshalled-kw-args-statically [kw-args param-specs]
  ; TODO: we can check for param names typos
  (assert (map? kw-args))
  (let [args (->> kw-args
                  (map (partial apply-type-conversion-statically param-specs))
                  (map marshal-kv-arg)
                  (mapcat identity))]
    `(~'js-obj ~@args)))

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

(defn gen-fn [fn-name module _form args]
  (let [module-name (invariants/get-module-name module)
        js-symbol (symbol "js" (str module-name "." fn-name))]
    `(~js-symbol ~@args)))

(defn gen-op-fn [fn-name module _form args]
  (let [[pos-args kw-args] args
        module-name (invariants/get-module-name module)
        py-call (symbol "js" "bclj.pycall")
        js-symbol (symbol "js" (str module-name "." fn-name))
        param-specs (get-in module [:params fn-name])
        marshalled-kw-args (gen-marshalled-kw-args kw-args fn-name module param-specs)]
    `(~py-call ~js-symbol (~'array ~@pos-args) ~marshalled-kw-args)))

(defn emit [kind name module form & args]
  (case kind
    :fn (gen-fn name module form args)
    :op-fn (gen-op-fn name module form args)
    (throw (ex-info (str "bcljs emit: don't know how to emit " kind) {:name name :module module :form form :args args}))))

; ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

(defmacro emit-m [& args]
  (apply emit args))

(comment

  (macroexpand-1 '(emit-m :fn "fn_name" {:name "bpy.ops.module"} {} a b 1))
  (macroexpand-1 '(emit-m :op-fn "fn_name" {:name "bpy.ops.module"} {} ["context"] {:align       :world
                                                                                    :translation [1 2 3]}))
  (macroexpand-1 '(emit-m :op-fn "fn_name" {:name "bpy.ops.module"} {} ["context"]))

  (macroexpand-1 '(emit-m :op-fn "fn_name" {:name "bpy.ops.module"} {} [] (identity {:align       :world
                                                                                     :translation [1 2 3]})))
  )
