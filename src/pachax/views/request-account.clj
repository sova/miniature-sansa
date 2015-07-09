(ns pachax.views.request-account
  (:require [clojure.string]
            [net.cgrand.enlive-html :as eh] :reload
            [pachax.views.usercard :as pvu]
            [pachax.database.dbmethods :as dbm]))

;;replace request.html contents 
(def request-page (eh/html-resource "request.html"))


(def request-text 
  {:title "Send feedback to the creator.", 
   :content "Thanks for using this participatory knowledge archive.  We are very open to feedback and constructive ideas.  Please keep it polite, and feel free to submit ideas you might have on how to make the site or the community better."})

;;feedback-text populating
(defn feedback-content [ email anti-forgery-token ]
  (let [feedback-title (get feedback-text :title)
        feedback-content (get feedback-text :content)]
    (list 
     {:tag :div,
      :attrs {:class "feedback-description"},
      :content (list 
                {:tag :div,
                 :attrs {:id (str "feedback-title")}
                 :content feedback-title},
                 {:tag :div,
                  :attrs {:id (str "feedback-content")}
                  :content feedback-content},
                 {:tag :form,
                 :attrs {:class "submitFeedbackForm",
                         :action "sendFeedbackGO",
                         :method "POST"}
                 :content (list
                           {:tag :textarea
                            :attrs {:name "feedback"
                                    :class "postcontentfeedback"
                                    :placeholder "ideas, suggestions, enhancements, all welcome."}
                            :content nil},
                           {:tag :input, 
                            :attrs {:name "feedback-submit",
                                    :class "post-feedback-button", 
                                    :value "send some happy feedback"
                                    :type "submit"}, 
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


(defn feedback-page-draw [ email anti-forgery-token ]
  (apply str (eh/emit* 
              (eh/at feedback-page
                     [:.request] (eh/substitute (request-content-transform email anti-forgery-token))))))
