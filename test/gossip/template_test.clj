(ns gossip.template-test
  (:require [clojure.test :refer :all]
            [gossip.template :as t]))

(deftest populate
  (testing "template parameters are substituted"
    (is (= "@name" (t/populate "${1}" {:1 "name"})))
    (is (= "@name" (t/populate "${1}" {:1 "@name"})))
    (is (= "@name @name" (t/populate "${1} ${1}" {:1 "name"})))
    (is (= "@n1 @n2" (t/populate "${1} ${2}" {:1 "n1", :2 "n2"}))))
  (testing "template without parameters left untouched"
    (is (= "" (t/populate "" {})))
    (is (= "text" (t/populate "text" {}))))
  (testing "template missing parameter substituted with empty string"
    (is (= "x y" (t/populate "x ${1}y" {})))))

(deftest check-errors
  (testing "template contains values less than 1"
    (is (= [[::t/at-least-one 0]] (t/check-errors "${0}")))
    (is (= [[::t/at-most-five 6]] (t/check-errors "${1}${2}${3}${4}${5}${6}")))
    (is (= [[::t/contiguous "2, 4"]] (t/check-errors "${1}${3}${5}")))
    (is (= [[::t/contiguous "2 to 99"]
            [::t/at-least-one -1]
            [::t/at-most-five 100]]
           (t/check-errors "${-1}${1}${100}")))))