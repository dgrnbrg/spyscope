(ns spyscope.core
  (:require [puget.printer :as pp]
            [clojure.string :as str]
            [clj-time.core :as time]
            [clj-time.format :as fmt]))

(defn- indent
  "Indents a string with `n` spaces."
  [n string]
  (let [indent (str/join (repeat n " "))]
    (->> (str/split string #"\n")
    (map (partial str indent))
    (str/join "\n"))))

(defn pretty-render-value
  "Prints out a form and some extra info for tracing/debugging.

  Prints the last `n` stack frames"
  [form meta]
  (let [now (time/now)
        nses-regex (:nses meta)
        n (or (:fs meta) 1)
        frames-base (->> (ex-info "" {})
                 .getStackTrace
                 seq
                 (drop 2))
        frames (if nses-regex
                 (filter (comp (partial re-find nses-regex) str)
                         frames-base)
                 frames-base)
        frames (->> frames
                 (take n)
                 (map str)
                 (reverse))

        value-string (pp/cprint-str form)

        ;Are there multiple trace lines?
        multi-trace? (> n 1)

        ;Indent if it's a multi-line structure
        value-string (if (or (> (count value-string) 40)
                             (.contains value-string "\n"))
                       (str "\n" (indent 2 value-string))
                       value-string)

        prefix (str/join "\n" frames)]
    {:message (str
                (when multi-trace?
                  (str (str/join (repeat 40 \-)) \newline))
                prefix
                (when-let [time? (:time meta)]
                  (str " " (fmt/unparse (if (string? time?)
                                          (fmt/formatter time?)
                                          (fmt/formatters :date-hour-minute-second))
                                        now))
                  )
                (when-let [marker (:marker meta)]
                  (str " " marker))
                (when (or (not (contains? meta :form))
                          (:form meta))
                  (str " " (pr-str (::form meta))))
                " => " value-string)
     :frame1 (str (first frames-base))}))

(defn print-log
  "Reader function to pprint a form's value."
  [form]
  `(doto ~form pp/cprint))

(def ^{:internal true} trace-storage (agent {:trace [] :generation 0}))

(defn trace
  "Reader function to store detailed information about a form's value at runtime
  into a trace that can be queried asynchronously."
  [form]
  `(let [f# ~form
         value# (pretty-render-value f#
                                     ~(assoc (meta form)
                                        ::form (list 'quote form)))]
     (when ~(::print? (meta form))
       (print (str (:message value#) "\n")))
     (send-off trace-storage
               (fn [{g# :generation t# :trace :as storage#}]
                 (assoc storage#
                   :trace
                   (conj t# (assoc value#
                              :generation g#)))))
     f#))

(defn print-log-detailed
  "Reader function to pprint a form's value with some extra information."
  [form]
  (letfn [(print [m] (assoc m ::print? true))]
    (->> form
      meta
      print
      (with-meta form)
      trace)))

  ; (defn fib
  ;   "Fibonacci number generator--an experimental tracing candidate"
  ;   ([x]
  ;   (fib (dec x) x))
  ;   ([n x]
  ;   (if (zero? n) x (fib #spy/t ^{:form true} (dec n)
  ;                         #spy/t ^{:form true} (* n x)))))
