(ns pachax.handler
  (:require [compojure.core :refer :all]
            [compojure.route :as route]
            [compojure.handler :refer :all]

            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
            [ring.middleware.session :refer [wrap-session]]
            [ring.middleware.file :as rf]
            [ring.middleware.params :as rp]

            [ring.util.response :as rr]
            [ring.middleware.anti-forgery :refer :all]

            [pachax.views.global :as vg :only draw-global-view]
            [pachax.views.upload :as vu]

            [net.cgrand.enlive-html :as eh]
            [ring.middleware.anti-forgery :as raf]))

(defroutes app-routes
  (GET "/" [] "Hello World")
  (GET "/hax" [] "welcome to the super secret club.")
  (GET "/pero" [] "Hey pero check out this sweet way to make a website.")
  (GET "/cider" [] ;;shuwa shuwa no saidaa
    "<iframe width=\"100%\" height=\"100%\" src=\"https://www.youtube.com/embed/1oFI7khOhtg\" frameborder=\"0\" allowfullscreen></iframe>")

  ;(GET "/goodhello" [] (good-hello))
  ;(GET "/login" [] (login))
  ;(GET "/signin" [] (signin))

;;blurbs
   ;post blurbs
  (GET "/post/blurb:id" [id]
    (str "the blurb id is... " id))
  
  (GET "/post/b:id" [id]
    (str "the blurb id is... " id))

;posting test
  (POST "/uploadblurbGO" [uploadblurb0]
    (str "the wonderful world of wonka presents " uploadblurb0))



  (GET "/uploadtestpage" []
    (vu/upload-ct-html *anti-forgery-token*))

  (GET "/showmethetoken" []
    (str *anti-forgery-token*))





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

;;testing at transforms on blurbs
  (GET "/xblurbsample" []
    (vg/blurb-ct-html)
    ;(vg/brief-ct-html)
    )


;;byeeee
  ;;(GET "/logout" [] (logout))
  ;;(GET "/signout" [] (signout))
  ;;(GET "/exit" [] (exit))
  ;;(GET "/goodbye" [] (goodbye))

;;global and local views
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

  ;static files
  (route/files "public")
  ;404
  (route/not-found "It is better to light a candle than to curse the darkness.")) ;;/end defroutes


(defn logue-rap 
  [{:keys [uri]}] 
  {:body (format "You requested %s" uri)})
;;github.com/edbond/CSRF
; more specifically
; https://github.com/edbond/CSRF/blob/master/src/csrf/core.clj

(def app
  ; (-> 
    ; (site app-routes)
    ; (wrap-anti-forgery)
    ; (wrap-session))) 
  (wrap-defaults app-routes site-defaults))

;{:cookie-attrs {:max-age 3600}
                   ;:store (cookie-store {:key "gluA95607layersofgumto2your7shoes"})})))
;app-routes site-defaults ))


