(ns pachax.views.write
  (:require [net.cgrand.enlive-html :as eh]
            [ring.util.anti-forgery :as ruaf]
            [datomic.api :as d]
            [pachax.views.usercard :as pvu]))

(def inspire-good ["Read not to contradict and confute, but to weigh and consider.",
                     "Clear, kind, true and necessary",
                     "Words in themselves are not the destination."
                     "Get alive with the dreamerless dream."])

(def post-page (eh/html-resource "post.html"))

(defn post-sample-content [antiforgerytoken]
  (list
   {:tag :form, 
    :attrs {:class "submitPostForm",
            :action "postGO"
            :method "POST"} 
    :content (list
              {:tag :input
               :attrs {:name "post-title"
                       :class "postcontenttitle"
                       :type "text"
                       :size "62"
                       :placeholder "title (optional)"}
               :content nil},
              {:tag :textarea
               :attrs {:name "post-input"
                       :class "postcontentsfield"
                       :type "text"
                       :rows "10"
                       :cols "35"
                       :placeholder (rand-nth inspire-good)
                       :autofocus "true"}
               :content nil},
             ; {:tag :input
             ;  :attrs {:name "post-tags"
             ;          :class "postcontenttags"
             ;          :type "text"
             ;          :placeholder "comma separated list of tags"}
             ;  :content nil},
              {:tag :input, 
               :attrs {:value "Post this blurb", 
                       :class "postsubmitbutton", 
                       :type "submit"}, 
               :content nil} 
              {:tag :input, 
               :attrs {:type "hidden"
                       :name "__anti-forgery-token",
                       :value antiforgerytoken}, 
               :content nil})}))
     

(defn post-content-transform [antiforgerytoken]
  (def post-area (eh/select post-page [:.post-field]))
  (eh/transform post-area [:.post-field]
    ;(eh/clone-for [i (range 1)] ;;affect just one post-field area
      (eh/do->
       (eh/content (post-sample-content antiforgerytoken)))))

(defn write-page-draw [antiforgerytoken email]
 (apply str (eh/emit* 
             (eh/at post-page
                    [:.post-field] (eh/substitute (post-content-transform antiforgerytoken))
                    [:.usercard] (eh/substitute (pvu/usercard-transform post-page email))
                    
                    ;ticker transform
                    ))))
