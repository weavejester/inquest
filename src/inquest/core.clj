(ns inquest.core)

(defn- report [state var key value]
  {:time   (System/nanoTime)
   :thread (.getId (Thread/currentThread))
   :state  state
   :var    var
   key     value})

(defn- dispatch [callbacks message]
  (doseq [c callbacks] (c message)))

(defn- wrap-monitor [f var callbacks]
  (with-meta
    (fn [& args]
      (let [cbs (vals @callbacks)]
        (dispatch cbs (report :enter var :args args))
        (try
          (let [ret (apply f args)]
            (dispatch cbs (report :exit var :return ret))
            ret)
          (catch Throwable th
            (dispatch cbs (report :exit var :exception th))
            (throw th)))))
    {::original f
     ::callbacks callbacks}))

(defn- monitor-callbacks [var]
  (-> var var-get meta ::callbacks))

(defn monitor [var key callback]
  (locking var
    (if-let [callbacks (monitor-callbacks var)]
      (swap! callbacks assoc key callback)
      (alter-var-root var wrap-monitor var (atom {key callback})))))

(defn unmonitor [var key]
  (locking var
    (when-let [callbacks (monitor-callbacks var)]
      (when (empty? (swap! callbacks dissoc key))
        (alter-var-root var (comp ::original meta))))))
