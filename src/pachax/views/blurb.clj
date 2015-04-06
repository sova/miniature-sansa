(ns pachax.views.blurb
  (:require [clojure.string]
            [net.cgrand.enlive-html :as eh] :reload
            [pachax.views.usercard :as pvu]
            [pachax.database.dbmethods :as dbm]))

;;replace blurb contents 
(def blurb-page (eh/html-resource "blurb.html"))
(def global-page (eh/html-resource "global.html"))

(def numberOfBlurbsToShow 1)


(defn isDice? [bid]
  (let [ratings-count (dbm/get-ratings-count-for-bid bid)]
    (if (> 7 ratings-count)
      (str "monoisDice")
      (str "monoblurbrating"))))

(defn get-blurb-rating [bid]
  (let [score (dbm/get-score-for-bid bid)
        ratings-count (dbm/get-ratings-count-for-bid bid)]
    (if (< 6 ratings-count)
      (if (< score 10)
        (str "0" score)
        (str score))
      (str ratings-count))))  ;;return the number of ratings so far if there are not 7 yet
                              ;; displayed as drakken dice ttf :]


;;blurb populating
(defn mono-blurb-content [ blurbmap anti-forgery-token ]
  (let [blurbtitle (get blurbmap :title)
        blurbcontent (get blurbmap :content)
        blurbeid (get blurbmap :bid)]
    (list 
     {:tag :div,
      :attrs {:class "monoblurbratingwrap"},
      :content (list 
                {:tag :div,
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
                         :class (isDice? blurbeid)}
                 :content (get-blurb-rating blurbeid)},
                {:tag :form,
                 :attrs {:class "submitRatingForm",
                         :action "ratingPostGO",
                         :method "POST"}
                 :content (list
                           {:tag :input, 
                            :attrs {:value "doubleplus",
                                    :name "new-rating",
                                    :class "rating-submit-button monodoubleplus-button", 
                                    :type "submit"}, 
                            :content nil},
                           {:tag :input, 
                            :attrs {:value "plus", 
                                    :name "new-rating",
                                    :class "rating-submit-button monosingleplus-button", 
                                    :type "submit"}, 
                            :content nil},
                           {:tag :input, 
                            :attrs {:value "needswork", 
                                    :name "new-rating",
                                    :class "rating-submit-button mononeedswork-button", 
                                    :type "submit"}, 
                            :content nil},
                           {:tag :input,
                            :attrs {:type "hidden"
                                    :name "bid"
                                    :value blurbeid},
                            :content nil},
                           {:tag :input, 
                            :attrs {:type "hidden"
                                    :name "__anti-forgery-token",
                                    :value anti-forgery-token}, 
                            :content nil})}    
                )},
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
                      :content blurbcontent})},
    ; {:tag :div,
    ;  :attrs {:id (str "monoblurbtags"),
    ;          :class (str "monoinnerblurbtags")}
    ;  :content blurbtags},
     ;comment form:
    ; {:tag :form, 
    ;  :attrs {:class "submitPostForm",
    ;          :action "commentPostGO"
    ;          :method "POST"} 
    ;:content (list
              ;{:tag :textarea
              ; :attrs {:name "comment-contents"
              ;         :class "postcontentsfield"
              ;         :type "text"
              ;         :rows "10"
              ;         :cols "35"
              ;         :placeholder "a wise comment is a happy comment."
              ;         :autofocus "true"}
              ; :content nil},
             ;)}
)))

(defn blurb-content-transform [ blurbmap anti-forgery-token ]
  (let [blurb-area (eh/select blurb-page [:.monoblurb])]
  ;;takes the first [only] element named .blurb, clones it, fills it with goodness
    (eh/transform blurb-area [:.monoblurb]
                  (eh/content (mono-blurb-content blurbmap anti-forgery-token)))))

;;tag div populating
(defn monoblurb-tag [ bid a-single-tag email anti-forgery-token ]
  (if (not (empty? (dbm/check-verified-tag bid a-single-tag email)))
   ;(set the class of the current-tag-no-div to be ".verified-tag")
    (list 
     {:tag :div,
      :attrs {:id "blurbtag-innerwrap",
              :class "blurbtag-innerwrapbox"},
      :content (list
                {:tag :div,
                 :attrs {:class "blurbtagbox verified"},
                 :content a-single-tag}
                {:tag :form,
                 :attrs {:class "submitTagForm",
                         :action "tagUnverifyGO",
                         :method "POST"}
                 :content (list
                           {:tag :input, 
                            :attrs {:value a-single-tag,
                                    :name "tag",
                                    :class "tag-unverify-button tag-button", 
                                    :type "submit"},
                            :content nil},
                           {:tag :input,
                            :attrs {:type "hidden"
                                    :name "bid"
                                    :value bid},
                            :content nil},
                           {:tag :input, 
                            :attrs {:type "hidden"
                                    :name "__anti-forgery-token",
                                    :value anti-forgery-token},
                            :content nil})
                 })})
    ;else, just set a normal bg color/ no color
    (list 
     {:tag :div,
      :attrs {:id "blurbtag-innerwrap",
              :class "blurbtag-innerwrapbox"},
      :content (list
                {:tag :div,
                 :attrs {:class "blurbtagbox not-yet-verified"},
                 :content a-single-tag}
               {:tag :form,
                 :attrs {:class "submitTagForm",
                         :action "tagVerifyGO",
                         :method "POST"}
                :content (list
                          {:tag :input, 
                           :attrs {:value a-single-tag,
                                   :name "tag",
                                   :class "tag-verify-button tag-button", 
                                   :type "submit"},
                           :content nil},
                          {:tag :input,
                           :attrs {:type "hidden"
                                   :name "bid"
                                   :value bid},
                           :content nil},
                          {:tag :input, 
                           :attrs {:type "hidden"
                                   :name "__anti-forgery-token",
                                   :value anti-forgery-token},
                           :content nil})
                })})))


