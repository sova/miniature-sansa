(ns pachax.views.usercard
  (:require [net.cgrand.enlive-html :as eh]
            [pachax.database.dbmethods :as dbm]
            [clj-digest/digest]))

;; usercard transform
(defn user-email-infix [ useremail ]
  (let [emailmd5hash (digest/md5 useremail)
        user-participation (dbm/get-user-participation-sum useremail)
        user-is-mod (:moderator (first (dbm/check-if-moderator useremail)))]
    (list
     {:tag :div
      :attrs {:id (str "useremailcard")
              :class "useremail"},
      :content useremail},
     {:tag :div
      :attrs {:id (str "user-participation")
              :class "userparticipation"},
      :content (str user-participation)},
     {:tag :img
      :attrs {:id "usergravatar",
              :class "avatar",
              :src (str "http://www.gravatar.com/avatar/" emailmd5hash "?s=88&d=identicon")}},
     (if (= true user-is-mod)
       {:tag :div
        :attrs {:id (str "moderatorbutton")
                :class "usercardbutton"}
        :content (list
                  {:tag :a
                   :attrs { :href "/moderator"}
                   :content "moderator"})})
                         
     {:tag :div
      :attrs {:id (str "postbutton")
              :class "usercardbutton"}
      :content (list
                {:tag :a
                 :attrs { :href "/write"}
                 :content "write"})}
     {:tag :div
      :attrs {:id (str "invitebutton")
              :class "usercardbutton"}
      :content (list
                {:tag :a
                 :attrs { :href "/invite"}
                 :content "invite"})}
     ;settings button to navigate to /settings
     ;{:tag :div
     ; :attrs {:id (str "settingsbutton")
     ;         :class "usercardbutton"}
     ; :content (list
     ;           {:tag :a
     ;            :attrs { :href "/settings"}
     ;            :content "settings"})}
     {:tag :div
      :attrs {:id (str "feedbackbutton")
              :class "usercardbutton"}
      :content (list
                {:tag :a
                 :attrs { :href "/feedback"}
                 :content "feedback"})}
     {:tag :div
      :attrs {:id (str "globalbutton")
              :class "usercardbutton"}
      :content (list
                {:tag :a
                 :attrs { :href "/global"}
                 :content "global view"})}
    {:tag :div
     :attrs {:id (str "logoutbutton")
             :class "usercardbutton"
             :href "/logout"}
     :content (list 
               {:tag :a
                :attrs {:href "/logout"}
                :content "sign out"})})))
  
(defn usercard-transform [ this-page useremail ]
  (def usercard-area (eh/select this-page [:.usercard]))
  (eh/transform usercard-area [:.usercard]
                (eh/clone-for [i (range 1)]
                              (eh/do->
                               (eh/content (user-email-infix useremail))))))
