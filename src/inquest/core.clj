(ns inquest.core)

(defn- wrap-monitor [f var callback]
  (with-meta
    (fn [& args]
      (callback {:var var, :args args})
      (apply f args))
    {::original f}))

(defn monitor [var callback]
  (alter-var-root var wrap-monitor var callback))

(defn unmonitor [var]
  (alter-var-root var (comp ::original meta)))
