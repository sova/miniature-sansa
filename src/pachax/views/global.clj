(ns pachax.views.global
  (:refer-clojure :exclude [sort find])
  (:require [net.cgrand.enlive-html :as eh]
            [monger.core :as mg]
            [monger.collection :as mc]
            [monger.query :refer :all]))

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

(def various-wisdoms 
  ["the heart would have no rainbow if the eye had no tears the heart would have no rainbow if the eye had no tears the heart would have no rainbow if the eye had no tears",
   "there is no death, only a change of worlds.", 
   "do not judge your neighbor until you walk two moons in his moccasins", 
   "the greatest strength is gentleness", "the art of paper folding is an ancient one...", 
   "The invariable mark of wisdom is to see the miraculous in the common. ~rwe",
   "practical human is a community effort, aimed at the futhering of human love, compassion, understanding, mutual growth.  you are currently at LOVE, where general life tips, collections of beautiful moments, and wise advice live."])

(def numberOfBlurbsToShow 16)

;;talk with the database and get posts by their [count]
(defn blurbs-from-db []
  (let [conn (mg/connect {:host "127.0.0.1" :port 27272})
        db (mg/get-db conn "posts")
        coll "blurbs"]
    (with-collection db coll
      (find {})
      (fields [:blurb_content :id])
      ;; it is VERY IMPORTANT to use array maps with sort
      (sort (array-map :blurb_content 1))
      (limit numberOfBlurbsToShow))))


;;brief populating
(defn brief-sample-content [briefID]
  (list
   {:tag :div, :attrs {:id (str "brief" briefID), :class "briefcontent"}
    :content (rand-nth various-wisdoms)}))

(def brief-content-transform 
  (eh/transform global-page [:.brief]
                (eh/clone-for [i (range 5)]
                              (eh/do->
                                (eh/content (brief-sample-content i))))))
(defn brief-ct-html []
  (apply str (eh/emit* brief-content-transform)))


;;blurb populating
(defn blurb-sample-content [blurbID]
;;generates blurbs with IDs from blurb-content-transform, to a random width and height 
  (list 
    {:tag :div, :attrs {:id (str "blurb" blurbID), :class 
      (if (= 0 (mod blurbID 5)) ;every nth blurb is a .blurbTop
        (str "topBlurb")
        (str "blurbcontent"))}
;:style (str "height: "  (+ 70 (rand-int 60)) "; width: " (+ 140 (rand-int 100)))}, 
     :content ((nth (blurbs-from-db) blurbID) :blurb_content)}))

(def blurb-content-transform 
  ;;takes the first [only] element named .blurb, clones it, fills it with goodness
  (eh/transform global-page [:.blurb]
    (eh/clone-for [i (range numberOfBlurbsToShow)] 
      (eh/do->
        (eh/content (blurb-sample-content i))))))


;;draw to screen
(defn blurb-ct-html []
 (apply str (eh/emit* blurb-content-transform)))


;;@TODO
;;snippet for a single blurb

;;render the data into the blurb


