(ns pachax.views.splash
  (:require [net.cgrand.enlive-html :as eh]))

;;replace coolquote
(def splash-page (eh/html-resource "splash.html"))

(def various-wisdoms 
  ["the heart would have no rainbow if the eye had no tears",
   "there is no death, only a change of worlds.", 
   "do not judge your neighbor until you walk two moons in his moccasins", 
   "the greatest strength is gentleness", 
   "The invariable mark of wisdom is to see the miraculous in the common. ~rwe"])

;;coolquote at bottom filling  (eventually by top rated blurbs!)  
(defn coolquote-content []
  (list 
   {:tag :div,
    :attrs {:class "splash"},
    :content (list
              {:tag :a
               :attrs {:class "splash",
                       :href "/login"},
               :content (str "enter"
                         )})}))

(defn coolquote-transform [] 
  (def coolquote-area (eh/select splash-page [:#coolquote]))
  (eh/transform coolquote-area [:#coolquote]
                (eh/content (coolquote-content))))

;;splashtext next to image
(defn splashtext-content []
    (list 
     {:tag :div,
      :attrs {:class "splash"},
      :content (list
                {:tag :h1,
                 :content (str "enravel ")},
                {:tag :span,
                 :content (list "positive grow community where the sharing of ideas, interesting knowledge, and truly notable life.habit.pattern insights is actively encouraged. ")})}))

(defn splashtext-transform []
  (def splashtext-area (eh/select splash-page [:#splashtext]))
  (eh/transform splashtext-area [:#splashtext]
                (eh/content (splashtext-content))))

(defn splash-page-draw []
  (apply str (eh/emit* 
               (eh/at splash-page 
                      [:#splashtext]    (eh/substitute (splashtext-transform))
                      [:#coolquote]    (eh/substitute (coolquote-transform))
                      ;vine transforms
                      ))))
