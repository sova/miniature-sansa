(ns pachax.views.moderator
  (:require [net.cgrand.enlive-html :as eh] :reload
            [pachax.views.usercard :as pvu]
            [pachax.database.dbmethods :as dbm]))

;;replace blurb contents 
(def moderator-page (eh/html-resource "moderator.html"))

(def number-of-feedbacks-to-show 100)

;;feedback area populating
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

(defn feedback-content-transform []
  (let [feedback-area (eh/select global-page [:.blurb])
        get-unread-feedback (dbm/get-unread-feedback)
        num-feedback (count get-unread-feedback)]
    ;;takes the first [only] element named .feedback, clones it, fills it with goodness
    (eh/transform feedback-area [:.feedback]
       (eh/clone-for [i num-feedback]
         (eh/content 
           (feedback-sample-content i ;;loop over every feedback
                                    (nth get-unread-feedback i)))))))

(defn moderator-page-draw [ email ]
  (apply str (eh/emit* 
              (eh/at moderator-page 
                     [:.feedback]    (eh/substitute (feedback-content-transform))
                     ;[:.brief]    (eh/substitute (brief-content-transform))
                     [:.usercard] (eh/substitute (pvu/usercard-transform moderator-page email))
                      ;vine transforms
                     ))))
