(ns inquest.core-test
  (:require [clojure.test :refer :all]
            [inquest.core :refer :all]))

(defn foo [x] (+ x 1))

(deftest test-monitor
  (let [reports (atom [])]
    (try
      (monitor #'foo ::test #(swap! reports conj %))
      (is (= (foo 1) 2))
      (is (thrown? ClassCastException (foo "1")))
      (is (= (count @reports) 4))
      (let [r (@reports 0)]
        (is (= (:var r) #'foo))
        (is (= (:state r) :enter))
        (is (= (:args r) '(1))))
      (let [r (@reports 1)]
        (is (= (:var r) #'foo))
        (is (= (:state r) :exit))
        (is (= (:return r) 2)))
      (let [r (@reports 2)]
        (is (= (:var r) #'foo))
        (is (= (:state r) :enter))
        (is (= (:args r) '("1"))))
      (let [r (@reports 3)]
        (is (= (:var r) #'foo))
        (is (= (:state r) :throw))
        (is (= (type (:exception r)) ClassCastException)))
      (finally
        (unmonitor #'foo ::test)))))

(deftest test-unmonitor
  (let [reports (atom [])]
    (monitor #'foo ::test #(swap! reports conj %))
    (unmonitor #'foo ::test)
    (is (= (foo 1) 2))
    (is (= (count @reports) 0))))
