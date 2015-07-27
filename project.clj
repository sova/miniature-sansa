


(defproject pachax "0.0.5"
  :description "ph: participatory knowledge archives"
  :url "practicalhuman.com"
  :min-lein-version "2.0.0"
  :repositories {"my.datomic.com" {:url "https://my.datomic.com/repo"
                                   :creds :gpg}}
  :dependencies [[com.datomic/datomic-pro "0.9.5130" :exclusions [joda-time]]
                 [org.clojure/clojure "1.7.0"]
                 [instaparse "1.4.0"]
                 [compojure "1.3.1"]
                 [ring/ring-defaults "0.1.2"]
                 [ring/ring-anti-forgery "1.0.0"]
                 [enlive "1.1.5"]
                 [com.draines/postal "1.11.3"]
                 [crypto-password "0.1.3"]
                 [digest "1.4.4"]]
                 ;[clojurewerkz/titanium "1.0.0-beta1"]]

  :plugins [[lein-ring "0.8.13"]]
  :ring {:handler pachax.handler/app
         :stacktraces? false
         :port 4027}
  :profiles
  {:dev 
   {:Dependencies [[javax.servlet/servlet-api "2.5"]
                        [ring-mock "0.1.5"]]}})
