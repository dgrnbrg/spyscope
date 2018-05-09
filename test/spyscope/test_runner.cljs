(ns spyscope.test-runner
  (:require [doo.runner :refer-macros [doo-tests]]
            [spyscope.tests]))

(doo-tests 'spyscope.tests)