(defn blurbtag-submit-form [ bid anti-forgery-token]
  (list 
   {:tag :form,
    :attrs {:class "submitTagsForm",
            :action "tagPostGO",
            :method "POST"}
    :content (list
              {:tag :input
               :attrs {:name "new-tags"
                       :class "postcontenttags"
                       :type "text"
                       :placeholder "please add a tag"}
               :content nil},
              {:tag :input, 
               :attrs {:value "Add tag", 
                       :class "postsubmitbutton", 
                       :type "submit"}, 
               :content nil},
              {:tag :input,
               :attrs {:type "hidden"
                       :name "blurb-eid"
                       :value bid},
               :content nil},
              {:tag :input, 
               :attrs {:type "hidden"
                       :name "__anti-forgery-token",
                       :value anti-forgery-token}, 
               :content nil})}))



(defn blurbtag-transform [bid blurbtags email antiforgerytoken]
  (let [blurbtag-area (eh/select blurb-page [:#tagholder])
        number-of-tags (count blurbtags)]
    (eh/transform blurbtag-area [:.blurbtags]
      (eh/clone-for [i (range number-of-tags)]
        (eh/content (monoblurb-tag bid (nth blurbtags i) email antiforgerytoken))))))

;(defn blurbrating-transform [bid blurb-rating antiforgerytoken]
;  (let [blurbrating-area (eh/select blurb-page [:#blurbrating])]
;    (eh/transform blurbrating-area [:#blurbrating]
;                  (eh/content (monoblurb-rating bid blurb-rating antiforgerytoken)))))

(defn blurb-page-draw [ email bid anti-forgery-token ]
  (let [raw-tags (dbm/get-tags-by-bid bid)]
    (if (not (empty? raw-tags))
      (let [blurb-tags (map clojure.string/trim (clojure.string/split (:tags (first (dbm/get-tags-by-bid bid))) #","))
            blurb-content (first (dbm/get-blurb-by-bid bid))
            blurb-rating (first (dbm/find-rating bid email))]
        (apply str (eh/emit* 
                    (eh/at blurb-page 
                           [:.usercard] (eh/substitute (pvu/usercard-transform blurb-page email))
                           [:.monoblurb]    (eh/substitute (blurb-content-transform blurb-content anti-forgery-token))
                         ;[:.brief]    (eh/substitute (brief-content-transform))
                           [:#tagholder] (eh/substitute (blurbtag-transform bid blurb-tags email anti-forgery-token))
                           [:#blurbtagsubmitform] (eh/substitute (blurbtag-submit-form bid anti-forgery-token))
                         ;[:#blurbrating] (eh/substitute (blurbrating-transform bid blurb-rating anti-forgery-token))
                         ;rating is added to the monoblurb area for rendering btns
                      ;vine transforms
                           ))))
    ;else no tags found... avoids null pointer..
     (let [blurb-content (first (dbm/get-blurb-by-bid bid))
           blurb-rating (first (dbm/find-rating bid email))]
       (apply str (eh/emit* 
                   (eh/at blurb-page 
                          [:.usercard] (eh/substitute (pvu/usercard-transform blurb-page email))
                          [:.monoblurb]    (eh/substitute (blurb-content-transform blurb-content anti-forgery-token))
                         ;[:.brief]    (eh/substitute (brief-content-transform))
                         ;don't draw tags when there are none to be found.
                         ;[:#tagholder] (eh/substitute (blurbtag-transform bid blurb-tags email anti-forgery-token))
                          [:#blurbtagsubmitform] (eh/substitute (blurbtag-submit-form bid anti-forgery-token))
                                        ;[:#blurbrating] (eh/substitute (blurbrating-transform bid blurb-rating anti-forgery-token))
                                        ;rating is added to the monoblurb area for rendering btns
                                        ;vine transforms
                          )))))))
