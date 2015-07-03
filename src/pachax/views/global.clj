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

(def number-of-blurbs-to-show 9)

;(defn blurbs-from-db []
;  (dbm/get-nine-blurbs))

;<justin_smith> vas: would it suffice to query when you first look for results, and reuse the result next time?
;<justin_smith> vas: if so (def query-results (delay (query ...))
;<justin_smith> vas: inside your page rendering, use let over a delay
;<justin_smith> vas: thus, when the page is rendered again, you get a new result

;(def get-the-nine 
;  (delay 
;   (do 
;     (blurbs-from-db))))  ;runs body once, invoke result set with deref: @get-the-nine

;(defn return-a-blurb [ idx ]
;  (let [get-the-nine (delay (do (blurbs-from-db)))]
;  (dbm/get-blurb-by-bid (nth @get-the-nine idx ))))

(defn randRating []
  (def prestringRating (rand-int 99))
  (if (< prestringRating 10)
    (str "0" prestringRating)
    (str prestringRating)))
 
(defn isDice? [bid]
  (let [ratings-count (dbm/get-ratings-count-for-bid bid)]
    (if (> 7 ratings-count)
      (str "isDice")
      (str "blurbrating"))))

(defn get-blurb-rating [bid]
  (let [score (dbm/get-score-for-bid bid)
        ratings-count (dbm/get-ratings-count-for-bid bid)]
    (if (< 6 ratings-count)
      (if (< score 10)
        (str "0" score)
        (str score))
      (str ratings-count))))  ;;return the number of ratings so far if there are not 7 yet
                              ;; displayed as drakken dice ttf :]

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
(defn blurb-sample-content [blurbID b-map-in]
  (let [blurbmap (first b-map-in)
        blurbtitle (:title blurbmap)
        blurbcontent (:content blurbmap)
        blurbtags (:tags blurbmap)
        blurbeid (:bid  blurbmap)
        blurbrating (get-blurb-rating blurbeid)]
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
                                         :class (str (isDice? blurbeid))}
                                 :content blurbrating})},
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

;;blurb return works on two axes: time (last X days) and probability matrix:
;; such fancy words to say "within a given time, gives back next blurb with prob:"
;; (.3 unseen highrated
;; (.3 seen highrated
;; (.2 unseen middling
;; (.1 seen middle
;; (.1 unrated)))))  which still plays nicely and flexibly with "randomness"

(defn blurb-content-transform []
  ;(def blurb-area (eh/select global-page [:.blurb]))
  (let [blurb-area (eh/select global-page [:.blurb])
        get-the-nine (delay (dbm/get-nine-blurbs))]
    ;;takes the first [only] element named .blurb, clones it, fills it with goodness
    (eh/transform blurb-area [:.blurb]
       (eh/clone-for [i (range number-of-blurbs-to-show)]
         (eh/content 
           (blurb-sample-content i (dbm/get-blurb-by-bid 
                                      (nth @get-the-nine i))))))))

(defn global-page-draw [ email ]
  (apply str (eh/emit* 
              (eh/at global-page 
                     [:.blurb]    (eh/substitute (blurb-content-transform))
                     ;[:.brief]    (eh/substitute (brief-content-transform))
                     [:.usercard] (eh/substitute (pvu/usercard-transform global-page email))
                      ;vine transforms
                     ))))
