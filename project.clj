(defproject inquest "0.1.0-SNAPSHOT"
  :description "Non-invasive monitoring library"
  :url "https://github.com/weavejester/inquest"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.5.1"]]
  :aliases {"test-all" ["do"
                        ["test"]
                        ["with-profile" "+1.6" "test"]
                        ["with-profile" "+1.7" "test"]]}
  :profiles
  {:1.6 {:dependencies [[org.clojure/clojure "1.6.0"]]}
   :1.7 {:dependencies [[org.clojure/clojure "1.7.0"]]}})
