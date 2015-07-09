(ns pachax.views.moderator
  (:require [net.cgrand.enlive-html :as eh] :reload
            [pachax.views.usercard :as pvu]
            [pachax.database.dbmethods :as dbm]))

;;replace blurb contents 
(def moderator-page (eh/html-resource "moderator.html"))

;;feedback area populating
(defn feedback-entry [feedback-map anti-forgery-token]
 (let [fid (:fid feedback-map)
      sender (:email feedback-map)
      feedback-content (:content feedback-map)]
   (list 
    {:tag :div
     :attrs {:class "feedbacks"}
     :content (list ;;(str fid sender feedback-content))})))
               {:tag :div
                :attrs {:class "inner-feedback-sender"}
                :content sender},
               {:tag :div
                :attrs {:class "inner-feedback-content"}
                :content feedback-content}
               {:tag :div
                :attrs {:class "inner-feedback-fid"}
                :content (str fid)} ;;quirky ... fid by itself throws long->iSeq err
               {:tag :form
                :attrs {:class "mark-feedback-read"
                        :action "markFeedbackReadGO"
                        :method "POST"}
                :content (list
                          {:tag :input
                           :attrs {:value "mark as read"
                                   :name "fid-read-button"
                                   :class "mark-feedback-read-button"
                                   :type "submit"}
                           :content nil}
                          {:tag :input
                           :attrs {:type "hidden"
                                   :name "fid"
                                   :value (str fid)}
                           :content nil}
                          {:tag :input
                           :attrs {:type "hidden"
                                   :name "__anti-forgery-token"
                                   :value anti-forgery-token}
                           :content nil})})})))

(defn feedback-content-transform [ anti-forgery-token ]
  (let [feedback-area (eh/select moderator-page [:.feedback])
        unread-feedbacks (dbm/get-unread-feedback)
        number-of-feedbacks (count unread-feedbacks)]
    ;;takes the first [only] element named .feedback, clones it, fills it with goodness
    (eh/transform feedback-area [:.feedback]
       (eh/clone-for [i (range number-of-feedbacks)]
          (let [fid (:fid (nth unread-feedbacks i))]
            (eh/content (feedback-entry (nth unread-feedbacks i) anti-forgery-token)))))))
             ;; works : (str (nth unread-feedbacks i))))))))

(defn moderator-page-draw [ email anti-forgery-token]
  (apply str (eh/emit* 
              (eh/at moderator-page 
                     [:.feedback] (eh/substitute (feedback-content-transform anti-forgery-token))
                     [:.requests] ;;for account requests
                     [:.usercard] (eh/substitute (pvu/usercard-transform moderator-page email))))))
