(ns pachax.views.global
  (:require [net.cgrand.enlive-html :as eh]))

;; Define the template
(eh/deftemplate global-template "global.html"
  [globus]
  [:title] (eh/content (:title globus))
  [:div#usercard] (eh/content (:usercard globus))
  [:div#zoomBox] (eh/content (:zoomBox globus)))

;; Sample data for global-template
(def global-sample {:title "Global View"
                    :usercard "currentuser please log in"
                    :zoomBox "zoom in, zoom out!"})
(defn draw-global-view [] 
  (reduce str (global-template global-sample)))


;;replace blurb contents 
(def global-page (eh/html-resource "global.html"))

(def blurb-sample-content '({:tag :div, :content ["the art of paper folding is an ancient one..."]}))

(def blurb-content-transform
  (eh/transform global-page [:#blurb001] (eh/clone-for [i (range 4)] (eh/content blurb-sample-content (str i)))))

;eh/content blurb-sample-content)))

(defn blurb-ct-html []
 (apply str (eh/emit* blurb-content-transform)))



;;@TODO
;;snippet for a single blurb

;;render the data into the blurb


