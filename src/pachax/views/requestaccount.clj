(ns pachax.views.requestaccount
  (:require [clojure.string]
            [net.cgrand.enlive-html :as eh] :reload
            [pachax.views.usercard :as pvu]
            [pachax.database.dbmethods :as dbm]))

;;replace request.html contents 
(def request-page (eh/html-resource "request.html"))


(def request-text 
  {:title "practicalhuman",
   :subtitle "participatory knowledge archive",
   :email "your email",
   :essay "write an essay describing who you are, your hobbies, and what you would contribute to the community. bonus points for musical artists and bands you enjoy."
   :rules "you may have only one account. respect this."})

(defn request-content [ anti-forgery-token ]
  (let [r-title    (:title    request-text)
        r-subtitle (:subtitle request-text)
        r-email    (:email    request-text)
        r-essay    (:essay    request-text)
        r-rules    (:rules    request-text)]
    (list 
     {:tag :div,
      :attrs {:class "request"},
      :content (list 
                {:tag :div,
                 :attrs {:id (str "request-title")}
                 :content r-title},
                {:tag :div,
                 :attrs {:id (str "request-subtitle")}
                 :content r-subtitle}
                {:tag :div,
                 :attrs {:id (str "request-essay")}
                 :content r-essay}
                {:tag :form,
                 :attrs {:class "submitRequestForm",
                         :action "request",
                         :method "POST"}
                 :content (list
                           {:tag :textarea, 
                            :attrs {:name "request-essay",
                                    :class "essay-area", 
                                    :placeholder "qualities we like: scientific training, depth, kindness"}                            
                            :content nil},
                           {:tag :input
                            :attrs {:name "email"
                                    :class "postrequestemail"
                                    :placeholder "your e-mail, you must have access to the inbox -- used for logging in."}
                            :content nil},
                           {:tag :div,
                            :attrs {:id (str "request-rules")}
                            :content r-rules}
                           {:tag :input,
                            :attrs {:type "submit"
                                    :class "request-submit-button"
                                    :value "send in your application."}
                           :content nil}
                           {:tag :input, 
                            :attrs {:type "hidden"
                                    :name "__anti-forgery-token",
                                    :value anti-forgery-token}, 
                            :content nil})})})))

(defn request-content-transform [ anti-forgery-token ]
  (let [request-area (eh/select request-page [:.request])]
    ;;takes the first [only] element named .request, clones it, fills it with goodness
    (eh/transform request-area [:.request]
                  (eh/content 
                   (request-content anti-forgery-token)))))


(defn request-page-draw [ anti-forgery-token ]
  (apply str (eh/emit* 
              (eh/at request-page
                     [:.request] (eh/substitute (request-content-transform anti-forgery-token))))))
