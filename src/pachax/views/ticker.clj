(ns pachax.views.ticker
  (:require [clojure.string]
            [net.cgrand.enlive-html :as eh] :reload))

;; ticker transform
(defn ticker-infix [ ]
  (list
   {:tag :span
    :attrs {:id (str "ascension")}
    :content (str "practical human")})) ;;header/ascension contents
  
(defn ticker-transform [ this-page ]
  (def ticker-area (eh/select this-page [:#ticker]))
  (eh/transform ticker-area [:#ticker]
                (eh/clone-for [i (range 1)]
                              (eh/do->
                               (eh/content (ticker-infix))))))
