


(defproject pachax "0.0.1"
  :description "ravel/braid phase 1 with compojure"
  :url "http://example.com/FIXME"
  :min-lein-version "2.0.0"
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [compojure "1.3.1"]
                 [ring/ring-defaults "0.1.2"]
                 [enlive "1.1.5"]]
  :plugins [[lein-ring "0.8.13"]]
  :ring {:handler pachax.handler/app}
  :profiles
  {:dev {:dependencies [[javax.servlet/servlet-api "2.5"]
                        [ring-mock "0.1.5"]]}})
