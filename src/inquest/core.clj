(ns inquest.core
  "Functions to add monitoring to existing vars."
  (:require [medley.core :refer [dissoc-in]]))

(def ^:private reporters (atom {}))

(defn- make-report [key map]
  (into {:time   (System/nanoTime)
         :thread (.getId (Thread/currentThread))
         :key    key}
        map))

(defn- dispatch! [reporters report]
  (doseq [r reporters] (r report)))

(defn monitor
  "Takes a function and a unique key, and returns a new function that will
  report on its operation. A report is a map that contains the following
  keys:

    :time   - the system time in nanoseconds
    :thread - the thread ID
    :target - the target being monitored
    :state  - one of :enter, :exit or :throw

  Additionally there may be the following optional keys:

    :args      - the arguments passed to the var (only in the :enter state)
    :return    - the return value from the var   (only in the :exit state)
    :exception - the thrown exception            (only in the :throw state)

  Reports are sent to reporters that can be registered with the monitor key
  using add-reporter."
  [func key]
  (with-meta
    (fn [& args]
      (if-let [rs (vals (@reporters key))]
        (do (dispatch! rs (make-report key {:state :enter, :args args}))
            (try
              (let [ret (apply func args)]
                (dispatch! rs (make-report key {:state :exit, :return ret}))
                ret)
              (catch Throwable th
                (dispatch! rs (make-report key {:state :throw, :exception th}))
                (throw th))))
        (apply func args)))
    {::original func
     ::key      key}))

(defn unmonitor
  "Remove monitoring from a function."
  [func]
  (-> func meta ::original (or func)))

(defn add-reporter
  "Add a reporter function to the function associated with monitor-key. The
  reporter-key should be unique to the reporter."
  [monitor-key reporter-key reporter]
  (swap! reporters assoc-in [monitor-key reporter-key] reporter))

(defn remove-reporter
  "Remove a reporter function identified by reporter-key, from the function
  associated with monitor-key."
  [monitor-key reporter-key]
  (swap! reporters dissoc-in [monitor-key reporter-key]))

(defn inquest
  "A convenience function for monitoring many vars with the same reporter
  function. The return value is a zero-argument function that, when called,
  will remove all of the monitoring added for the inquest.

  See the monitor function for an explanation of the reporter function."
  [vars reporter]
  (let [key (gensym "inquest-")]
    (doseq [v vars]
      (alter-var-root v monitor v)
      (add-reporter v key reporter))
    (fn []
      (swap! reporters dissoc key)
      (doseq [v vars]
        (alter-var-root v unmonitor)))))
