(ns inquest.core
  (:require [com.stuartsierra.component :as component]))

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
            (dispatch! rs (report :exit var :exception th))
            (throw th)))))
    {::original f
     ::reporters reporters}))

(defn- monitor-reporters [var]
  (-> var var-get meta ::reporters))

(defn monitor [var key reporter]
  (locking var
    (if-let [reporters (monitor-reporters var)]
      (swap! reporters assoc key reporter)
      (alter-var-root var wrap-monitor var (atom {key reporter})))))

(defn unmonitor [var key]
  (locking var
    (when-let [reporters (monitor-reporters var)]
      (when (empty? (swap! reporters dissoc key))
        (alter-var-root var (comp ::original meta))))))

(defrecord Inquest [vars reporters]
  component/Lifecycle
  (start [inquest]
    (if (:monitor-key inquest)
      inquest
      (let [k (gensym "inquest-")
            f (fn [msg] (doseq [c reporters] (c msg)))]
        (doseq [v vars] (monitor v k f))
        (assoc inquest :monitor-key k))))
  (stop [inquest]
    (if-let [k (:monitor-key inquest)]
      (doseq [v vars] (unmonitor v k))
      (dissoc inquest :monitor-key))))

(defn inquest [vars reporters]
  (->Inquest (set vars) (set reporters)))
