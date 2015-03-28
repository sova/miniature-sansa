(ns pachax.views.global
  (:require [net.cgrand.enlive-html :as eh] :reload
            [pachax.views.usercard :as pvu]
            [pachax.database.dbmethods :as dbm]))

;;replace blurb contents 
(def global-page (eh/html-resource "global.html"))

(def various-wisdoms 
  ["the heart would have no rainbow if the eye had no tears",
   "there is no death, only a change of worlds.", 
   "do not judge your neighbor until you walk two moons in his moccasins", 
   "the greatest strength is gentleness", "the art of paper folding is an ancient one...", 
   "The invariable mark of wisdom is to see the miraculous in the common. ~rwe",
   "enravel is a community effort, for futhering human love, compassion, and understanding."])


;;talk with the database and get posts by their [count]
(defn blurbs-from-db []
  (dbm/get-all-blurbs))

(defn num-blurbs-total []
  (count (blurbs-from-db)))


(def numberOfBlurbsToShow (num-blurbs-total))

(defn randRating []
  (def prestringRating (rand-int 99))
  (if (< prestringRating 10)
    (str "0" prestringRating)
    (str prestringRating)))
 
(defn get-blurb-rating [bid]
  (let [score (dbm/get-score-for-bid bid)]
    (if (< score 10)
      (str "0" score)
      (str score))))

;brief populating
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
        blurbcontent (:content blurbmap)
        blurbtags (:tags blurbmap)
        blurbeid (:bid blurbmap)]
    (list
     {:tag :div, 
      :attrs {:id (str "blurb" blurbID)
              :class "blurbin"}
             ; :class (if (= 0 (mod blurbID (rand 5))) ;every nth blurb is a .blurbTop
             ;          (str "brightBlurb")
             ;          (str "lightBlurb"))}
      :content (list 
                {:tag :a,
                 :attrs {:class "blurblink"
                         :href (str "/blurb" blurbeid)},
                 },
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
                                 :content  (get-blurb-rating blurbeid)})},
                {:tag :div,
                 :attrs {:id (str "blurbtitle" blurbID),
                         :class (str "innerblurbtitle")}
                 :content blurbtitle},
                {:tag :span,
                 :attrs {:class "after-title-space"},
                 :content "  "},
                {:tag :div,
                 :attrs {:id (str "blurbcontent" blurbID),
                         :class (str "innerblurbcontent")}
                 :content blurbcontent})},
     {:tag :div,
      :attrs {:id (str "blurbtags" blurbID),
              :class (str "innerblurbtags")}
      :content blurbtags})))

(defn return-a-blurb [ idx ]
  (nth (blurbs-from-db) idx ))

;;blurb return works on two axes: time (last X days) and probability matrix:
;; such fancy words to say "within a given time, gives back next blurb with prob:"
;; (.3 unseen highrated
;; (.3 seen highrated
;; (.2 unseen middling
;; (.1 seen middle
;; (.1 unrated)))))  which still plays nicely and flexibly with "randomness"

(defn blurb-content-transform []
  (def blurb-area (eh/select global-page [:.blurb]))
  ;;takes the first [only] element named .blurb, clones it, fills it with goodness
  (eh/transform blurb-area [:.blurb]
    (eh/clone-for [i (range numberOfBlurbsToShow)] 
                  (eh/content (blurb-sample-content i (return-a-blurb i))))))

(defn global-page-draw [ email ]
  (apply str (eh/emit* 
              (eh/at global-page 
                     [:.blurb]    (eh/substitute (blurb-content-transform))
                     [:.brief]    (eh/substitute (brief-content-transform))
                     [:.usercard] (eh/substitute (pvu/usercard-transform global-page email))
                      ;vine transforms
                     ))))
