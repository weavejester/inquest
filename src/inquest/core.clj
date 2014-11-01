(ns inquest.core)

(defn- wrap-monitor [f var callback]
  (fn [& args]
    (callback {:var var, :args args})
    (apply f args)))

(defn monitor [var callback]
  (alter-var-root var wrap-monitor var callback))
