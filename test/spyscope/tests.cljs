(ns spyscope.tests
  (:require [cljs.test :refer-macros [deftest is]]
            [spyscope.core]
            [spyscope.repl]))

(deftest print-log-test
  (is (= "6\n"
         (with-out-str #spy/p (+ 1 2 3)))))

(deftest print-log-detailed-test
  (spyscope.repl/trace-clear)
  (is (re-matches #"spyscope\.tests\.print_log_detailed_test\.cljs.*\(\+ 1 2 3\) => 6"
                  (with-out-str #spy/d (+ 1 2 3)))))

(deftest trace-test
  (spyscope.repl/trace-clear)
  #spy/t (* 1 2 3)
  (is (re-matches #"spyscope\.tests\.trace_test\.cljs.*\(\* 1 2 3\) => 6\n"
                  (with-out-str (spyscope.repl/trace-query)))))