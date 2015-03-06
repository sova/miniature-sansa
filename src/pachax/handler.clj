(ns pachax.handler
  (:require [compojure.core :refer :all]
            [compojure.route :as route]
            [compojure.handler :refer :all]

            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
            [ring.middleware.session :refer [wrap-session]]
            [ring.middleware.file :as rf]
            [ring.middleware.params :as rp]
            [ring.util.response :refer [response]]
            [ring.middleware.anti-forgery :refer :all]

            [pachax.views.global :as vg :only draw-global-view]
            [pachax.views.login :as vl :only draw-login-view]
            [pachax.views.post :as vp :only post-draw-page]
            [pachax.views.blurb :as vb :only blurb-page-draw]
            [pachax.database.dbmethods :as dbm]
            [pachax.views.createuser :as vcu]
            [pachax.secret.credentials :as secrets]

            [net.cgrand.enlive-html :as eh]

            [crypto.password.scrypt :as scryptgen]
            [postal.core :as mailmail]))

(defroutes noauth-routes

 ;if you instead specify {{token :ph-auth-token} :as request} that should bind token to your token, and also allow access to the whole thing as "request"
;
  ;;routes which can be accessed without authentication in the session values
  (GET "/session" [ :as req ]
    (pr-str "hey this is cooooool :D ...." req))

  (GET "/login" [] (vl/login-ct-html *anti-forgery-token*))

  (GET "/" [] "Hello World")
  (GET "/hax" [] "welcome to the super secret club.")
  (GET "/pero" [] "Hey pero check out this sweet way to make a website.")
  (GET "/cider" [] ;;shuwa shuwa no saidaa
    "<iframe width=\"100%\" height=\"100%\" src=\"https://www.youtube.com/embed/1oFI7khOhtg\" frameborder=\"0\" allowfullscreen></iframe>")

  ;(GET "/goodhello" [] (good-hello))
  ;(GET "/signin" [] (signin))

  (GET "/login/:key&:email&:timestamp" [key email timestamp :as request]
    ;; the keys can sometimes have forward slashes so the loginGO fixtoken should have replaced
    ;; any forward slashes with the string "eep a forward slash" all caps no spaces
    (def fixtkey (clojure.string/replace key "EEPAFORWARDSLASH" "/"))
    (if (scryptgen/check (str email timestamp) fixtkey)
      (do
        ;;set the session vars [email timestamp scrypt-token]
        (let [old-session (:session request)
              currenttime (quot (System/currentTimeMillis) 1000)
              new-session (assoc old-session
                                 :ph-auth-email email,
                                 :ph-auth-timestamp currenttime
                                 :ph-auth-token (scryptgen/encrypt (str email currenttime)))]
          (-> (response "You are now logged in! communist party time!")
              (assoc :session new-session)
              (assoc :headers {"Content-Type" "text/html"}))))
      (str "something didn't pan out with that auth key yo. redirect to /login...")))

  (POST "/loginGO" [ username-input ]
    (def lowercaseemail (clojure.string/lower-case (clojure.string/trim username-input)))
    (def timestamp (quot (System/currentTimeMillis) 1000))
    (def token (scryptgen/encrypt (str lowercaseemail timestamp)))
    ;(java.net.URLEncoder/encode "a/b/c.d%&e" "UTF-8")
    (def fixtoken (clojure.string/replace token "/" "EEPAFORWARDSLASH"))
    (def link (str "http://localhost:4000/login/" fixtoken "&" lowercaseemail "&" timestamp)) ;;& requested page for immediate redirect
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
    (str "email with login link looks like this:<br/>" link))) ;;end defroutes login routes
  

(defroutes auth-routes
  ;;these routes need the appropriate session values to verify authentication

;;blurbs
   ;post blurbs

  (GET "/post/blurb:id" [id :as request]
    ;(def email (get-in request [:session :ph-auth-email]))
    ;(vb/blurb-page-draw email eid))
    )

    ;(str "the blurb id is... " id))
  
  (GET "/post/b:id" [id]
    (str "the blurb id is... " id))

