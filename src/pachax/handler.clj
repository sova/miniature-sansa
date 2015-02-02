(ns pachax.handler
  (:require [compojure.core :refer :all]
            [compojure.route :as route]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
            [ring.middleware.file :as rf]
            [pachax.views.global :as vg :only draw-global-view]
            [net.cgrand.enlive-html :as eh]))

(defroutes app-routes
  (GET "/" [] "Hello World")
  (GET "/hax" [] "welcome to the super secret club.")
  (GET "/pero" [] "Hey pero check out this sweet way to make a website.")

  ;(GET "/goodhello" [] (good-hello))
  ;(GET "/login" [] (login))
  ;(GET "/signin" [] (signin))

;;blurbs
   ;post blurbs
  (GET "/post/blurb:id" [id]
    (str "the blurb id is... " id))
  
  (GET "/post/b:id" [id]
    (str "the blurb id is... " id))
  
   ;blurbs 
  (GET "/blurb:id" [id]
    (str "the blurb id is... " id))
  (GET "/b:id" [id]
    (str "the blurb id is awesomely and simply... " id))

  (GET "/edit/blurb:id" [id]
    (str "the blurb id for editing is... " id))
  (GET "/e/b:id" [id]
    (str "the shorthand blurb id for editing is ... " id))

;;comments
  (GET "/comment:id" [id]
    (str "comment with id ... " id))
  (GET "/c:id" [id]
    (str "comment with id ... " id))

  (GET "/edit/comment:id" [id]
    (str "editing comment with id ... " id))

  (GET "e/c:id" [id]
    (str "editing comment with id ... " id))

  ;;post comment to a specific blurb
  (GET "/post/comment:blurb:id" [id]
    (str "posting a comment on blurb with id... " id))

  (GET "/p/c:b:id" [id]
    (str "posting a comment on blurb with id ... " id))

;;articles
  (GET "/article:id" [id]
    (str "article with id of ... " id))
  (GET "/a:id" [id]
    (str "article with id of ... " id))

;;testing templating
  (GET "/sample" []
    (vg/draw-global-view))
;;byeeee
  ;;(GET "/logout" [] (logout))
  ;;(GET "/signout" [] (signout))
  ;;(GET "/exit" [] (exit))
  ;;(GET "/goodbye" [] (goodbye))

;;global and local views
  ;;(GET "/global" [] (global-view))
  ;;(GET "/local" [] (local-view))

;;cycling (bicycling club for browsing and pinning-up new blurbs for your group)
;;  (GET "/cycle" [] (cycle-view))
;;  (GET "/switch" [] (switch-cycle))

;;user settings
  ;;(GET "/user" [] (user-view))
  ;;(GET "/settings" [] (settings-view))

;;revenue
  ;;(GET "/revenue" [] (revenue-view))



  ;grab a param 
;  (GET "/:wildcard" [wildcard]
;    (str "Wildcard = " wildcard))

  ;static files and not-founds
  (route/files "public")
  (route/not-found "It is better to light a candle than to curse the darkness.")) ;;/end defroutes

(def app
  (wrap-defaults app-routes site-defaults))
