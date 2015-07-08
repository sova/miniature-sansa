(ns pachax.database.dbmethods
  (:require [datomic.api :only [db q] :as d]
            [pachax.algorithms.hgm :as hgm]))

;; Database connection
;(defn create-empty-in-mem-db []
;  (let [uri "datomic:mem://ph-db"]
;    (d/delete-database uri)
;    (d/create-database uri)
;    (let [conn (d/connect uri)
;          schema (load-file "ph-schema.edn")]
;      (d/transact conn schema)
;      conn)))

(def uri "datomic:dev://localhost:4334/ph")

(def conn (d/connect uri))
(def schema (load-file "ph-schema.edn"))
(defn set-schema [] (d/transact conn schema)) ;connect and load schema .. run this when you want to update the current schema like in the instance when you add a new attribute

(defn add-user-to-ph [ user-email ]
  (d/transact conn [{:db/id (d/tempid :db.part/user),
                     :author/email user-email,
                     :account/verified true}]))

(defn make-moderator-acct
  "for use in loading modertor pages to edit blurbs, delete tags"
  [ email ]
  (d/transact conn [{:db/id (d/tempid :db.part/user),
                     :author/email email,
                     :account/moderator true}]))

(defn check-if-moderator [email]
  (->>
   (d/q '[:find ?moderator ?email
          :in $ ?email
          :where
          [?mid :account/moderator ?moderator]
          [?mid :author/email ?email]] (d/db conn) email)
   (map (fn [[ moderator email ]] {:email email, :moderator moderator}))))

(defn check-if-user-verified [user-email]
  (->> 
   (d/q '[:find ?verified ?user-email
              :in $ ?user-email
              :where
              [ ?vid :account/verified ?verified ]
              [ ?vid :author/email ?user-email ]] (d/db conn) user-email)
       (map (fn [[ verified user-email ]] {:email user-email, :verified verified}))))


;(defn changeCardinality []
;  (d/transact conn [{:db/id :participation/value
;                      :db/cardinality :db.cardinality/many
;                      :db.alter/_attribute :db.part/db}]))
;  in case you bump cardinality around in the schema... this will fix :)

;; \\\ putting stuff into the DB. ///
(defn add-blurb [title, content, useremail]
  (d/transact conn [{:db/id (d/tempid :db.part/user),
                     :blurb/title title,
                     :blurb/content content,
                     :author/email useremail}]))

(defn get-blurb-by-bid [bid]
  (->> (d/q '[:find ?title ?content ?bid
              :in $ ?bid
              :where
              [?bid blurb/title ?title]
              [?bid blurb/content ?content]] (d/db conn) bid)
       (map (fn [[title content bid]]
              {:title title
               :content content
               :bid bid}))))
 
    
(defn add-tag-to-blurb [blurb-eid email tags]
  (let [cast-bid (Long. blurb-eid)]
    (d/transact conn [{:db/id (d/tempid :db.part/user),
                       :author/email email,
                       :tag/blurb cast-bid,
                       :tag/value tags}])))

(defn get-tag-creator [bid tag]
  (->> (d/q '[:find ?tid ?email
              :in $ ?bid ?tag
              :where 
              [?tid tag/blurb ?bid]
              [?tid tag/value ?tag]
              [?tid author/email ?email]] (d/db conn) bid tag)
       (map (fn [[tid email]] {:tid tid, :author email}))))

(defn get-tag-by-tid [ tid ]
  (->> (d/q '[:find ?tag ?creator ?bid
              :in $ ?tid
              :where
              [?tid :tag/value ?tag]
              [?tid :author/email ?creator]
              [?tid :tag/blurb ?bid]] (d/db conn) tid)
       (map (fn [[tag creator bid]] {:tag tag, :creator creator, :bid bid}))))

(defn get-publisher-email [ eid ]
  (->> (d/q '[:find ?email ?eid
              :in $ ?eid
              :where 
              [?eid author/email ?email]] (d/db conn) eid)
       (map (fn [[email eid]] {:publisher email, :of-eid eid}))))
  

(defn remove-blurb [bid]
  (let [blurb-info (get-blurb-by-bid bid)
        b-title (:title (first blurb-info))
        b-content (:content (first blurb-info))
        b-author (:publisher (first (get-publisher-email bid)))]
  (d/transact conn [[:db/retract bid :blurb/title b-title]
                    [:db/retract bid :blurb/content b-content]
                    [:db/retract bid :author/email b-author]])))

