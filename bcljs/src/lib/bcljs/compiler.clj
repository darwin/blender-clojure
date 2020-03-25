(ns bcljs.compiler)

(defn get-module-name [module]
  (:name module))

(defn emit-fn [fn-name module _form args]
  (let [module-name (get-module-name module)
        js-symbol (symbol "js" (str module-name "." fn-name))]
    `(~js-symbol ~@args)))

(defn emit-op-fn [fn-name module _form args]
  (let [[pos-args kw-args] args
        module-name (get-module-name module)
        py-call (symbol "js" "bclj.pycall")
        js-symbol (symbol "js" (str module-name "." fn-name))]
    `(~py-call ~js-symbol (~'array ~@pos-args) ~kw-args)))

(defn emit [kind name module form & args]
  (case kind
    :fn (emit-fn name module form args)
    :op-fn (emit-op-fn name module form args)
    (throw (ex-info (str "bcljs emit: don't know how to emit " kind) {:name name :module module :form form :args args}))))

; ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

(defmacro emit-m [& args]
  (apply emit args))

(comment

  (macroexpand-1 '(emit-m :fn "fn_name" {:name "bpy.ops.module"} {} a b 1))
  (macroexpand-1 '(emit-m :op-fn "fn_name" {:name "bpy.ops.module"} {} ["context"] {:align 1
                                                                        :translation [1 2 3]}))
  (macroexpand-1 '(emit-m :op-fn "fn_name" {:name "bpy.ops.module"} {} ["context"]))
  )
