(ns pachax.views.request-account
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
   :essay "write an essay describing who you are, your hobbies, and what you would contribute to the community."
   :rules "rules state that you may only have one account. respect this."}

(defn request-content [ email anti-forgery-token ]
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
                {:tag :form,
                 :attrs {:class "submitRequestForm",
                         :action "sendRequestGO",
                         :method "POST"}
                 :content (list
                           {:tag :input
                            :attrs {:name "email"
                                    :class "postrequestemail"
                                    :placeholder "your e-mail, you must have access to the inbox -- used for logging in."}
                            :content nil},
                           {:tag :textarea, 
                            :attrs {:name "request-essay",
                                    :class "post-essay-button", 
                                    :placeholder "qualities we like: scientific training, depth, kindness"}
                            
                            :content nil},
                           {:tag :input,
                            :attrs {:type "hidden"
                                    :name "sender-email"
                                    :value "email"},
                            :content nil},
                           {:tag :input, 
                            :attrs {:type "hidden"
                                    :name "__anti-forgery-token",
                                    :value anti-forgery-token}, 
                            :content nil})})})))

(defn feedback-content-transform [ email anti-forgery-token ]
  (let [feedback-area (eh/select feedback-page [:.feedback])]
    ;;takes the first [only] element named .invite, clones it, fills it with goodness
    (eh/transform feedback-area [:.feedback]
                  (eh/content 
                   (feedback-content email anti-forgery-token)))))


(defn request-page-draw [ email anti-forgery-token ]
  (apply str (eh/emit* 
              (eh/at request-page
                     [:.request] (eh/substitute (request-content-transform email anti-forgery-token))))))
