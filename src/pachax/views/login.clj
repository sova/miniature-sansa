(ns pachax.views.login
  (:require [net.cgrand.enlive-html :as eh]
            [ring.util.anti-forgery :as ruaf]
            [crypto.password.scrypt :as pw]))

;; Define the template
(eh/deftemplate login-template "login.html"
  [loginus]
  [:title] (eh/content (:title loginus))
  [:div#usercard] (eh/content (:usercard loginus))
  [:div#zoomBox] (eh/content (:zoomBox loginus)))

;; Sample data for the login-template
(def login-sample {:title "Login View"
                    :usercard "currentuser please log in"
                    :zoomBox "zoom in, zoom out!"})
(defn draw-login-view [] 
  (reduce str (login-template login-sample)))


;;replace login form area contents 
(def login-page (eh/html-resource "login.html"))

;;login form and button  populating
(defn login-sample-content 
  ([antiforgerytoken]
   ;;generates textarea and submit button
   ;;caution: for :content always wrap the actual contents in (list) tags, since parens don't seem to work.
   (list
    {:tag :form, 
     :attrs {:id "signinForm",
             :class "goodhello",
             :action "loginGO",
             :method "POST"} 
     :content (list
               {:tag :input
                :attrs {:id "email"
                        :name "username-input"
                        :class "formwrap"
                        :type "text"
                        :size "44"
                        :placeholder "your email, please"
                        :autofocus "true"}
                :content nil},
               {:tag :input, 
                :attrs {:value "sign in", 
                        :class "formwrap loginsubmitbutton", 
                        :type "submit"}, 
                :content nil} 
               {:tag :input, 
                :attrs {:type "hidden"
                        :name "__anti-forgery-token",
                        :value antiforgerytoken}, 
                :content nil})}))
  ([redirect antiforgerytoken]
   (list
    {:tag :form, 
     :attrs {:id "signinForm",
             :class "goodhello",
             :action "loginGO",
             :method "POST"} 
     :content (list
               {:tag :input
                :attrs {:id "email"
                        :name "username-input"
                        :class "formwrap"
                        :type "text"
                        :size "44"
                        :placeholder "your email, please"
                        :autofocus "true"}
                :content nil},
               {:tag :input, 
                :attrs {:value "sign in", 
                        :class "formwrap loginsubmitbutton", 
                        :type "submit"}, 
                :content nil}
               {:tag :input,
                :attrs {:type "hidden",
                        :name "redirect",
                        :value redirect},
                :content nil}
               {:tag :input, 
                :attrs {:type "hidden",
                        :name "__anti-forgery-token",
                        :value antiforgerytoken}, 
                :content nil})})))
     
     

(defn login-content-transform 
  ([antiforgerytoken]
  ;;takes the first [only] element named .blurb, clones it, fills it with goodness
   (eh/transform login-page [:#signinForm]
                 (eh/do->
                  (eh/content (login-sample-content antiforgerytoken)))))
  ([redirect antiforgerytoken]
   (eh/transform login-page [:#signinForm]
                 (eh/do->
                  (eh/content (login-sample-content redirect antiforgerytoken))))))


;;draw to screen
(defn login-ct-html 
  ([antiforgerytoken] 
   (apply str (eh/emit* (login-content-transform antiforgerytoken))))
  ([redirect antiforgerytoken]
   (apply str (eh/emit* (login-content-transform redirect antiforgerytoken)))))

;;@TODO
;;snippet for a single blurb

;;render the data into the blurb


