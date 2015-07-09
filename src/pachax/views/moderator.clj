(ns pachax.views.moderator
  (:require [net.cgrand.enlive-html :as eh] :reload
            [pachax.views.usercard :as pvu]
            [pachax.database.dbmethods :as dbm]))

;;replace blurb contents 
(def moderator-page (eh/html-resource "moderator.html"))

;;feedback area populating
(defn feedback-entry [feedback-map]
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
                :content (str fid)})}))) ;;enlive does not like raw Longs. and therefore neds a str wrapper on this fid

(defn feedback-content-transform []
  (let [feedback-area (eh/select moderator-page [:.feedback])
        unread-feedbacks (dbm/get-unread-feedback)
        number-of-feedbacks (count unread-feedbacks)]
    ;;takes the first [only] element named .feedback, clones it, fills it with goodness
    (eh/transform feedback-area [:.feedback]
       (eh/clone-for [i (range number-of-feedbacks)]
          (let [fid (:fid (nth unread-feedbacks i))]
            (eh/content (feedback-entry (nth unread-feedbacks i))))))))
             ;; works : (str (nth unread-feedbacks i))))))))

(defn moderator-page-draw [ email ]
  (apply str (eh/emit* 
              (eh/at moderator-page 
                     [:.feedback] (eh/substitute (feedback-content-transform))
                     [:.usercard] (eh/substitute (pvu/usercard-transform moderator-page email))))))
