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
            [pachax.views.login :as vl :only draw-login-view]
            [pachax.views.upload :as vu]
            [pachax.views.createuser :as vcu]
            [pachax.secret.credentials :as secrets]

            [net.cgrand.enlive-html :as eh]
            [monger.core :as mg]
            [monger.collection :as mc]

            [crypto.password.scrypt :as scryptgen]
            [postal.core :as mailmail]))

(defroutes app-routes
  (GET "/" [] "Hello World")
  (GET "/hax" [] "welcome to the super secret club.")
  (GET "/pero" [] "Hey pero check out this sweet way to make a website.")
  (GET "/cider" [] ;;shuwa shuwa no saidaa
    "<iframe width=\"100%\" height=\"100%\" src=\"https://www.youtube.com/embed/1oFI7khOhtg\" frameborder=\"0\" allowfullscreen></iframe>")

  ;(GET "/goodhello" [] (good-hello))
  (GET "/login" [] (vl/login-ct-html *anti-forgery-token*))
  ;(GET "/signin" [] (signin))

  (GET "/login/:key&:email&:timestamp" [key email timestamp :as req]
    ;; the keys can sometimes have forward slashes so the loginGO fixtoken should have replaced
    ;; any forward slashes with the string "eep a forward slash" all caps no spaces
    (def fixtkey (clojure.string/replace key "EEPAFORWARDSLASH" "/"))
    (if (scryptgen/check (str email timestamp) fixtkey)
      (do
        ;;set the session var to have the user email
        ;(assoc req :session ( email))
        (def reqwithemail (assoc-in req [:session :ph-auth-email] email))
        (def currenttime (quot (System/currentTimeMillis) 1000))
        (def reqwithtimestamp (assoc-in reqwithemail [:session :ph-auth-timestamp] currenttime))
        (def reqwithemail (assoc-in reqwithtimestamp [:session :ph-auth-token] (scryptgen/encrypt (str email currenttime))))
        (str "the new request map looks like " reqwithemail))
      (str "something didn't pan out with that auth key yo.")))

  (POST "/loginGO" [ username-input ] 
    (def timestamp (quot (System/currentTimeMillis) 1000))
    (def token (scryptgen/encrypt (str username-input timestamp)))
    ;(java.net.URLEncoder/encode "a/b/c.d%&e" "UTF-8")
    (def fixtoken (clojure.string/replace token "/" "EEPAFORWARDSLASH"))
    (def link (str "http://localhost:4000/login/" fixtoken "&" username-input "&" timestamp))
    ;;allegedly there is an error here .. but i'm not certain as to what it is.  we'll see... =)
    ;(mailmail/send-message {:host (secrets/host)
    ;                        :user (secrets/user)
    ;                        :pass (secrets/pass)
    ;                        :ssl :blimeyYEs.yo}
    ;                       {:from (secrets/user)
    ;                        :to [ username-input ]
    ;                        ;:cc "bob@example.com"
    ;                        :subject "login request with link"
    ;                        :body (str "Hello,  this is the devbox at sova.so ... your login link is " link)})
    (str "email with login link looks like this:<br/>" link))
  

;;create users

;  (GET "/createuser" [] (vcu/createuser-ct-html *anti-forgery-token*))

  ;(POST "/createuserGO" [useremail]
  ;  (let [conn (mg/connect {:host "127.0.0.1" :port 27272})
  ;        db (mg/get-db conn "ph-users")
  ;        coll "users"]
      ;; hash the user email using scrypt or similarly awesome 1-way.
  ;    (def hashedemail (scryptgen/encrypt useremail))
  ;    (mc/insert db coll {:useremail useremail, :password hashedpw})
  ;    (def retval
  ;      (mc/count db coll))
  ;    (str "successfully created a new user " useremail ", " retval " users so far in the db<br/><br/>"
  ;         (pr-str (mc/find-maps db coll)))))


;;blurbs
   ;post blurbs

  (GET "/post/blurb:id" [id]
    (str "the blurb id is... " id))
  
  (GET "/post/b:id" [id]
    (str "the blurb id is... " id))

;posting test
  (POST "/uploadblurbGO" [short-title-input uploadblurb0 tags-input score-input]
    (let [conn (mg/connect {:host "127.0.0.1" :port 27272})
          db (mg/get-db conn "posts")
          coll "blurbs"]
      (mc/insert db coll {:blurb_content uploadblurb0, :tags tags-input, :score score-input})
      (def retval
        (mc/count db coll))
      (str "the wonderful world of wonka presents " uploadblurb0 " " retval " total entries so far in the database<br/><br/>"
           (pr-str (mc/find-maps db coll)))))



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



  ;grab a param up
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

(assoc site-defaults :cookie-attrs {:max-age 86400})
(def app
  (wrap-defaults app-routes site-defaults))
      