(defn remove-tag [tag bid]
  ;(let [tag-info 
  ;find all participation for this tag
  ;remove all participation for this tag
  ;remove the tag
)

(defn rating-to-participation [rating]
  (cond                                 ;++ 20 doubleplus
    (= rating "doubleplus") 15         ; + 15 plus
    (= rating "needswork") 0            ; -  0 needswork
    (= rating "plus") 12


    ; might be good to call these something else. not necessarily "ratings,"
    ; but user corroborations.
    (= rating "invitedfriend") -10000 ;costs 10000 points to invite a friend.  keep the quality of the community high =)
    (= rating "newblurb") -10  ;costs 10 points to post
    (= rating "newtag") -1 ;costs 1 point to tag something
    
    (= rating "tag-corrob") 1 ;earn a point for tag corroboration
                              ; = someone else approves the tag you added. $$$
    :else 0)) ;;default is throw a zero at it.

(defn remove-participation [ pid rating ]
  (d/transact conn [[:db/retract pid :participation/value rating]]))
;  might be some unused attributes after removing this so...
;  to optimize maybe add the following. 
                   ; [:db/retract pid :participation/recipient ?? ]
                   ; [:db/retract pid :participation/bequeather ?? ]
                   ; [:db/retract pid :participation/entity ?? ]]))

(defn find-participation-given [ eid giver ]
  (->> (d/q '[:find ?pid ?giver ?recipient ;?rating ?giver ?recipient
              :in $ ?eid 
              :where
              [?pid :participation/entity ?eid]
              [?pid :participation/bequeather ?giver]
              [?pid :participation/recipient ?recipient]] (d/db conn) eid giver)
       (map (fn [[pid giver recipient]] {:pid pid, :giver giver, :recipient recipient}))))

(defn give-rating-participation [ eid giver-email rating ]
  ;;gotta make sure to check existence of prior participation/changes  
  (let [participation-existence (find-participation-given eid giver-email)
        publisher (:publisher (last (get-publisher-email eid)))]
    (if (not (empty? participation-existence))
      (let [pid (get (first participation-existence) :pid)
            participation (get (first participation-existence) :participation)]
        (remove-participation pid participation)))
      ;;resolve author of eid in question.
    (if (not (= publisher giver-email)) ;make sure author and giver are not the same.
      (d/transact conn [{:db/id (d/tempid :db.part/user),
                         :participation/value rating,
                         :participation/recipient publisher,
                         :participation/bequeather giver-email,
                         :participation/entity eid}]))))

(defn send-invite-participation
  "creates a participation entry when sending an invite to deduct points"
  [ sender-email recipient]
  (d/transact conn [{:db/id (d/tempid :db.part/user),
                     :participation/value "invitedfriend",
                     :participation/bequeather recipient, ;;the recipient is the benefactor in this case
                     :participation/recipient sender-email
                     :participation/entity 10000}]))

(defn deduct-blurb-participation 
  "it costs points to post new blurbs"
  [user-email bid]
  (d/transact conn [{:db/id (d/tempid :db.part/user),
                     :participation/value "newblurb"
                     :participation/recipient user-email
                     :participation/bequeather "made-a-blurb"
                     :participation/entity bid}]))

(defn deduct-tag-participation 
  "it costs points to post a new tag"
  [user-email tid]
  (d/transact conn [{:db/id (d/tempid :db.part/user),
                     :participation/value "newtag"
                     :participation/recipient user-email
                     :participation/bequeather "made-a-tag"
                     :participation/entity tid}])) 

(defn check-verified-tag 
  [bid tag verifier]
  (let [verified-result (->> (d/q '[:find ?tid ?pid ?tag ?creator
                                    :in $ ?bid ?tag ?verifier
                                    :where
                                    [?tid :tag/value ?tag]
                                    [?tid :tag/blurb ?bid]
                                    [?tid :author/email ?creator]
                                    [?pid :participation/value "tag-corrob"]
                                    [?pid :participation/entity ?tid]
                                    [?pid :participation/bequeather ?verifier]
                                    ] (d/db conn) bid tag verifier)
                             (map (fn [[tid pid tag creator]] {:tid tid, :pid pid, :tag tag, :tag-maker creator})))]
    verified-result))


(defn get-tag-participation [ tag ] ;[giver tag]
  (->>
   (d/q '[:find ?tid ?pid 
          :in $ ?tag ;?giver ?tag 
          :where
          ;[?tid :tag/verifier ?giver]
          [?tid :tag/value ?tag]
          [?pid :participation/entity ?tid]] (d/db conn) tag))); giver tag)))

