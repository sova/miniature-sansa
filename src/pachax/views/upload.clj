(ns pachax.views.upload
  (:require [net.cgrand.enlive-html :as eh]
            [ring.util.anti-forgery :as ruaf]))

;; Define the template
(eh/deftemplate upload-template "upload.html"
  [uploadus]
  [:title] (eh/content (:title uploadus))
  [:div#usercard] (eh/content (:usercard uploadus))
  [:div#zoomBox] (eh/content (:zoomBox uploadus)))

;; Sample data for global-template
(def upload-sample {:title "Upload View"
                    :usercard "currentuser please log in"
                    :zoomBox "zoom in, zoom out!"})
(defn draw-upload-view [] 
  (reduce str (upload-template upload-sample)))


;;replace upload form area contents 
(def upload-page (eh/html-resource "upload.html"))

;;upload form and button  populating
(defn upload-sample-content [uploadblurbID antiforgerytoken]
;;generates textarea and submit button
;;caution: for :content always wrap the actual contents in (list) tags, since parens don't seem to work.
  (list
   {:tag :form, 
    :attrs {:class "submitBlurbContentForm",
            :action "uploadblurbGO"
            :method "POST"} 
    :content (list
              {:tag :textarea, 
               :attrs {:cols 100, 
                       :rows 40, 
                       :class "uploadblurbcontent", 
                       :name (str "uploadblurb" uploadblurbID)},  ;this field [the name attr] is referenced by the handler for post requests.
               :content nil},
              {:tag :input, 
               :attrs {:value "post blurb for great win", 
                       :class "uploadsubmitbutton", 
                       :type "submit"}, 
               :content nil} 
              {:tag :input, 
               :attrs {:type "hidden"
                       :name "__anti-forgery-token",
                       :value antiforgerytoken}, 
               :content nil})}))
     

(defn upload-content-transform [antiforgerytoken]
  ;;takes the first [only] element named .blurb, clones it, fills it with goodness
  (eh/transform upload-page [:.uploadblurb]
    (eh/clone-for [i (range 1)] ;;draw only one input blurb area
      (eh/do->
       (eh/content (upload-sample-content i antiforgerytoken))))))


;;draw to screen
(defn upload-ct-html [antiforgerytoken]
 (apply str (eh/emit* (upload-content-transform antiforgerytoken))))


;;@TODO
;;snippet for a single blurb

;;render the data into the blurb


