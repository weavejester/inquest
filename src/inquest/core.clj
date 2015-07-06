(ns inquest.core
  "Functions to add monitoring to existing vars.")

(defn- report [state var key value]
  {:time   (System/nanoTime)
   :thread (.getId (Thread/currentThread))
   :state  state
   :var    var
   key     value})

(defn- dispatch! [reporters message]
  (doseq [r reporters] (r message)))

(defn- wrap-monitor [f var reporters]
  (with-meta
    (fn [& args]
      (let [rs (vals @reporters)]
        (dispatch! rs (report :enter var :args args))
        (try
          (let [ret (apply f args)]
            (dispatch! rs (report :exit var :return ret))
            ret)
          (catch Throwable th
            (dispatch! rs (report :throw var :exception th))
            (throw th)))))
    {::original f
     ::reporters reporters}))

(defn- monitor-reporters [var]
  (-> var var-get meta ::reporters))

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
    (if-let [reporters (monitor-reporters var)]
      (swap! reporters assoc key reporter)
      (alter-var-root var wrap-monitor var (atom {key reporter})))))

(defn unmonitor
  "Removes the reporter identified by the key from the var, setting it back to
  normal."
  [var key]
  (locking var
    (when-let [reporters (monitor-reporters var)]
      (when (empty? (swap! reporters dissoc key))
        (alter-var-root var (comp ::original meta))))))

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
