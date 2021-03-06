(ns pachax.views.moderator
  (:require [net.cgrand.enlive-html :as eh] :reload
            [pachax.views.usercard :as pvu]
            [pachax.views.ticker :as pvt]
            [pachax.database.dbmethods :as dbm]))

;;replace blurb contents 
(def moderator-page (eh/html-resource "moderator.html"))


;;controls for adding users, gifting participation, and removing stuff
(defn feedback-controls [anti-forgery-token]
  (list 
   ;;add a user to PH field
    {:tag :form,
     :attrs {:class "submit-new-user-email",
             :action "addNewUserGO",
             :method "POST"}
     :content (list
               {:tag :input,
                :attrs {:name "user-to-add",
                        :class "new-user-button",
                        :placeholder "enter a user-email"},
                :content nil},   
               {:tag :input, 
                :attrs {:value "add user",
                        :name "new-user",
                        :class "new-user-button", 
                        :type "submit"},
                :content nil},
               {:tag :input, 
                :attrs {:type "hidden"
                        :name "__anti-forgery-token",
                        :value anti-forgery-token},
                :content nil})},
    ;;gift some participation points [user-email, giver]
    {:tag :form,
     :attrs {:class "submit-participation-boost",
             :action "giftParticipationGO",
             :method "POST"}
     :content (list
               {:tag :input, 
                :attrs {:value "gift",
                        :name "user-email-button",
                        :class "participation-button", 
                        :type "submit"},
                :content nil},
               {:tag :input,
                :attrs {:name "user-email",
                        :class "participation-button",
                        :placeholder "user to give participation to"},
                :content nil},   
               {:tag :input, 
                :attrs {:type "hidden"
                        :name "__anti-forgery-token",
                        :value anti-forgery-token},
                :content nil})},
    ;;remove a blurb
    {:tag :form,
     :attrs {:class "remove-blurb",
             :action "removeBlurbGO",
             :method "POST"}
     :content (list
               {:tag :input,
                :attrs {:name "blurb-to-remove",
                        :class "tag-removal",
                        :placeholder "enter a blurb to remove"},
                :content nil},                
               {:tag :input, 
                :attrs {:value "remove blurb",
                        :name "blurb-removal",
                        :class "blurb-remove-button", 
                        :type "submit"},
                :content nil},
               {:tag :input, 
                :attrs {:type "hidden"
                        :name "__anti-forgery-token",
                        :value anti-forgery-token},
                :content nil})},

    ;; remove a tag [tag bid]
    {:tag :form,
     :attrs {:class "remove-tag",
             :action "removeTagGO",
             :method "POST"}
     :content (list
               {:tag :input,
                :attrs {:name "tag-to-remove",
                        :class "tag-removal",
                        :placeholder "enter a tag to remove"},
                :content nil},   
               {:tag :input,
                :attrs {:name "tag-in-bid-to-remove",
                        :class "tag-removal",
                        :placeholder "enter a bid"},
                :content nil},   
               {:tag :input, 
                :attrs {:value "remove tag",
                        :name "tag-removal",
                        :class "remove-tag", 
                        :type "submit"},
                :content nil},
               {:tag :input, 
                :attrs {:type "hidden"
                        :name "__anti-forgery-token",
                        :value anti-forgery-token},
                :content nil})}))

;;feedback area populating
(defn feedback-entry [feedback-map anti-forgery-token]
 (let [fid (:fid feedback-map)
      sender (:email feedback-map)
      feedback-content (:content feedback-map)]
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
                           :attrs {:value "mark feedback read"
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
                           :content nil})})}))

;(defn feedback-controls-transform [anti-forgery-token]
;  (let [feedback-controls-area (eh/select moderator-page [:.feedback-controls])]
;    (eh/transform feedback-controls-area [:.feedback-controls]
;      (eh/clone-for [i 1]
;        (eh/content (feedback-controls anti-forgery-token))))))

(defn feedback-content-transform [ anti-forgery-token ]
  (let [feedback-area (eh/select moderator-page [:.feedback])
        unread-feedbacks (dbm/get-unread-feedback)
        number-of-feedbacks (count unread-feedbacks)]
    ;;takes the first [only] element named .feedback, clones it, fills it with goodness
    (eh/transform feedback-area [:.feedback]
       (eh/clone-for [i (range number-of-feedbacks)]
          (let [fid (:fid (nth unread-feedbacks i))]
            (eh/content (feedback-entry (nth unread-feedbacks i) anti-forgery-token)))))))


;;request area populating
(defn account-request-entry [arequest-map anti-forgery-token]
 (let [arid (:arid arequest-map)
       requester (:email arequest-map)
       essay-content (:essay arequest-map)]
   (list 
    {:tag :div
     :attrs {:class "account-requests"}
     :content (list
               {:tag :div
                :attrs {:class "inner-account-request-er"}
                :content requester},
               {:tag :div
                :attrs {:class "inner-account-request-essay"}
                :content essay-content}
               {:tag :div
                :attrs {:class "inner-account-request-arid"}
                :content (str arid)} ;;quirky ... arid by itself throws long->iSeq err
               {:tag :form
                :attrs {:class "mark-account-request-read"
                        :action "markAccountRequestReadGO"
                        :method "POST"}
                :content (list
                          {:tag :input
                           :attrs {:value "mark account request read"
                                   :name "arid-read-button"
                                   :class "mark-account-request-read-button"
                                   :type "submit"}
                           :content nil}
                          {:tag :input
                           :attrs {:type "hidden"
                                   :name "arid"
                                   :value (str arid)}
                           :content nil}
                          {:tag :input
                           :attrs {:type "hidden"
                                   :name "__anti-forgery-token"
                                   :value anti-forgery-token}
                           :content nil})})})))

(defn account-request-content-transform [ anti-forgery-token ]
  (let [ar-area (eh/select moderator-page [:.account-request])
        unread-ar (dbm/get-unread-account-requests)
        number-of-ar (count unread-ar)]
    ;;takes the first [only] element named .account-requests, clones it, fills it with goodness
    (eh/transform ar-area [:.account-request]
       (eh/clone-for [i (range number-of-ar)]
         (eh/content (account-request-entry (nth unread-ar i) anti-forgery-token))))))

(defn moderator-page-draw [ email anti-forgery-token]
  (apply str (eh/emit* 
              (eh/at moderator-page 
                     [:#ticker]                (eh/substitute (pvt/ticker-transform moderator-page))
                     [:.feedback-controls]     (eh/substitute (feedback-controls anti-forgery-token))
                     [:.feedback]              (eh/substitute (feedback-content-transform anti-forgery-token))
                     [:.usercard]              (eh/substitute (pvu/usercard-transform moderator-page email))
                     [:.account-request]       (eh/substitute (account-request-content-transform anti-forgery-token))))))
