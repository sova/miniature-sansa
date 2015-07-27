(ns pachax.views.about
  (:require [clojure.string]
            [net.cgrand.enlive-html :as eh] :reload
            [pachax.views.usercard :as pvu]
            [pachax.views.ticker :as pvt]
            [pachax.database.dbmethods :as dbm]))

;;replace about.html contents 
(def about-page (eh/html-resource "about.html"))

(def about-text 
  {:title "About PracticalHuman: participatory knowledge archive", 
   :phases "PracticalHuman is an effort in 3 phases, we are currently on phase 1."
   :blurbs "You can submit a blurb by going to -write- in the user panel on the left."
   :tags   "Users are encouraged to tag things by submitting tags for specific blurbs via the input field on the blurb's individual page."
   :tag-verify "You can also verify tags others have posted by clicking on the tags that appear to the right of a blurb on its individual page."
   :ratings "You can rate blurbs 'doubleplus', 'plus', or 'needs work' by the tri-colored square in the corner of a blurb.  Ratings users give will eventually contribute to their scores."
   :dice "Blurbs with fewer than 7 ratings will simply show a dice-style count of how many have been cast so far."
   :points "Over time, via sharing content, you'll accumulate points.  They show up in your user panel near your email.  Eventually, you'll be able to do cool things with them, like invite friends to join the community."
})


;;about explanation populating
(defn about-content [ email anti-forgery-token ]
  (let [about-title      (get about-text :title)
        about-phases     (get about-text :phases)
        about-blurbs     (get about-text :blurbs)
        about-tag-verify (get about-text :tag-verify)
        about-ratings    (get about-text :ratings)
        about-dice       (get about-text :dice)
        about-points     (get about-text :points)]
    (list 
     {:tag :div,
      :attrs {:class "about-description"},
      :content (list 
                {:tag :div,
                 :attrs {:id (str "about-title")
                         :class (str "about-block")}
                 :content about-title},
                 {:tag :div,
                  :attrs {:id (str "about-phases")
                          :class (str "about-block")}
                  :content about-phases},
                 {:tag :div,
                  :attrs {:id (str "about-blurbs")
                          :class (str "about-block")}
                  :content about-blurbs}
                 {:tag :div,
                  :attrs {:id (str "about-tag-verify")
                          :class (str "about-block")}
                  :content about-tag-verify}
                 {:tag :div,
                  :attrs {:id (str "about-ratings")
                          :class (str "about-block")}
                  :content about-ratings}
                 {:tag :div,
                  :attrs {:id (str "about-dice")
                          :class (str "about-block")}
                  :content about-dice}
                 {:tag :div,
                  :attrs {:id (str "about-points")
                          :class (str "about-block")}
                  :content about-points})})))

(defn about-content-transform [ email anti-forgery-token ]
  (let [about-area (eh/select about-page [:.about])]
    (eh/transform about-area [:.about]
                  (eh/content 
                   (about-content email anti-forgery-token)))))


(defn about-page-draw [ email anti-forgery-token ]
  (apply str (eh/emit* 
              (eh/at about-page
                     [:#ticker] (eh/substitute (pvt/ticker-transform about-page))
                     [:.usercard] (eh/substitute (pvu/usercard-transform about-page email))
                     [:.about] (eh/substitute (about-content-transform email anti-forgery-token))))))
