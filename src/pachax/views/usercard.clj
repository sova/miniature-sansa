(ns pachax.views.usercard
  (:require [net.cgrand.enlive-html :as eh]
            [clj-digest/digest]))

;; usercard transform
(defn user-email-infix [ useremail ]
  (def emailmd5hash (digest/md5 useremail))
  (list
   {:tag :div
    :attrs {:id (str "useremailcard")
            :class "useremail"},
    :content useremail},
   {:tag :img
    :attrs {:id "usergravatar",
            :class "avatar",
            :src (str "http://www.gravatar.com/avatar/" emailmd5hash "?s=90&d=identicon")}}))

(defn usercard-transform [ this-page useremail ]
  (def usercard-area (eh/select this-page [:.usercard]))
  (eh/transform usercard-area [:.usercard]
                (eh/clone-for [i (range 1)]
                              (eh/do->
                               (eh/content (user-email-infix useremail))))))
