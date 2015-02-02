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




