(ns pachax.views.blurb
  (:require [net.cgrand.enlive-html :as eh] :reload
            [pachax.views.usercard :as pvu]
            [pachax.database.dbmethods :as dbm]))

;;replace blurb contents 
(def blurb-page (eh/html-resource "blurb.html"))


;;;edits in progress up to this point :D

;;; making this the "single simple blurb display page"

(def numberOfBlurbsToShow 1)

(defn randRating []
  (def prestringRating (rand-int 99))
  (if (< prestringRating 10)
    (str "0" prestringRating)
    (str prestringRating)))
  

(defn return-a-blurb [ eid ]
  (dbm/get-blurb-by-eid eid))

;;blurb populating
(defn mono-blurb-content [ blurbmap]
  (let [blurbtitle (get blurbmap :title)
        blurbcontent (get blurbmap :content)
        blurbtags (get blurbmap :tags)]
    (list 
     {:tag :div,
      :attrs {:class "monoblurbratingwrap"},
      :content (list {:tag :div,
                      :attrs {:id (str "monodoubleplusblurb"),
                              :class "monodoubleplus"}},
                     {:tag :div,
                      :attrs {:id (str "monosingleplusblurb"),
                              :class "monosingleplus"}},
                     {:tag :div,
                      :attrs {:id (str "mononeedsworkblurb"),
                              :class "mononeedswork"}},
                     {:tag :div,
                      :attrs {:id (str "monoblurbrating"),
                              :class "monoblurbrating"}
                      :content (randRating)})},
     {:tag :div, 
      :attrs {:id (str "monoblurb"), 
              :class ""}, ;maybe throw in a coloration class for the bg
      :content (list {:tag :div,
                      :attrs {:id (str "monoblurbtitle"),
                              :class (str "monoinnerblurbtitle")}
                      :content blurbtitle},
                     {:tag :div,
                      :attrs {:id (str "monoblurbcontent"),
                              :class (str "monoinnerblurbcontent")}
                      :content blurbcontent},
                     {:tag :div,
                      :attrs {:id (str "monoblurbtags"),
                              :class (str "monoinnerblurbtags")}
                      :content blurbtags})})))

;(defn return-a-blurb []
;  (rand-nth (blurbs-from-db)))

(defn blurb-content-transform [eid]
  (def blurb-area (eh/select blurb-page [:.monoblurb]))
  ;;takes the first [only] element named .blurb, clones it, fills it with goodness
  (eh/transform blurb-area [:.monoblurb]
                (eh/content (mono-blurb-content (return-a-blurb eid)))))

(defn blurb-page-draw [ email eid ]
  (apply str (eh/emit* 
               (eh/at blurb-page 
                      [:.monoblurb]    (eh/substitute (blurb-content-transform eid))
;                      [:.brief]    (eh/substitute (brief-content-transform))
                      [:.usercard] (eh/substitute (pvu/usercard-transform blurb-page email))
                      ;vine transforms
                      ))))