(defn tag-verify-toggle
   "unverify the tag and remove the participation
    else, if the tag is not yet verified, verify it and give tag participation"
   [bid tag verifier]
   (if-let [verified-result (check-verified-tag bid tag verifier)]
     (let [pid (:pid (first verified-result))
           tid (:tid (first verified-result))
           tag-maker (:author (first (get-tag-creator bid tag)))]
       (if (not (empty? verified-result))
         (do  ;(unverify tag and remove participation)
           (d/transact conn [[:db/retract pid :participation/bequeather verifier]
                             [:db/retract pid :participation/value "tag-corrob"]
                             [:db/retract pid :participation/recipient tag-maker]
                             ;;tag maker appears to be nil. whiS?
                             [:db/retract pid :participation/entity tid]]))
         (if (not (= verifier tag-maker))
           (do ;else (verified result is empty) verify the tag and give participation :D
                                        ;(println "yo it's empty now what")
             (let [tag-creator-keys (get-tag-creator bid tag)
                   tid (:tid (first tag-creator-keys))
                   tag-maker (:author (first tag-creator-keys))]
                                        ;(println tid verifier tag-maker)))))))
               (d/transact conn [{:db/id (d/tempid :db.part/user),
                                  :participation/value "tag-corrob",
                                  :participation/recipient tag-maker,
                                  :participation/bequeather verifier,
                                  :participation/entity tid}]))))))))

(defn get-entities-via-author-email
  "returns the entity id linked to an author/email field"
  [ email ]
  (d/q '[:find ?eid
         :in $ ?email
         :where 
         [?eid author/email ?email]] (d/db conn) email))

(defn get-user-participation
  "Get author participation by supplying their email"
  [ receiver-email ]
  ;(let [receiver-email (:publisher (last (get-publisher-email eid)))]
    (->> (d/q '[:find ?pid ?rating ?giver ?entity
                :in $ ?receiver-email
                :where
                [?pid :participation/entity ?entity]
                [?pid :participation/recipient ?receiver-email]
                [?pid :participation/bequeather ?giver]
                [?pid :participation/value ?rating]] (d/db conn) receiver-email)
         (map (fn [[pid rating giver entity]] {:pid pid, :participation rating, :giver giver, :entity entity}))))

(defn get-user-participation-sum [ email ] 
  (reduce +
          (map rating-to-participation
               (map :participation 
                    (get-user-participation email)))))

(defn get-all-user-participation []
  (->> (d/q '[:find ?user
              :in $
              :where
              [?aid :author/email ?user]] (d/db conn))
       (map (fn [[user]] {:user user, :participation (get-user-participation-sum user)}))
       (sort-by :participation >)
))

 (defn get-all-tags []
  "return a library of tags, basically returns a list of all tags in the db"
  (->> (d/q '[:find ?tag
              :in $
              :where
              [?tid :blurb/tag ?tag]] (d/db conn))
       (map (fn [[tag]] {:tag tag}))))


(defn get-tag-verified-count [tag bid]
  ;in progress.. so far returns all verifications as a truple <:tag :bid :pid>
  "returns the participation value for a given tag and bid -- effectively a count of how many times it was verified." 
  (->> (d/q '[:find ?tag ?bid ?pid
              :in $ ?tag ?bid
              :where
              [?tid :tag/value ?tag]
              [?tid :tag/blurb ?bid]
              [?pid :participation/entity ?tid]] (d/db conn) tag bid)
       (map (fn [[tag bid pid]] {:tag tag, :bid bid, :pid pid}))
       (count)))

(defn find-rating [ bid email ]
  (->> (d/q '[:find ?rid ?rating ?email
              :in $ ?bid ?email
              :where
              [?rid rating/blurb ?bid]
              [?rid rating/val ?rating]
              [?rid author/email ?email]] (d/db conn) bid email)
       (map (fn [[rid rating email]] {:rid rid, :rating rating, :email email}))))

(defn remove-rating [ rid rating ]
  (d/transact conn [[:db/retract rid :rating/val rating]]))
    ;(if (not-empty rating-result-set)
    ;  (let [rid (get rating-result-set :rid)
    ;        rating (get rating-result-set :rating)]
    ;    ;remove old rating
    ;    (d/transact conn [:db/retract rid, 
    ;                      :rating/val rating]))))
       


