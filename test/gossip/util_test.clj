(ns gossip.util-test
  (:require [clojure.test :refer :all]
            [gossip.util :as u]))

(deftest from-query
  (testing "simple inputs parsed correctly."
    (is (= "value" (u/from-query {:query-string "q=value"}))))
  (testing "inputs with several parameters are parsed as single value."
    (is (= "value&k=v" (u/from-query {:query-string "q=value&k=v"}))))
  (testing "nil inputs return empty string."
    (is (= "" (u/from-query {})))
    (is (= "" (u/from-query nil))))
  (testing "inputs are decoded."
    (is (= "v v" (u/from-query {:query-string "q=v%20v"})))))

(deftest from-query*
  (testing "simple inputs parsed correctly."
    (is (= {:k "v"} (u/from-query* {:query-string "k=v"}))))
  (testing "multiple kv pairs parsed correctly."
    (is (= {:a "va" :b "vb"}
           (u/from-query* {:query-string "a=va&b=vb"}))))
  (testing "nil inputs return empty map."
    (is (= {} (u/from-query* {})))
    (is (= {} (u/from-query* nil))))
  (testing "inputs are decoded."
    (is (= {:k "v v"}
           (u/from-query* {:query-string "k=v%20v"})))
    (is (= {:k "v&v=v"}
           (u/from-query* {:query-string "k=v%26v%3Dv"})))))

(deftest count-if
  (testing "counts elements of collection for which pred is true."
    (is (= 3 (u/count-if (constantly true) [1 2 3])))
    (is (= 2 (u/count-if even? [1 2 3 4])))
    (is (= 0 (u/count-if even? [1 3 5]))))
  (testing "nil inputs return zero."
    (is (= 0 (u/count-if (constantly true) nil)))))
