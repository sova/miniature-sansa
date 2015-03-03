(ns pachax.views.global
  (:require [net.cgrand.enlive-html :as eh] :reload
            [clj-digest/digest]
            [pachax.database.dbmethods :as dbm]))

;;replace blurb contents 
(def global-page (eh/html-resource "global.html"))

(def various-wisdoms 
  ["the heart would have no rainbow if the eye had no tears the heart would have no rainbow if the eye had no tears the heart would have no rainbow if the eye had no tears",
   "there is no death, only a change of worlds.", 
   "do not judge your neighbor until you walk two moons in his moccasins", 
   "the greatest strength is gentleness", "the art of paper folding is an ancient one...", 
   "The invariable mark of wisdom is to see the miraculous in the common. ~rwe",
   "practical human is a community effort, aimed at the futhering of human love, compassion, understanding, mutual growth.  you are currently at LOVE, where general life tips, collections of beautiful moments, and wise advice live."])

(def numberOfBlurbsToShow 12)

(defn randRating []
  (def prestringRating (rand-int 99))
  (if (< prestringRating 10)
    (str "0" prestringRating)
    (str prestringRating)))
  

;;talk with the database and get posts by their [count]
(defn blurbs-from-db []
  (dbm/get-all-blurbs))

;; usercard transform
(defn user-email-infix [ useremail ]
  (def emailmd5hash (digest/md5 useremail))
  (list
   {:tag :div
    :attrs {:id (str "useremailcard")
            :class "useremail"},
    :content useremail},
   {:tag :img
    :attrs {:id "usergravatar",
            :class "avatar",
            :src (str "http://www.gravatar.com/avatar/" emailmd5hash "?s=90&d=identicon")}}))

(defn usercard-transform [ useremail ]
  (def usercard-area (eh/select global-page [:.usercard]))
  (eh/transform usercard-area [:.usercard]
    (eh/clone-for [i (range 1)]
      (eh/do->
        (eh/content (user-email-infix useremail))))))

;;brief populating
(defn brief-sample-content [briefID]
  (list
   {:tag :div,
    :attrs {:id (str "briefratingwrap" briefID),
            :class "briefratingwrap"},
    :content (list {:tag :div,
                    :attrs {:id (str "doubleplusbrief" briefID),
                            :class "doubleplusbrief"}},
                   {:tag :div,
                    :attrs {:id (str "singleplusbrief" briefID),
                            :class "singleplusbrief"}},
                   {:tag :div,
                    :attrs {:id (str "needsworkbrief" briefID),
                            :class "needsworkbrief"}},
                   {:tag :div,
                    :attrs {:id (str "briefrating" briefID),
                            :class "briefrating"}
                    :content (randRating)})},
   {:tag :div, 
    :attrs {:id (str "brief" briefID), 
            :class "briefcontent"},
    :content (rand-nth various-wisdoms)}))

(defn brief-content-transform []
  (def brief-area (eh/select global-page [:.brief]))
  (eh/transform brief-area [:.brief]
    (eh/clone-for [i (range 5)]
      (eh/do->
        (eh/content (brief-sample-content i))))))

;;blurb populating
(defn blurb-sample-content [blurbID blurbmap]
  (let [blurbtitle (:title blurbmap)
        blurbcontent (:content blurbmap)]
    (list 
     {:tag :div,
      :attrs {:class "blurbratingwrap"},
      :content (list {:tag :div,
                      :attrs {:id (str "doubleplusblurb" blurbID),
                              :class "doubleplus"}},
                     {:tag :div,
                      :attrs {:id (str "singleplusblurb" blurbID),
                              :class "singleplus"}},
                     {:tag :div,
                      :attrs {:id (str "needsworkblurb" blurbID),
                              :class "needswork"}},
                     {:tag :div,
                      :attrs {:id (str "blurbrating" blurbID),
                              :class "blurbrating"}
                      :content  (randRating)})},
     {:tag :div, 
      :attrs {:id (str "blurb" blurbID), 
              :class (if (= 0 (mod blurbID 3)) ;every nth blurb is a .blurbTop
                       (str "topBlurb")
                       (str "blurbcontent"))}
      :content (list {:tag :div,
                      :attrs {:id (str "blurbtitle" blurbID),
                              :class (str "innerblurbtitle")}
                      :content blurbtitle},
                     {:tag :div,
                      :attrs {:id (str "blurbcontent" blurbID),
                              :class (str "innerblurbcontent")}
                      :content blurbcontent})})))

(defn return-a-blurb []
  (rand-nth (blurbs-from-db)))

(defn blurb-content-transform []
  (def blurb-area (eh/select global-page [:.blurb]))
  ;;takes the first [only] element named .blurb, clones it, fills it with goodness
  (eh/transform blurb-area [:.blurb]
    (eh/clone-for [i (range numberOfBlurbsToShow)] 
                  (eh/content (blurb-sample-content i (return-a-blurb))))))

(defn global-page-draw [ email ]
  (apply str (eh/emit* 
               (eh/at global-page 
                      [:.blurb]    (eh/substitute (blurb-content-transform))
                      [:.brief]    (eh/substitute (brief-content-transform))
                      [:.usercard] (eh/substitute (usercard-transform email))
                      ;vine transforms
                      ))))
