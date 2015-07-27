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
            [pachax.views.write :as vw :only write-page-draw]
            [pachax.views.blurb :as vb :only blurb-page-draw]
            [pachax.views.invite :as vi :only invite-page-draw]
            [pachax.views.feedback :as vf :only feedback-page-draw]
            [pachax.views.about :as va :only about-page-draw]
            [pachax.views.moderator :as vm :only moderator-page-draw]
            [pachax.views.requestaccount :as vr :only request-page-draw]

            [pachax.database.dbmethods :as dbm]
            [pachax.secret.credentials :as secrets]

            [net.cgrand.enlive-html :as eh]

            [crypto.password.scrypt :as scryptgen]
            [postal.core :as mailmail]))

;;(def THIS_DOMAIN "http://localhost")
(def THIS_DOMAIN "http://practicalhuman.com")

(defroutes noauth-routes

 ;if you instead specify {{token :ph-auth-token} :as request} that should bind token to your token, and also allow access to the whole thing as "request"
;
  ;;routes which can be accessed without authentication in the session values
 
  ;;(GET "session" [ :as req ]
  ;;  (pr-str "hey this is cooooool :D ...." req))


;;login with redirect ... passes the redirect into the login link
  (GET "/login&:redirect" [ redirect :as request ] (vl/login-ct-html redirect *anti-forgery-token*))

;;login without redirect, takes user to global by default
  (GET "/login" [] (
                    vl/login-ct-html *anti-forgery-token*))

  (GET "/" [] 
    {:status 302, 
     :body "welcome to practical human: participatory knowledge archive", 
     :headers {"Location" (str "/login")}})

  (GET "/hax" [] "welcome to the super secret club.")
  (GET "/pero" [] "Hey pero check out this sweet way to make a website.")
  (GET "/cider" [] ;;shuwa shuwa no saidaa
    "<iframe width=\"100%\" height=\"100%\" src=\"https://www.youtube.com/embed/1oFI7khOhtg\" frameborder=\"0\" allowfullscreen></iframe>")

  (GET "/blurbtest" [] ;;figuring out dom stuff
    (io/resource "blurbtest.html"))
    
  ;(GET "/goodhello" [] (good-hello))
  ;(GET "/signin" [] (signin))

;;login link with redirect
  (GET "/login/:key&:email&:timestamp&:redirect" [key email timestamp redirect :as request]
    (let [fixtkey (clojure.string/replace key "EEPAFORWARDSLASH" "/")]
      (if (and
           (< (- (quot (System/currentTimeMillis) 1000) (. Integer parseInt timestamp)) 632) ;; difference in timestamps is less than 10 minutes  ==  600 seconds
           (scryptgen/check (str email timestamp) fixtkey))
        (do
          ;;set the session vars [email timestamp scrypt-token]
          (let [old-session (:session request)
                currenttime (quot (System/currentTimeMillis) 1000)
                new-session (assoc old-session
                                   :ph-auth-email email,
                                   :ph-auth-timestamp currenttime
                                   :ph-auth-token (scryptgen/encrypt (str email currenttime)))]
            (-> (response/response "redirecting") 
                (assoc :session new-session)
                (assoc :headers {"Content-Type" "text/html",
                                 "Location" (str "/" redirect)})
                (assoc :status 302))))
        (str "Looks like your login key expired or had some endemic funk that was not fresh."))))

;;login link without redirect
  (GET "/login/:key&:email&:timestamp" [key email timestamp :as request]
    ;; the keys can sometimes have forward slashes so the loginGO fixtoken should have replaced
    ;; any forward slashes with the string "eep a forward slash" all caps no spaces
    (def fixtkey (clojure.string/replace key "EEPAFORWARDSLASH" "/"))
    (if (and
         (< (- (quot (System/currentTimeMillis) 1000) (. Integer parseInt timestamp)) 632) ;; difference in timestamps is less than 10 minutes  ==  600 seconds
         (scryptgen/check (str email timestamp) fixtkey))
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
      (str "Looks like your login key expired or had some endemic funk that was not fresh.")))

  ;;login link creation with a redirect appendage
  (POST "/loginGO" [ username-input redirect]
    (if (= true (:verified (first (dbm/check-if-user-verified username-input))))
      (let [lowercaseemail (clojure.string/lower-case (clojure.string/trim username-input))
            timestamp (quot (System/currentTimeMillis) 1000)
            token (scryptgen/encrypt (str lowercaseemail timestamp))
            ;;(java.net.URLEncoder/encode "a/b/c.d%&e" "UTF-8")
            fixtoken (clojure.string/replace token "/" "EEPAFORWARDSLASH")
            link (str THIS_DOMAIN "/login/" fixtoken "&" lowercaseemail "&" timestamp )
            ;;& requested page for immediate redirect
            login-str (str "email with login link looks like this:<br/>" link)]
        (do
          (mailmail/send-message {:host secrets/host, :user secrets/user, :pass secrets/pass
                                  :ssl true}
                                 {:from secrets/user, 
                                  :to username-input, :subject "PracticalHuman Login Link Requested."
                                  :body (str "Hello!  
This is your practicalhuman login link sent by our automated mailer.  
Please click on or copy and paste the following link in order to log in to ph.  
If you believe you received this in error, please contact us.
" (str link (if (not (empty? redirect)) (str "&" redirect))) "
With peace and respect,
ph")})
          (str "Thank you for coming to share your kindness, wisdom, and good heart!  <br/>A login link has been sent to your email.  <br/>Please use that to log in.  <br/>It expires in about 10 minutes.")))
      (do ;;else the user doesn't have an activated account...
        (str "please request an account or get an invite."))))


  (POST "/loginGO" [ username-input ]
    (if (= true (:verified (first (dbm/check-if-user-verified username-input))))
      (let [lowercaseemail (clojure.string/lower-case (clojure.string/trim username-input))
            timestamp (quot (System/currentTimeMillis) 1000)
            token (scryptgen/encrypt (str lowercaseemail timestamp))
            ;;(java.net.URLEncoder/encode "a/b/c.d%&e" "UTF-8")
            fixtoken (clojure.string/replace token "/" "EEPAFORWARDSLASH")
            link (str "http://localhost:4000/login/" fixtoken "&" lowercaseemail "&" timestamp )
            ;;& requested page for immediate redirect
            login-str (str "email with login link looks like this:<br/>" link)]
        (do
          (mailmail/send-message {:host secrets/host, :user secrets/user, :pass secrets/pass
                                  :ssl true}
                                 {:from secrets/user, 
                                  :to username-input, :subject "PracticalHuman Login Link Requested."
                                  :body (str "Hello!  
This is your practicalhuman login link sent by our automated mailer.  
Please click on or copy and paste the following link in order to log in to ph.  
If you believe you received this in error, please contact us.
" link "
With peace and respect,
ph")})
          (str "Thank you for coming to share your kindness, wisdom, and good heart!  <br/>A login link has been sent to your email.  <br/>Please use that to log in.  <br/>It expires in about 10 minutes.")))
      (do ;;else the user doesn't have an activated account...
        (str "please request an account or get an invite."))))



  (GET "/request" [ :as request ]
    (vr/request-page-draw *anti-forgery-token*))

  (POST "/request" [ email request-essay :as request ]
    ;(str email " and the essay was : " request-essay)
    (dbm/request-account email request-essay)
    (str "Thanks!  Your account request has been sent for review."))
    
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
        (if (not (= email invite-recipient))  ;;make sure that the recipient is not the same as the sender.
          (do ;;justin_smith says email spec is whack so don't bother trying to check its validity
            (dbm/send-invite-participation email invite-recipient) ;;deduct 10k points via rating
            (dbm/add-user-to-ph invite-recipient) ;;activate new user account
            ;;send an invite
            (mailmail/send-message {:host secrets/host, :user secrets/user, :pass secrets/pass
                                    :ssl true}
                                   {:from secrets/user, 
                                    :to invite-recipient, 
                                    :subject "PracticalHuman Invite!  Somebody loves you."
                                    :body (str "Hello!  It's your lucky day.  Your friend, " email " has sent you an exclusive invite to PracticalHuman, participatory knowledge archive.  Your account is active, please stop by any time to log in.")})

            (str "deducted 10,000 participation points and sent an invite to " invite-recipient )))
        ;;else tell them not enough minerals
        (str "You only have " user-participation " participation points currently.  You need " (- 10000 user-participation) " more to invite a friend."))))
  
  (POST "/sendFeedbackGO" [ feedback :as request ]
    (let [email (get-in request [:session :ph-auth-email])]
      (dbm/send-feedback email feedback)
      (str "Thanks!  Your feedback has been sent.")))

  (POST "/markFeedbackReadGO" [ fid :as request ]
    (dbm/mark-feedback-read (Long. fid))
    {:status 302,
     :body "",
     :headers {"Location" (str "/moderator")}})

  (POST "/markAccountRequestReadGO" [ arid :as request ]
    (dbm/mark-account-request-read (Long. arid))
    {:status 302,
     :body "",
     :headers {"Location" (str "/moderator")}})

  (POST "/addNewUserGO" [ user-to-add :as request ]
    (let [email (get-in request [:session :ph-auth-email])]
      ;;check if user is a mod
      (if (:moderator (first (dbm/check-if-moderator email)))
        (do
          (dbm/add-user-to-ph user-to-add)
          (str "Added user " user-to-add " to ph!")))))

  (POST "/giftParticipationGO" [ user-email :as request  ]
    (let [giver (get-in request [:session :ph-auth-email])]
      ;;check if giver is a mod
      (if (:moderator (first (dbm/check-if-moderator giver)))
        (do
          (dbm/gift-user-participation user-email giver)
          (str "hey you gave " user-email " a participation point boost!")))))

  (POST "/removeBlurbGO" [ blurb-to-remove :as request  ]
    (let [email (get-in request [:session :ph-auth-email])]
      ;;check if email is a mod
      (if (:moderator (first (dbm/check-if-moderator email)))
   ;     (str "you're a mod, let's remove this blurb!")
        (do
          (dbm/remove-blurb (Long. blurb-to-remove))
          (str "removed blurb with bid " blurb-to-remove " successfully")))))
  
  (POST "/removeTagGO" [ tag-to-remove tag-in-bid-to-remove :as request ]
    (let [email (get-in request [:session :ph-auth-email])]
      ;;check if email is a mod
      (if (:moderator (first (dbm/check-if-moderator email)))
        (do
          (dbm/remove-tag tag-to-remove (Long. tag-in-bid-to-remove))
          (str "removed tag " tag-to-remove " from bid " tag-in-bid-to-remove)))))

  (GET "/write" [ :as request ]
    (let [email (get-in request [:session :ph-auth-email])]
      (vw/write-page-draw *anti-forgery-token* email)))

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
;;  (GET "/article:id" [id]
;;    (str "article with id of ... " id))
;;  (GET "/a:id" [id]
;;    (str "article with id of ... " id))

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
      (if (= true (:moderator (first (dbm/check-if-moderator email))))
        (vm/moderator-page-draw email *anti-forgery-token*)
        (str "This account does not have moderator privs."))))

;;draw the invite page
  (GET "/invite" [ :as request ]
    (if-let [email (get-in request [:session :ph-auth-email])]
      (vi/invite-page-draw email *anti-forgery-token*)))
       
  ;;draw the feedback page
  (GET "/feedback" [ :as request ]
    (if-let [email (get-in request [:session :ph-auth-email])]
      (vf/feedback-page-draw email *anti-forgery-token*)))
  

  ;;draw the about page
  (GET "/about" [ :as request ]
    (if-let [email (get-in request [:session :ph-auth-email])]
      (va/about-page-draw email *anti-forgery-token*)))

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
    (let [desired-redirect (:route-params request)
          pre-redirect (second (first desired-redirect))
          ph-redirect (clojure.string/replace-first pre-redirect "/" "")]
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
        {:status 200, :body (str "<a href=\"/login&" ph-redirect "\">Please sign in</a> to access " ph-redirect), :headers {"Content-Type" "text/html"}}))))



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
