(ns pachax.handler
  (:require [compojure.core :refer :all]
            [compojure.route :as route]
            [compojure.handler :refer :all]
            [clojure.java.io :as io]
            [clojure.string :as cljstr]

            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
            [ring.middleware.session :refer [wrap-session]]
            [ring.middleware.file :as rf]
            [ring.middleware.params :as rp]
            [ring.util.response :as response]
            [ring.middleware.anti-forgery :refer :all]

            [pachax.views.global :as vg :only draw-global-view]
            [pachax.views.login :as vl :only draw-login-view]
            [pachax.views.post :as vp :only post-draw-page]
            [pachax.views.blurb :as vb :only blurb-page-draw]
            [pachax.views.invite :as vi :only invite-page-draw]
            [pachax.views.feedback :as vf :only feedback-page-draw]
            [pachax.views.moderator :as vm :only moderator-page-draw]

            [pachax.database.dbmethods :as dbm]
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

  (GET "/blurbtest" [] ;;figuring out dom stuff
    (io/resource "blurbtest.html"))
    
  ;(GET "/goodhello" [] (good-hello))
  ;(GET "/signin" [] (signin))

  (GET "/login/:key&:email&:timestamp" [key email timestamp :as request]
    ;; the keys can sometimes have forward slashes so the loginGO fixtoken should have replaced
    ;; any forward slashes with the string "eep a forward slash" all caps no spaces
    (def fixtkey (clojure.string/replace key "EEPAFORWARDSLASH" "/"))
    ;;insert test to see if timestamp is within 33 minute window?  or similar
    (if (scryptgen/check (str email timestamp) fixtkey)
      (do
        ;;set the session vars [email timestamp scrypt-token]
        (let [old-session (:session request)
              currenttime (quot (System/currentTimeMillis) 1000)
              new-session (assoc old-session
                                 :ph-auth-email email,
                                 :ph-auth-timestamp currenttime
                                 :ph-auth-token (scryptgen/encrypt (str email currenttime)))]
          (-> (response/resource-response "login-s.html" {:root "public"})
;(response "<img src=\"../lorentz-rainbow-ball-flrn.gif\"/>You are now logged in! communist party time!<meta http-equiv=\"refresh\" content=\"3;url=/global\" />")
              (assoc :session new-session)
              (assoc :headers {"Content-Type" "text/html"}))))
      (str "something didn't pan out with that auth key yo. redirect to /login...")))

  (POST "/loginGO" [ username-input ]
    (let [lowercaseemail (clojure.string/lower-case (clojure.string/trim username-input))
          timestamp (quot (System/currentTimeMillis) 1000)
          token (scryptgen/encrypt (str lowercaseemail timestamp))
          ;;(java.net.URLEncoder/encode "a/b/c.d%&e" "UTF-8")
          fixtoken (clojure.string/replace token "/" "EEPAFORWARDSLASH")
          link (str "<a  href=\"" "http://localhost:4000/login/" fixtoken "&" lowercaseemail "&" timestamp "\">login link</a>")
          ;;& requested page for immediate redirect
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
          login-str (str "email with login link looks like this:<br/>" link)]
      (if (= true (:verified (first (dbm/check-if-user-verified lowercaseemail))))
        (str login-str)
        (str "please request an account or get an invite."))))
);;end defroutes login routes
  

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

  (POST "/postGO" [ post-title post-input :as request ]
    (let [email (get-in request [:session :ph-auth-email])]
      (if (< 10 (dbm/get-user-participation-sum email))
        (let [blurb-shovel-in @(dbm/add-blurb post-title post-input email)
              ;;derefernce the result of the transaction and voila,
              ;; data you can play with :)
              blurb-eid (:e (second (:tx-data blurb-shovel-in)))]
          (dbm/deduct-blurb-participation email blurb-eid)
          ;;(vb/blurb-page-draw email eid *anti-forgery-token*)
          {:status 302, 
           :body "", 
           :headers {"Location" (str "/blurb" blurb-eid)}})
        (do ;;else not enough participation points
          (str "It costs 10 participation points to post a new blurb.")))))
  

