(ns spyscope.repl
  "This contains the query functions suitable for inspecting traces
  from the repl."
  (:require [clojure.string :as str]) 
  (:use [spyscope.core :only [trace-storage]]))

(defn trace-query
  "Prints information about trace results.
  
  With no arguments, this prints every trace from the current generation.
  
  With one numeric argument `generations`, this prints every trace from the previous
  `generations` generations.
  
  With one regex argument `re`, this prints every trace from the current generation
  whose first stack frame matches the regex.
  
  With two arguments, `re` and `generations`, this matches every trace whose stack frame
  matches `re` from the previosu `generations` generations."
  ([]
   (trace-query #".*" 1))
  ([re-or-generations]
   (if (number? re-or-generations)
     (trace-query #".*" re-or-generations)
     (trace-query re-or-generations 1)))
  ([re generations]
   (let [{:keys [generation trace]} @trace-storage
         generation-min (- generation generations)]
     (->> trace
       (filter #(re-find re (:frame1 %)))
       (filter #(> (:generation %) generation-min))
       (map :message)
       (interpose (str/join (repeat 40 "-")))
       (str/join "\n")
       (println)))))

(defn trace-next
  "Increments the generation of future traces."
  []
  #?(:clj (send trace-storage update-in [:generation] inc)
     :cljs (swap! trace-storage update-in [:generation] inc))
  nil)

(defn trace-clear
  "Deletes all trace data so far (used to reduce memory consumption)"
  []
  #?(:clj (send trace-storage (fn [_] {:trace [] :generation 0}))
     :cljs (swap! trace-storage (fn [_] {:trace [] :generation 0})))
  nil)