;;;; POST ACTION

;;; does cool stuff
;;; replace with datomic upsert method
;;; include dbmethods.clj when time

  (POST "/postGO" [ post-title post-input post-tags :as request ]
    (def email (get-in request [:session :ph-auth-email]))
    ; connect to datomic and write in the request
    ;      add measures to make sure there's no duplication (somehow)
    (def blurb-shovel-in @(dbm/add-blurb post-title post-input post-tags email))
    ;;derefernce the result of the transaction and viola,
    ;; data you can play with :)
    (def eid (:e (second (:tx-data blurb-shovel-in))))

    (def email (get-in request [:session :ph-auth-email])))
    ;(vb/blurb-page-draw email eid))


    ;{:status 200, 
    ; :body (str blurb-shovel-in " the eid is " eid),
    ; :headers {"Content-Type" "text/plain"}})
    

    ; return a new view of the specified blurb.
    ;  potentially in the middle of the nine-tile pool, or on its own.

  (GET "/post" [ :as request ]
    (def email (get-in request [:session :ph-auth-email]))
    (vp/post-page-draw *anti-forgery-token* email))


 ; (POST "/uploadblurbGO" [short-title-input uploadblurb0 tags-input score-input]
    ;(let [conn (mg/connect {:host "127.0.0.1" :port 27272})
    ;      db (mg/get-db conn "posts")
    ;      coll "blurbs"]
    ;  (mc/insert db coll {:blurb_content uploadblurb0, :tags tags-input, :score score-input})
    ;  (def retval
    ;    (mc/count db coll))
    ;  (str "the wonderful world of wonka presents " uploadblurb0 " " retval " total entries so far in the database<br/><br/>"
    ;       (pr-str (mc/find-maps db coll)))))



;  (GET "/uploadtestpage" []
 ;   (vu/upload-ct-html *anti-forgery-token*))

  (GET "/showmethetoken" []
    (str *anti-forgery-token*))





   ;blurbs 
  (GET "/blurb:id" [id :as request]
    ;(str "the blurb id is... " id)
    (def email (get-in request [:session :ph-auth-email]))
    (vb/blurb-page-draw email 17592186045616))


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

  (GET "/e/c:id" [id]
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
  ;(GET "/sample" []
    ;(vg/draw-global-view))

;;testing at transforms on blurbs
  (GET "/global" [ :as request ]
    (def email (get-in request [:session :ph-auth-email]))
    (vg/global-page-draw email))


;;byeeee
  (GET "/logout" [ :as request] 
    (if-let [useremail (get-in request [:session :ph-auth-email])]
      {:status 200,
       :body (str "logged out " useremail),
       :session nil,
       :headers {"Content-Type" "text/html"}}))

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
  (route/not-found "It is better to light a candle than to curse the darkness.")) ;;/end defroutes user routes


(defn logged-in-verify
  [ring-handler]
  (fn new-ring-handler
    [request]
    ;;verify that the scrypt hash of email and timestamp matches.
    (if-let [email (get-in request [:session :ph-auth-email])]
      (let [session   (:session request)
            email     (:ph-auth-email session)
            token     (:ph-auth-token session)
            timestamp (:ph-auth-timestamp session)]
            (if (scryptgen/check (str email timestamp) token)
              (do 
                ;; return response from wrapped handler
                (ring-handler request))
              {:status 200, :body "token don't check out yo!", :headers {"Content-Type" "text/plain"}}))
            ;; return error response
            {:status 200, :body "<a href=\"/login\">Please sign in.</a>", :headers {"Content-Type" "text/html"}})))


(def authenticated-routes
  (-> #'auth-routes 
      (logged-in-verify))) ;;add functionality to support 
                           ;;automatic "hey you wanted this page"
                           ;;post-login redirect. 

(def unauthenticated-routes
  (-> #'noauth-routes))

(defroutes all-routes
  (ANY "*" [] unauthenticated-routes)
  (ANY "*" [] authenticated-routes))


(def app
  (wrap-defaults all-routes site-defaults))
