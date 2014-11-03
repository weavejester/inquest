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
      (let [cbs @callbacks]
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

(defn- add-callback [f var callback]
  (if-let [cbs (-> f meta ::callbacks)]
    (do (swap! cbs conj callback) f)
    (wrap-monitor f var (atom #{callback}))))

(defn monitor [var callback]
  (alter-var-root var add-callback var callback))

(defn unmonitor [var]
  (alter-var-root var (fn [f] (::original (meta f) f))))
