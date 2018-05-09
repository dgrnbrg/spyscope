(ns spyscope.core
  (:require [clojure.string :as str]
            #?(:clj  [clj-time.core :as time]
               :cljs [cljs-time.core :as time])
            #?(:clj  [clj-time.format :as fmt]
               :cljs [cljs-time.format :as fmt])
            #?(:clj  [puget.printer :as pp]))
  #?(:clj  (:require [net.cgrand.macrovich :as macrovich])
     :cljs (:require-macros [net.cgrand.macrovich :as macrovich])))

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

        frames-base #?(:clj  (->> (ex-info "" {})
                                  .getStackTrace
                                  seq
                                  (drop 2))
                       :cljs (->> (ex-info "" {})
                                  .-stack
                                  (str/split-lines)
                                  (drop 2)
                                  (drop-while #(str/includes? % "ex_info"))
                                  (map #(str/replace % "    at " ""))))

        frames (if nses-regex
                 (filter (comp (partial re-find nses-regex) str)
                         frames-base)
                 frames-base)

        frames #?(:clj  (->> frames
                             (remove #(clojure.string/includes? % "spyscope.core"))
                             (take n)
                             (map str)
                             (reverse))
                  :cljs (->> frames
                             (remove #(clojure.string/includes? % "spyscope$core"))
                             (remove #(not (clojure.string/includes? % "(")))
                             (take n)
                             (reverse)))

        value-string #?(:clj  (pp/cprint-str form)
                        :cljs (str form))

        ;Are there multiple trace lines?
        multi-trace? (> n 1)

        ;Indent if it's a multi-line structure
        value-string (if (or (> (count value-string) 40)
                             (str/includes? value-string "\n"))
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
                                        now)))
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
  `(macrovich/case
     :clj  (doto ~form pp/cprint)
     :cljs (doto ~form println)))

; Trace storage - an atom rather than an agent is used in cljs.
#?(:clj  (def ^{:internal true} trace-storage (agent {:trace [] :generation 0}))
   :cljs (def ^{:internal true} trace-storage (atom {:trace [] :generation 0})))

(defn trace
  "Reader function to store detailed information about a form's value at runtime
  into a trace that can be queried asynchronously."
  [form]
  `(let [f# ~form
         value# (pretty-render-value f#
                                     ~(assoc (meta form)
                                        ::form (list 'quote form)))]
     (when ~(::print? (meta form))
       (macrovich/case
         :clj  (print (str (:message value#) "\n"))
         :cljs (print (str (:message value#)))))
     ((macrovich/case :clj send-off :cljs swap!) trace-storage
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
