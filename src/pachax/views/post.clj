(ns pachax.views.post
  (:require [net.cgrand.enlive-html :as eh]
            [ring.util.anti-forgery :as ruaf]
            [datomic.api :as d]))

;; Define the template
(eh/deftemplate post-template "upload.html"
  [postus]
  [:title] (eh/content (:title postus))
  [:div#usercard] (eh/content (:usercard postus))
  [:div#zoomBox] (eh/content (:zoomBox postus)))


;;replace form area contents 
(def post-page (eh/html-resource "upload.html"))

;;login form and button  populating
(defn login-sample-content [antiforgerytoken]
;;generates textarea and submit button
;;caution: for :content always wrap the actual contents in (list) tags, since parens don't seem to work.
  (list
   {:tag :form, 
    :attrs {:class "submitLoginForm",
            :action "loginGO"
            :method "POST"} 
    :content (list
              {:tag :input
               :attrs {:name "username-input"
                       :class "usernamefield"
                       :type "text"
                       :size "44"
                       :placeholder "your email, please"
                       :autofocus "true"}
               :content nil},
              {:tag :input, 
               :attrs {:value "login go go go", 
                       :class "loginsubmitbutton", 
                       :type "submit"}, 
               :content nil} 
              {:tag :input, 
               :attrs {:type "hidden"
                       :name "__anti-forgery-token",
                       :value antiforgerytoken}, 
               :content nil})}))
     

(defn login-content-transform [antiforgerytoken]
  ;;takes the first [only] element named .blurb, clones it, fills it with goodness
  (eh/transform login-page [:.login-field]
    ;(eh/clone-for [i (range 1)] ;;draw only one input blurb area
      (eh/do->
       (eh/content (login-sample-content antiforgerytoken)))))


;;draw to screen
(defn login-ct-html [antiforgerytoken]
 (apply str (eh/emit* (login-content-transform antiforgerytoken))))


;;@TODO
;;snippet for a single blurb

;;render the data into the blurb


;;;POST.clj -- work in progress
