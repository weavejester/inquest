(ns inquest.core)

(defn- report [callbacks message]
  (doseq [c callbacks] (c message)))

(defn- wrap-monitor [f var callbacks]
  (with-meta
    (fn [& args]
      (let [cbs @callbacks]
        (report cbs {:state :enter, :var var, :args args})
        (let [ret (apply f args)]
          (report cbs {:state :exit, :var var, :return ret})
          ret)))
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
