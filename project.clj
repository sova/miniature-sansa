


(defproject pachax "0.0.1"
  :description "ravel/braid phase 1 with compojure"
  :url "none so far but maybe sova.so"
  :min-lein-version "2.0.0"
  :repositories {"my.datomic.com" {:url "https://my.datomic.com/repo"
                                   :creds :gpg}}
  :dependencies [[com.datomic/datomic-pro "0.9.5130"]
                 [org.clojure/clojure "1.6.0"]
                 [compojure "1.3.1"]
                 [ring/ring-defaults "0.1.2"]
                 [ring/ring-anti-forgery "1.0.0"]
                 [enlive "1.1.5"]
                 [com.novemberain/monger "2.0.0"]
                 [com.draines/postal "1.11.3"]
                 [crypto-password "0.1.3"]
                 [digest "1.4.4"]]
  :plugins [[lein-ring "0.8.13"]]
  :ring {:handler pachax.handler/app}
  :profiles
  {:dev 
   {:dependencies [[javax.servlet/servlet-api "2.5"]
                        [ring-mock "0.1.5"]]}})