(defn get-all-ratings [ ]
  (->> (d/q '[:find ?rating ?rid ?bid
              :in $ 
              :where 
              [?rid rating/blurb ?bid]
              [?rid rating/val ?rating]
              ;[?rid author/email ?email]
              ] (d/db conn))
       (map (fn [[rating rid bid]] {:rating rating, :rid rid, :bid bid}))))

(defn get-ratings-count []
  (->> (frequencies (map :bid (get-all-ratings)))
       (map (fn [[bid frequency]] {:bid bid, :number-of-ratings frequency}))
       (sort-by :bid >)
       (sort-by :number-of-ratings <))) ;; < means monotonically increasing

(defn get-all-ratings-for-bid [ bid ]
  (->> (d/q '[:find ?rating ?rid ?bid
              :in $ ?bid
              :where 
             ;[?rid author/email ?email]
              [?rid rating/blurb ?bid]
              [?rid rating/val ?rating]] (d/db conn) bid)
       (map (fn [[rating rid bid]] {:rating rating, :rid rid, :bid bid}))))

(defn get-ratings-count-for-bid [ bid ]
  (let [ar (get-all-ratings-for-bid bid)]
    (if (empty? ar)
      0
      (second (first (frequencies (map :bid ar)))))))




(defn score-mapping
  "maps rating term to score"
  [rating-word]
  (cond
    (= rating-word "doubleplus") 99
    (= rating-word "needswork") 30
    :else 74)) ;else return 74 for "plus"

(defn turn-ratings-into-score [ ratings-results ]
  (let [ratings-word-list (map :rating ratings-results) 
        ratings-list (map score-mapping ratings-word-list)
        number-of-ratings (count ratings-list)
        sum-of-ratings (reduce + ratings-list)]
    (if (= 0 number-of-ratings)
      0
      (int (/ sum-of-ratings number-of-ratings)))))



(defn get-score-for-bid [ bid ]
  ;; if there are fewer than 7 ratings, show the number of ratings.
  ;; if there are more than 6, show the rating value.
  (let [ratings-lst (get-all-ratings-for-bid bid)
        score (turn-ratings-into-score ratings-lst)
        number-of-ratings (get-ratings-count-for-bid bid)]
        ;; in progress -what to does if no ratings at all? :S <3
    score))
    

(defn check-if-first-seven-ratings-are-needswork [bid]
  (let [number-of-ratings (get-ratings-count-for-bid bid)]
    (if (= 7 number-of-ratings)
      (let [blurb-score-check (get-score-for-bid bid)]
        (if (= blurb-score-check (score-mapping "needswork"))
          (do
            (remove-blurb bid) ;delete blurbs where the first 7 ratings are needswork
            (println "removed blurb " bid))))
      ;(println "not the 7th rating currently.")
      )))

(defn add-rating [ bid email rating ]
  (let [cast-bid (Long. bid)]
    (do (d/transact conn [{:db/id (d/tempid :db.part/user),
                           :author/email email,
                           :rating/blurb bid,
                           :rating/val rating}])
        ;;give participation to blurb author
        (give-rating-participation cast-bid email rating)
        (check-if-first-seven-ratings-are-needswork bid))))


(defn new-rating [ bid email rating ]
  (let [rating-existence (find-rating bid email)
        publisher-email (:publisher (last (get-publisher-email bid)))]
    (do
      (if (not (empty? rating-existence))
        (let [rid (get (first rating-existence) :rid)
              rating (get (first rating-existence) :rating)]
          (remove-rating rid rating)))
      (if (not (= publisher-email email))
        (add-rating bid email rating)
      ;else... publisher and rater are same email so don't do anythan
        ))))
;;add functionality to remove needs-work-seven blurbs


(defn get-score-for-bid [ bid ]
  ;; if there are fewer than 7 ratings, show the number of ratings.
  ;; if there are more than 6, show the rating value.
  (let [ratings-lst (get-all-ratings-for-bid bid)
        score (turn-ratings-into-score ratings-lst)
        number-of-ratings (get-ratings-count-for-bid bid)]
        ;; in progress -what to does if no ratings at all? :S <3
    score))

;add commenting to blurbs (=
;(defn add-comment-to-blurb [eid, content, tags, useremail]
;  (d/transact conn [{:db/id (d/tempid :db.part/user),
;                     :comment/parent eid,
;                     :comment/content content,
;                     :comment/tag tags,
;                     :author/email useremail}]))

(defn get-all-blurbs []
  (->> (d/q '[:find ?name ?content ?email ?b
              :where 
              [?b author/email ?email]
              [?b blurb/title ?name ]
              [?b blurb/content ?content]] (d/db conn))
       (map (fn [[name content email bid]] {:title name :content content :email email :bid bid}))
       (sort-by :bid)))

