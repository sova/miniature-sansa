(ns pachax.views.usercard
  (:require [net.cgrand.enlive-html :as eh]
            [clj-digest/digest]))

;; usercard transform
(defn user-email-infix [ useremail user-participation]
  (def emailmd5hash (digest/md5 useremail))
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
            :src (str "http://www.gravatar.com/avatar/" emailmd5hash "?s=90&d=identicon")}},
   {:tag :div
    :attrs {:id (str "logoutbutton")
            :class "usercardbutton"
            :href "/logout"}
    :content (list 
              {:tag :a
               :attrs {:href "/logout"}
               :content "sign out"})},
   {:tag :div
    :attrs {:id (str "postbutton")
             :class "usercardbutton"}
    :content (list
              {:tag :a
               :attrs { :href "/write"}
               :content "write"})}
    {:tag :div
     :attrs {:id (str "globalbutton")
             :class "usercardbutton"}
     :content (list
               {:tag :a
                :attrs { :href "/global"}
                :content "global view"})}))

(defn usercard-transform [ this-page useremail user-participation]
  (def usercard-area (eh/select this-page [:.usercard]))
  (eh/transform usercard-area [:.usercard]
                (eh/clone-for [i (range 1)]
                              (eh/do->
                               (eh/content (user-email-infix useremail user-participation))))))
