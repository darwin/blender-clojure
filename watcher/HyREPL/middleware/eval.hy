(import types
        sys
        [io [StringIO]])
(import ctypes)
(import traceback)

(import
  [hy.lex [tokenize]]
  [hy.lex.exceptions [LexException]])

(import
  [HyREPL.ops [ops find-op]])

(require [hy.contrib.walk [let]]
         [HyREPL.ops [defop]])

(setv eval-module (types.ModuleType "__main__")) ; Module context for evaluations

(defn format-exception [session msg writer trace]
  (let [exc-type (first trace)
        exc-value (second trace)
        exc-traceback (get trace 2)]
    (setv session.last-traceback exc-traceback)
    (traceback.print_tb exc-traceback)
    (writer {"status"  ["eval-error"]
             "ex"      (. exc-type --name--)
             "root-ex" (. exc-type --name--)
             "id"      (.get msg "id")})
    ; TODO: deal with LexException cases later
    ;(when (instance? LexException exc-value)
    ;  (when (is exc-value.source None)
    ;    (setv exc-value.source ""))
    ;  (setv exc-value (.format "LexException: {}" exc-value.message)))
    (writer {"err" (+ (.strip (str exc-value)) "\n")})))

(defop eval [session msg transport]
       {"doc" "Evaluates code."
        "requires" {"code" "The code to be evaluated"}
        "optional" {"session" (+ "The ID of the session in which the code will"
                                 " be evaluated. If absent, a new session will"
                                 " be generated")
                    "id" "An opaque message ID that will be included in the response"}
        "returns" {"ex" "Type of the exception thrown, if any. If present, `value` will be absent."
                   "ns" (+ "The current namespace after the evaluation of `code`."
                           " For HyREPL, this will always be `Hy`.")
                   "root-ex" "Same as `ex`"
                   "value" (+ "The values returned by `code` if execution was"
                              " successful. Absent if `ex` and `root-ex` are"
                              " present")}}
  (let [code (get msg "code")
        oldout sys.stdout
        tokens None
        writer (fn [x]
                               (assoc x "id" (get msg "id") "session" (get msg "session"))
                               (.write session x transport))
        ]
    (try
      (setv tokens (tokenize code))
      (except []
        (format-exception session msg writer (sys.exc-info))
        (writer {"status" ["done"] "id" (.get msg "id")}))
      (else
        (for [i tokens]
          (let [p (StringIO)]
            (try
              (do
                (setv sys.stdout (StringIO))
                (let [eval-res (eval i (if (instance? dict eval-module)
                                          eval-module
                                          (. eval-module --dict--))
                                        "__main__")]
                  (.write p (str eval-res))))
              (except []
                (setv sys.stdout oldout)
                (format-exception session msg writer (sys.exc-info)))
              (else
                (when (and (= (.getvalue p) "None") (bool (.getvalue sys.stdout)))
                  (writer {"out" (.getvalue sys.stdout)}))
                (writer {"value" (.getvalue p) "ns" (.get msg "ns" "Hy")})))))
          (setv sys.stdout oldout)
          (writer {"status" ["done"]})))))

(defop "load-file" [session msg transport]
       {"doc" "Loads a body of code. Delegates to `eval`"
        "requires" {"file" "full body of code"}
        "optional" {"file-name" "name of the source file, for example for exceptions"
                    "file-path" "path to the source file"}
        "returns" (get (:desc (get ops "eval")) "returns")}
       (let [code (-> (get msg "file")
                     (.split " " 2)
                     (get 2))]
         (print (.strip code) :file sys.stderr)
         (assoc msg "code" code)
         (del (get msg "file"))
         ((find-op "eval") session msg transport)))
