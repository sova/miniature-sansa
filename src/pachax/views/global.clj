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

(def various-wisdoms ["the soul would have no rainbow if the eye had no tears", "there is no death, only a change of worlds.", "do not judge your neighbor until you walk two moons in his moccasins", "the greatest strength is gentleness", "the art of paper folding is an ancient one..."])

(defn blurb-sample-content [blurbID] 
  (list 
    {:tag :div, :attrs {:id (str "blurb" blurbID)}, 
     :content (rand-nth various-wisdoms)}))

(def blurb-content-transform
  ;;takes the first [only] element named .blurb, clones it, fills it with goodness
  (eh/transform global-page [:.blurb]
    (eh/clone-for [i (range 9)] 
      (eh/do->
       (eh/content (blurb-sample-content i))))))

;eh/content blurb-sample-content)))

(defn blurb-ct-html []
 (apply str (eh/emit* blurb-content-transform)))



;;@TODO
;;snippet for a single blurb

;;render the data into the blurb


