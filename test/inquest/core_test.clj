(ns inquest.core-test
  (:require [clojure.test :refer :all]
            [inquest.core :refer :all]))

(defn foo [x] (+ x 1))
(defn bar [x] (- x 1))

(deftest test-monitor
  (let [reports (atom [])
        foo'    (monitor foo #'foo)]
    (add-reporter #'foo ::test #(swap! reports conj %))
    (is (= (foo' 1) 2))
    (is (thrown? ClassCastException (foo' "1")))
    (is (= (count @reports) 4))
    (let [r (@reports 0)]
      (is (= (:key r) #'foo))
      (is (= (:state r) :enter))
      (is (= (:args r) '(1))))
    (let [r (@reports 1)]
      (is (= (:key r) #'foo))
      (is (= (:state r) :exit))
      (is (= (:return r) 2)))
    (let [r (@reports 2)]
      (is (= (:key r) #'foo))
      (is (= (:state r) :enter))
      (is (= (:args r) '("1"))))
    (let [r (@reports 3)]
      (is (= (:key r) #'foo))
      (is (= (:state r) :throw))
      (is (= (type (:exception r)) ClassCastException)))))

(deftest test-unmonitor
  (is (= (unmonitor (monitor foo #'foo)) foo)))

(deftest test-inquest
  (let [reports (atom [])
        stop    (inquest [#'foo #'bar] #(swap! reports conj %))]
    (testing "started"
      (try
        (is (= (foo 1) 2))
        (is (= (bar 2) 1))
        (is (= (count @reports) 4))
        (let [r (@reports 0)]
          (is (= (:key r) #'foo))
          (is (= (:state r) :enter))
          (is (= (:args r) '(1))))
        (let [r (@reports 1)]
          (is (= (:key r) #'foo))
          (is (= (:state r) :exit))
          (is (= (:return r) 2)))
        (let [r (@reports 2)]
          (is (= (:key r) #'bar))
          (is (= (:state r) :enter))
          (is (= (:args r) '(2))))
        (let [r (@reports 3)]
          (is (= (:key r) #'bar))
          (is (= (:state r) :exit))
          (is (= (:return r) 1)))
        (finally
          (stop))))
    (testing "stopped"
      (is (= (foo 1) 2))
      (is (= (bar 2) 1))
      (is (= (count @reports) 4)))))