(defn get-blurb-and-tags-by-eid [bid]
  (let [tag-multiplicity-result 
        (->> (d/q '[:find ?title ?content ?bid ?tags
                    :in $ ?bid
                    :where
                    [?bid blurb/title ?title]
                    [?bid blurb/content ?content]
                    [?tid tag/blurb ?bid]
                    [?tid tag/value ?tags]
                    ](d/db conn) bid)
             ;;the tags are in a separate entity
             (map (fn [[title content bid tags]]
                    {:title title
                     :content content
                     :tags tags
                     :bid bid}))
             (sort-by :bid))]
    (assoc (first tag-multiplicity-result) 
           :tags (clojure.string/join ", " (map :tags tag-multiplicity-result)))))

(defn get-tags-by-bid [bid]
  (->> (d/q '[:find ?tags
              :in $ ?bid
              :where
              [?tid tag/value ?tags]
              [?tid tag/blurb ?bid]] (d/db conn) bid)
       (map (fn [[tags]] {:tags tags}))

       (partition-by :bid)
       (map #(assoc (first %) 
                   :tags (clojure.string/join ", " (map :tags %))))))
(defn show-all-tags []
  (->> (d/q '[:find ?tags
              :in $ 
              :where
              [?tid tag/value ?tags]] (d/db conn))
       (map (fn [[tags]] {:tag tags}))))

(defn get-bid-by-tag [tag]
  (->> (d/q '[:find ?bid ?tag
              :in $ ?tag
              :where
              [?tid tag/value ?tag]
              [?tid tag/blurb ?bid]] (d/db conn) tag)
       (map (fn [[bid tag]] {:bid bid, :tag tag}))))

(defn get-ratings []
  (->> (d/q '[:find ?rid ?rating ?bid
              :in $ 
              :where
              [?rid rating/blurb ?bid]
              [?rid rating/val ?rating]] (d/db conn))
       (map (fn [[rid rating bid]] 
              {:rid rid 
               :rating rating
               :bid bid}))))

(defn get-rating-by-bid-and-author [bid email]
  (->> (d/q '[:find ?bid ?rating ?email
              :in $ ?bid ?email
              :where
              [?rid rating/blurb ?bid]
              [?rid rating/val ?rating]
              [?rid author/email ?email]] (d/db conn) bid email)
       (map 
        (fn [[bid rating email]] 
          {:bid bid, 
           :rating (d/ident (d/db conn) rating), ;enumerated types need (d/ident $ eid)
           :email email}))))

(defn send-feedback 
  "upload feedback to the db that can be viewed by a moderator and set as unread/read pending/resolved"
  [email feedback]
  (d/transact conn [{:db/id (d/tempid :db.part/user),
                     :feedback/content feedback,
                     :feedback/status "unread",
                     ;:feedback/date ...current moment,
                     :author/email email}]))

(defn get-unread-feedback []
  (->> (d/q '[:find ?fid ?email ?content
              :in $
              :where
              [?fid :author/email ?email]
              [?fid :feedback/status "unread"]
              [?fid :feedback/content ?content]] (d/db conn))
       (map (fn [[fid email content]] {:fid fid, :email email, :content content}))))
              
(defn mark-feedback-read [ fid ]
  (d/transact conn [{:db/id fid 
                     :feedback/status "read"}]))

