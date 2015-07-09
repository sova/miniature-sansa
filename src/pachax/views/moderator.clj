(ns pachax.views.moderator
  (:require [net.cgrand.enlive-html :as eh] :reload
            [pachax.views.usercard :as pvu]
            [pachax.database.dbmethods :as dbm]))

;;replace blurb contents 
(def moderator-page (eh/html-resource "moderator.html"))

(def number-of-feedbacks-to-show 100)

;;feedback area populating
(defn feedback-sample-content [feedbackID feedback-map]
  (let [fid (:fid feedback-map)
        sender (:email feedback-map)
        content (:content feedback-map)]
    (list
     {:tag :div, 
      :attrs {:id (str "feedback" feedbackID)
              :class "feedbackin"}
      :content (list 
                {:tag :div,
                 :attrs {:id (str "feedback-sender" feedbackID),
                         :class (str "inner-feedback-sender")}
                 :content sender},
                {:tag :div,
                 :attrs {:id (str "feedback-content" feedbackID),
                         :class (str "inner-feedback-content")},
                 :content content},
                {:tag :div,
                 :attrs {:id (str "feedback-fid" feedbackID),
                         :class (str "inner-feedback-fid")}
                 :content fid})})))

(defn feedback-content-transform []
  (let [feedback-area (eh/select moderator-page [:.feedback])
        get-unread-feedback (dbm/get-unread-feedback)
        num-feedback (count get-unread-feedback)]
    ;;takes the first [only] element named .feedback, clones it, fills it with goodness
    (eh/transform feedback-area [:.feedback]
       (eh/clone-for [i num-feedback]
         (eh/content 
           (feedback-sample-content i ;;loop over every feedback
                                    (nth get-unread-feedback i)))))))

(defn moderator-page-draw [ email ]
  (apply str (eh/emit* 
              (eh/at moderator-page 
                     [:.feedback]    (eh/substitute (feedback-content-transform))
                     ;[:.brief]    (eh/substitute (brief-content-transform))
                     [:.usercard] (eh/substitute (pvu/usercard-transform moderator-page email))
                      ;vine transforms
                     ))))
