(ns inquest.core
  "Functions to add monitoring to existing vars."
  (:require [medley.core :refer [dissoc-in]]))

(def reporters
  "An atom that maps targets to reporters."
  (atom {}))

(defn- make-report [state target key value]
  {:time   (System/nanoTime)
   :thread (.getId (Thread/currentThread))
   :state  state
   :target target
   key     value})

(defn- report! [state target key value]
  (let [report (make-report state target key value)]
    (doseq [reporter (vals (@reporters target))]
      (reporter report))))

(defn- wrap-reporting [func target]
  (if (-> func meta ::original)
    func
    (with-meta
      (fn [& args]
        (report! :enter target :args args)
        (try
          (let [ret (apply func args)]
            (report! :exit target :return ret)
            ret)
          (catch Throwable th
            (report! :throw target :exception th)
            (throw th))))
      {::original func})))

(defn monitor
  "Monitors an existing var, passing reports to a reporter function. A report
  is a map consisting of at least the following keys:

    :time   - the system time in nanoseconds
    :thread - the thread ID
    :state  - one of :enter, :exit or :throw
    :var    - the var being monitored

  Additionally there may be the following optional keys:

    :args      - the arguments passed to the var (only in the :enter state)
    :return    - the return value from the var   (only in the :exit state)
    :exception - the thrown exception            (only in the :throw state)

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
      (alter-var-root var (comp ::original meta)))))

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
