(ns pachax.views.upload
  (:require [net.cgrand.enlive-html :as eh]
            [ring.util.anti-forgery :as raf]))

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
(defn upload-sample-content [uploadblurbID]
;;generates textarea and submit button
  (list 
   {:tag :form,
    :attrs {:action "/uploadblurbGO"}}
   {:tag :textarea, :attrs {:name (str "uploadblurb" uploadblurbID), :class "uploadblurbcontent", :rows "40", :cols "100"}}
   {:tag :input, :attrs {:id "__anti-forgery-token", :name "__anti-forgery-token", :type "hidden", :value `raf.*anti-forgery-token*}}
   {:tag :input, :attrs {:type "submit", :class "uploadsubmitbutton", :value "post blurb for great win"}, 
    :content nil}
))

(def upload-content-transform 
  ;;takes the first [only] element named .blurb, clones it, fills it with goodness
  (eh/transform upload-page [:.uploadblurb]
    (eh/clone-for [i (range 1)] ;;draw only one input blurb area
      (eh/do->
       (eh/content (upload-sample-content i))))))


;;draw to screen
(defn upload-ct-html []
 (apply str (eh/emit* upload-content-transform)))


;;@TODO
;;snippet for a single blurb

;;render the data into the blurb


