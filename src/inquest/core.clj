(ns inquest.core)

(defn- wrap-monitor [f var]
  (fn [& args]
    (prn {:var var, :args args})
    (apply f args)))

(defn monitor [var]
  (alter-var-root var wrap-monitor var))
