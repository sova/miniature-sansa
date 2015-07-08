(ns pachax.views.invite
  (:require [clojure.string]
            [net.cgrand.enlive-html :as eh] :reload
            [pachax.views.usercard :as pvu]
            [pachax.database.dbmethods :as dbm]))

;;replace invite.html contents 
(def invite-page (eh/html-resource "invite.html"))
;(def global-page (eh/html-resource "global.html"))

;;this is the invite page and therefore just needs
;; usercard and the blurb-area overwritten with some simple text
;; and a couple form elements.

;; after that, the handler needs to be updated to handle requests on /invite
;; and we gotta double check that point deduction works properly.
;; 

(def invite-text 
  {:title "Invite a friend to this participatory knowledge archive.", 
   :content "via participation by submitting quality blurbs and tag verification you have accumulated ", 
   :content-tail " participation points thus far."
   :points "for 10,000 points you can invite a friend.", 
   :link "for 888 points you may post a link"})

;;invite-text populating
(defn invite-content [ email user-participation anti-forgery-token ]
  (let [invite-title (get invite-text :title)
        invite-content (get invite-text :content)
        invite-content-tail (get invite-text :content-tail)
        invite-points (get invite-text :points)
        points-link (get invite-text :link)]
    (list 
     {:tag :div,
      :attrs {:class "invite-description"},
      :content (list 
                {:tag :div,
                 :attrs {:id (str "invite-title")}
                 :content invite-title},
                 {:tag :div,
                  :attrs {:id (str "invite-content")}
                  :content (str invite-content user-participation invite-content-tail)},
                 {:tag :div,
                  :attrs {:id (str "invite-points")}
                  :content invite-points},
                {:tag :form,
                 :attrs {:class "submitInviteForm",
                         :action "sendInviteGO",
                         :method "POST"}
                 :content (list
                           {:tag :input
                            :attrs {:name "invite-recipient"
                                    :class "postcontentinvite"
                                    :type "text"
                                    :placeholder "enter your friend's email"}
                            :content nil},
                           {:tag :input, 
                            :attrs {:name "invite-submit",
                                    :class "post-invite-button", 
                                    :value "invite a friend"
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

(defn invite-content-transform [ email anti-forgery-token ]
  (let [invite-area (eh/select invite-page [:.invite])
        user-participation (dbm/get-user-participation-sum email)]
    ;;takes the first [only] element named .invite, clones it, fills it with goodness
    (eh/transform invite-area [:.invite]
                  (eh/content 
                   (invite-content   email user-participation anti-forgery-token)))))


(defn invite-page-draw [ email anti-forgery-token ]
  (apply str (eh/emit* 
              (eh/at invite-page
                     [:.usercard] (eh/substitute (pvu/usercard-transform invite-page email))
                     [:.invite] (eh/substitute (invite-content-transform email anti-forgery-token))))))
