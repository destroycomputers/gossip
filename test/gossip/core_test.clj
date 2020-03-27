(ns gossip.core-test
  (:require [clojure.test :refer :all]
            [gossip.core :refer :all]))

(deftest arithmetics
  (is (= (+ 2 3) 5)))