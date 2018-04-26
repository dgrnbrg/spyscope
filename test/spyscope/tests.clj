(ns spyscope.tests
  (:require [clojure.test :refer :all]
            [puget.printer :as puget]
            [clojure.string :as str]
            [spyscope.core]
            [spyscope.repl]))

(set! *data-readers*
  {'spy/p spyscope.core/print-log
   'spy/d spyscope.core/print-log-detailed
   'spy/t spyscope.core/trace})

(deftest print-log-test
  (is (= (str (puget/cprint-str 6) "\n")
         (with-out-str #spy/p (+ 1 2 3)))))

(deftest print-log-detailed-test
  (is (re-matches #"(?s)spyscope\.tests.*\.invoke\(tests\.clj:.*\) \(\+ 1 2 3\) => .*"
                  (with-out-str #spy/d (+ 1 2 3))))
  (is (str/ends-with? (with-out-str #spy/d (+ 1 2 3))
                      (str (puget/cprint-str 6) "\n"))))

(deftest trace-test
  #spy/t (* 1 2 3)
  (is (re-matches #"(?s)spyscope\.tests.*\.invokeStatic\(tests\.clj:.*\) \(\* 1 2 3\) => .*"
                  (with-out-str (spyscope.repl/trace-query))))
  (is (str/ends-with? (with-out-str (spyscope.repl/trace-query))
                      (str (puget/cprint-str 6) "\n"))))