(defn get-tag-comparison-vectors 
  "gets a <unique> list  of bids for where tag1 and tag2 occur, returns 2 vect;ors suitable for cosine-similarity calculation."
  [tag1 tag2]
  (let [first-tag-map (get-bid-by-tag tag1)
        second-tag-map (get-bid-by-tag tag2)
        unique-bid-list  (map :bid (concat first-tag-map second-tag-map))
        first-tag-vec  (map #(get-tag-verified-count tag1 %) unique-bid-list)
        second-tag-vec (map #(get-tag-verified-count tag2 %) unique-bid-list)]
    
                                        ;unique-bid-list is the bids where tag1 and tag2 occur (union)
                                        ;first-tag-vec is the vector-count of occurences in unique-bid-list for tag1
                                        ;second-tag-vec is the same but for tag2

    (hgm/cosine-similarity first-tag-vec second-tag-vec)


))


;;nonfunctioning for some reason..
(defn get-blurbs-by-author [useremail]
  (->> (d/q '[:find ?title ?bid
              :in $ ?useremail
              :where
              [?bid blurb/title ?title]
              [?bid author/email ?useremail]] (d/db conn) useremail)
       (map (fn [[title useremail]]
              {:title title
               :useremail useremail}))
       (sort-by :title)))
  
(defn get-active-blurb-set []
  (->> (d/q '[:find ?title ?content ?tags ?email ?b
              :where
              [?b blurb/title ?title]
              [?b blurb/content ?content]
              [?b blurb/tag ?tags]
              [?b author/email ?email]] (d/db conn))
       (map (fn [[title content tags author eid]] 
              {:title title,
               :content content,
               :tags tags,
               :author author,
               :eid eid}))
       (sort-by :title)))
         

(defn get-comments-for-blurb [ eid ] 
  (->> (d/q '[:find ?content ?tags ?email
              :in $ ?eid
              :where 
              [?eid comment/content ?content]
              [?eid comment/tag ?tags]
              [?eid author/email ?email]] (d/db conn) eid)
       (map (fn [[content tags email]]
              {:content content
               :tags tags
               :email email}))))

(defn get-bids-with-score
  ( []  (map :bid (get-all-blurbs)))
  ( [lowerbound] (filter
            (fn [bid] (< lowerbound (get-score-for-bid bid)))
            (map :bid (get-all-blurbs))))
  ( [lowerbound upperbound] (filter
                         (fn [bid]  (and (< lowerbound (get-score-for-bid bid))
                                         (> upperbound (get-score-for-bid bid))))
                         (map :bid (get-all-blurbs)))))

(defn get-tags-by-author-and-eid [ email eid ]
  (->> (d/q '[:find ?tags
              :in $ ?email ?eid
              :where
              [?eid author/email ?email]
              [?eid blurb/tag ?tags]] (d/db conn) email eid)))

(defn get-bids-x-to-y-ratings [lower-amount upper-amount]
  ;;(1 <= ratings-count < 7)
  (->>
   (d/q '[:find ?bid
          :in $
          :where
          [?bid blurb/content _]
          [?rid rating/blurb ?bid]
          [?rid rating/val _]](d/db conn))
   (filter (fn [[bid]] (<= lower-amount (get-ratings-count-for-bid bid))))
   (filter (fn [[bid]] (> upper-amount (get-ratings-count-for-bid bid))))))

(defn get-bids-n-or-more-ratings [ number-of-ratings threshold-score]
  ; n is typically invoked as 7 
    (->>
     (d/q '[:find ?bid
            :in $
            :where
            [?bid blurb/content _]
            [?rid rating/blurb ?bid]
            [?rid rating/val _]](d/db conn))
  ; (< rating 70)
    (filter (fn [[bid]] (> (get-score-for-bid bid) threshold-score)))
    (filter (fn [[bid]] (> (get-ratings-count-for-bid bid) number-of-ratings)))))

(defn get-blurbs-with-no-ratings []
  (->> (d/q '[:find ?bid
              :in $
              :where
              [?bid blurb/content _]
              (not [_ rating/blurb ?bid])] (d/db conn))
       (sort) ;;get oldest entries with no ratings first
       (take 3)
       (flatten)))

(defn get-nine-blurbs 
  "The nine tiles get populated with 3 not-yet-rated, 3 with ratings-count varying between 1 and 6, and 3 with ratings-count > 7 and average score > 70"
  []
  (->>  ;first 3 tiles || no ratings
   (concat  (take 3 (get-blurbs-with-no-ratings))
                ;;idea was to take the first two (most recent)
                ;; and the last 1 (least recent) of non-rated blurbs.
                ;(conj (last (get-blurbs-with-no-ratings))))
              ;last 3 tiles || rating-conunt > 7 and rating > 70
            (->> (get-bids-n-or-more-ratings 7 70)
                 (take 4))
              ;middle 3 tiles (blurbs with rating-counts ranging from 1 up to but not including 7
            (->> (get-bids-x-to-y-ratings 1 7)
                 ;(shuffle)
                 (take 12)))
   (vector) ;;super fastorama
   ;(distinct)
   ;(take 9)
   (flatten)
;   (shuffle)
))




  ;(sort-by :);;;sort somehow by tx time
  ;(first))) ;first works since there is only one being returned

;;some DB notes -- in the query field area [[ ?b blurb/title ?title ]] 
;; will resolve ?b to the entity id (eid) of the entity -- good piece of info.
;; the rest of the lines have to "balance / stiggle stitch / match up / level out to be tru
