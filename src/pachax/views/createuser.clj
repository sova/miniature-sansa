(ns pachax.views.createuser
  (:require [net.cgrand.enlive-html :as eh]
            [ring.util.anti-forgery :as ruaf]))

;; Define the template
(eh/deftemplate createuser-template "createuser.html"
  [usercreationus]
  [:title] (eh/content (:title usercreationus))
  [:div#usercard] (eh/content (:usercard usercreationus))
  [:div#zoomBox] (eh/content (:zoomBox usercreationus)))

;; Sample data for usercreation-template
(def createuser-sample {:title "Creat User View"
                    :usercard "currentuser please log in"
                    :zoomBox "zoom in, zoom out!"})
(defn draw-createuser-view [] 
  (reduce str (createuser-template createuser-sample)))


;;replace usercreation div area contents 
(def createuser-page (eh/html-resource "createuser.html"))

;;upload form and button  populating
(defn createuser-sample-content [ antiforgerytoken ]
;;generates inputs and submit button
;;caution: for :content always wrap the actual contents in (list) tags, since parens don't seem to work.
  (list
   {:tag :form, 
    :attrs {:class "submitUserCreationForm",
            :action "createuserGO"
            :method "POST"} 
    :content (list
              {:tag :input
               :attrs {:name "useremail"
                       :type "text"
                       :size "63"
                       :placeholder "your email :D.  must be deliverable and accesible to you."}
               :content nil},
              {:tag :input
               :attrs {:name "password1"
                       :type "text"
                       :size "55"
                       :placeholder "your password"}
               :content nil},
              {:tag :input
               :attrs {:name "password2"
                       :type "text"
                       :size "55"
                       :placeholder "verify your password"}
               :content nil},
              {:tag :input, 
               :attrs {:value "create a new user!", 
                       :class "createuserbutton", 
                       :type "submit"}, 
               :content nil} 
              {:tag :input, 
               :attrs {:type "hidden"
                       :name "__anti-forgery-token",
                       :value antiforgerytoken}, 
               :content nil})}))
     

(defn createuser-content-transform [antiforgerytoken]
  ;;takes the first [only] element named .usercreation, clones it, fills it with goodness
  (eh/transform createuser-page [:.usercreation]
    ;(eh/clone-for [i (range 1)] ;;draw only one input blurb area
      (eh/do->
       (eh/content (createuser-sample-content antiforgerytoken)))))



;;draw to screen
(defn createuser-ct-html [antiforgerytoken]
 (apply str (eh/emit* (createuser-content-transform antiforgerytoken))))


;;@TODO
;;snippet for a single blurb

;;render the data into the blurb