;;;Commenting ~~~~~~
;;;
;  (POST "/commentPostGO" [ comment-contents comment-tags blurb-eid :as request ]
;    (let [email (get-in request [:session :ph-auth-email])]
;          (dbm/add-comment-to-blurb blurb-eid comment-contents comment-tags email)))
  ;(vb/blurb-page-draw email eid))

  (POST "/tagPostGO" [ blurb-eid new-tags :as request ]
;;add functionality to make sure tags are letters
;; and don't have crazy symbols...
    (let [email (get-in request [:session :ph-auth-email])]
      ;tag-shovel-in @
      (if (not (cljstr/blank? new-tags))
        (if (< 1 (dbm/get-user-participation-sum email))
          (let [tag-shovel-in @(dbm/add-tag-to-blurb blurb-eid email new-tags)
                tag-eid (:e (second (:tx-data tag-shovel-in)))]
            (dbm/deduct-tag-participation email tag-eid)
            {:status 302, 
             :body "", 
             :headers {"Location" (str "/blurb" blurb-eid)}})
          (do
            (str "It costs 1 participation point to make a new tag."))))))

  (POST "/ratingPostGO" [ bid new-rating :as request ]
    (let [email (get-in request [:session :ph-auth-email])
          cast-bid (Long. bid)]
      (dbm/new-rating cast-bid email new-rating)
      {:status 302,
       :body "",
       :headers {"Location" (str "/blurb" cast-bid)}}))


  (POST "/tagVerifyGO" [ bid tag :as request]
    (let [email (get-in request [:session :ph-auth-email])
          cast-bid (Long. bid)
          tag-creator (dbm/get-tag-creator cast-bid tag)]
      (if (not (= email tag-creator))
          (dbm/tag-verify-toggle cast-bid tag email))
      {:status 302, 
       :body "", 
       :headers {"Location" (str "/blurb" bid)}}))

  (POST "/tagUnverifyGO" [ bid tag :as request]
    (let [email (get-in request [:session :ph-auth-email])
          cast-bid (Long. bid)]
      (do
        (dbm/tag-verify-toggle cast-bid tag email)
      ;    eid (:e (second (:tx-data tag-shovel-in)))]
      {:status 302, 
       :body "", 
       :headers {"Location" (str "/blurb" bid)}})))

  (POST "/sendInviteGO" [ invite-recipient :as request ]
    ;
    ;in progress
    ;
    (let [email (get-in request [:session :ph-auth-email])
          user-participation (dbm/get-user-participation-sum email)]
      ;;check if user has 10,000 points, if so deduct and send invite.
      (if (<= 10000 user-participation)
        (do

          ;;make sure the e-mail is valid
          ;;make sure that the recipient is not the same as the sender.
          (dbm/send-invite-participation email invite-recipient) ;;deduct 10k points via rating
          ;;send an invite
          ;;activate new user account

          (str "deducted 10,000 participation points and sent an invite to " invite-recipient ))
        ;else tell them not enough minerals
        (str "You only have " user-participation " participation points currently.  You need " (- 10000 user-participation) " more to invite a friend."))))
  
  (POST "/sendFeedbackGO" [ feedback :as request ]
    (let [email (get-in request [:session :ph-auth-email])]
      ;(dbm/send-feedback email feedback)
      ))
          

  (GET "/write" [ :as request ]
    (def email (get-in request [:session :ph-auth-email]))
    (vp/post-page-draw *anti-forgery-token* email))

  (GET "/showmethetoken" []
    (str *anti-forgery-token*))





   ;blurbs 
  (GET "/blurb:id" [id :as request]
    (let [email (get-in request [:session :ph-auth-email])]
      (try 
        (vb/blurb-page-draw email (Long. id) *anti-forgery-token*)
        (catch Exception e 
          ;(str "caught exception: " (.getMessage e)))
          (str "It is better to light a candle than to curse the darkness.")))))
  


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

;;draw the global page (blurb transforms)
  (GET "/global" [ :as request ]
    (if-let [email (get-in request [:session :ph-auth-email])]
      (vg/global-page-draw email)))

;;draw the moderator page
  (GET "/moderator" [ :as request ]
    (if-let [email (get-in request [:session :ph-auth-email])]
      (vm/moderator-page-draw email)))

;;draw the invite page
  (GET "/invite" [ :as request ]
    (if-let [email (get-in request [:session :ph-auth-email])]
      (vi/invite-page-draw email *anti-forgery-token*)))
       
;;draw the feedback page
  (GET "/feedback" [ :as request ]
    (if-let [email (get-in request [:session :ph-auth-email])]
      (vf/feedback-page-draw email *anti-forgery-token*)))

;;byeeee
  (GET "/logout" [ :as request] 
    (if-let [useremail (get-in request [:session :ph-auth-email])]
      {:status 200,
       :body (str "logged out " useremail "<meta http-equiv=\"refresh\" content=\"3;url=/login\" />"),
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
