(ns inquest.core)

(defn- report [callbacks message]
  (doseq [c callbacks] (c message)))

(defn- wrap-monitor [f var callbacks]
  (with-meta
    (fn [& args]
      (report @callbacks {:var var, :args args})
      (apply f args))
    {::original f
     ::callbacks callbacks}))

(defn- add-callback [f var callback]
  (if-let [cbs (-> f meta ::callbacks)]
    (do (swap! cbs conj callback) f)
    (wrap-monitor f var (atom #{callback}))))

(defn monitor [var callback]
  (alter-var-root var add-callback var callback))

(defn unmonitor [var]
  (alter-var-root var (comp ::original meta)))
