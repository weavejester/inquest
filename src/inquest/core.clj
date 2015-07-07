(ns inquest.core
  "Functions to add monitoring to existing vars."
  (:require [medley.core :refer [dissoc-in]]))

(def reporters
  "An atom that maps targets to reporters."
  (atom {}))

(defn- make-report [target map]
  (assoc map
         :time   (System/nanoTime)
         :thread (.getId (Thread/currentThread))
         :target target))

(defn- report! [target map]
  (let [report (make-report target map)]
    (doseq [reporter (vals (@reporters target))]
      (reporter report))))

(defn wrap-reporting
  "Takes a function and a target key, and returns a new function that will
  report on its operation. A report is a map that contains the following
  keys:

    :time   - the system time in nanoseconds
    :thread - the thread ID
    :target - the target being monitored
    :state  - one of :enter, :exit or :throw

  Additionally there may be the following optional keys:

    :args      - the arguments passed to the var (only in the :enter state)
    :return    - the return value from the var   (only in the :exit state)
    :exception - the thrown exception            (only in the :throw state)"
  [func target]
  (if (-> func meta ::original)
    func
    (with-meta
      (fn [& args]
        (report! target {:state :enter, :args args})
        (try
          (let [ret (apply func args)]
            (report! target {:state :exit, :return ret})
            ret)
          (catch Throwable th
            (report! target {:state :throw, :exception th})
            (throw th))))
      {::original func
       ::target target})))

(defn unwrap-reporting
  "Remove reporting from a function."
  [func]
  (let [m (meta func)]
    (swap! reporters dissoc (::target m))
    (::original m)))

(defn monitor
  "Monitors the specified target, passing reports to a reporter function.
  The key argument may be anything, and is used with the unmonitor function
  to remove the reporter from the var."
  [var key reporter]
  (locking var
    (swap! reporters assoc-in [var key] reporter)
    (alter-var-root var wrap-reporting var)))

(defn unmonitor
  "Removes the reporter identified by the key from the var, setting it back to
  normal."
  [var key]
  (locking var
    (when (empty? (swap! reporters dissoc-in [var key]))
      (alter-var-root var unwrap-reporting))))

(defn inquest
  "A convenience function for monitoring many vars with the same reporter
  function. The return value is a zero-argument function that, when called,
  will remove all of the monitoring added for the inquest.

  See the monitor function for an explanation of the reporter function."
  [vars reporter]
  (let [key (gensym "inquest-")]
    (doseq [v vars]
      (monitor v key reporter))
    (fn []
      (doseq [v vars]
        (unmonitor v key)))))
