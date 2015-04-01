(ns pachax.database.dbmethods
  (:require [datomic.api :only [db q] :as d]))

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
(defn set-schema [] (d/transact conn schema)) ;connect and load schema


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
    
(defn add-tag-to-blurb [blurb-eid email tags]
  (let [cast-bid (Long. blurb-eid)]
    (d/transact conn [{:db/id (d/tempid :db.part/user),
                       :author/email email,
                       :tag/blurb cast-bid,
                       :tag/value tags}])))

;(defn rating-to-signs [ rating ]
;  "Takes a string ['doubleplus' 'needswork'] and returns a string ++/+/;- ..default is + ['plus']"
;  (if (= rating "doubleplus")
;    (str "doubleplus")
;    (if (= rating "needswork")
;      (str "needswork")
;      (str "plus")))) ;default is "+"

(defn get-publisher-email [ eid ]
  (->> (d/q '[:find ?email ?eid
              :in $ ?eid
              :where 
              [?eid author/email ?email]] (d/db conn) eid)
       (map (fn [[email eid]] {:publisher email, :of-eid eid}))))
  
(defn rating-to-participation [rating]
  (cond                                 ;++ 20 doubleplus
    (= rating "doubleplus") 20          ; + 15 plus
    (= rating "needswork") 0            ; -  0 needswork
    (= rating "plus") 15


    ; might be good to call these something else. not necessarily "ratings,"
    ; but user corroborations.
    (= rating "newblurb") -10  ;costs 10 points to post
    (= rating "newtag") -1 ;costs 1 point to tag something
    
    (= rating "tag-corrob") 1 ;earn a point for tag corroboration
                              ; = someone else approves the tag you added. $$$
    :else 0)) ;;default is throw a zero at it.

(defn remove-participation [ pid rating ]
  (d/transact conn [[:db/retract pid :participation/value rating]
;  might be some unused attributes after removing this so...
;  to optimize maybe add the following.
                   ; [:db/retract pid :participation/recipient recipient]
                   ; [:db/retract pid :participation/bequeather bequeather]
                   ; [:db/retract pid :participation/entity eid]
                    ]))

(defn find-participation-given [ eid email ]
  (->> (d/q '[:find ?pid ?participation 
              :in $ ?eid email
              :where
              [?pid :participation/entity ?eid]
              [?pid :participation/value ?participation]] (d/db conn) eid email)
       (map (fn [[pid participation]] {:pid pid, :participation participation}))))

(defn give-rating-participation [ eid giver-email rating ]
  ;;gotta make sure to check existence of prior participation/changes  
  (let [participation-existence (find-participation-given eid giver-email)]
  (if (not (empty? participation-existence))
    (let [pid (get (first participation-existence) :pid)
          participation (get (first participation-existence) :participation)]
      (remove-participation pid participation))))
  ;;resolve author of eid in question.
  (let [publisher (:publisher (last (get-publisher-email eid)))]
    (if (not (= publisher giver-email)) ;make sure author and giver are not the same.
      (d/transact conn [{:db/id (d/tempid :db.part/user),
                         :participation/value rating,
                         :participation/recipient publisher,
                         :participation/bequeather giver-email,
                         :participation/entity eid}]))))

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
    (->> (d/q '[:find ?pid ?participation
                :in $ ?receiver-email
                :where
                [?pid participation/recipient ?receiver-email]
                [?pid participation/value ?participation]] (d/db conn) receiver-email)
         (map (fn [[pid participation]] {:pid pid, :participation participation}))))

(defn get-user-participation-sum [ email ] 
  (reduce +
          (map rating-to-participation
               (map :participation 
                    (get-user-participation email)))))
  

(defn find-rating [ bid email ]
  (->> (d/q '[:find ?rid ?rating ?email
              :in $ ?bid ?email
              :where
              [?rid rating/blurb ?bid]
              [?rid rating/val ?rating]
              [?rid author/email ?email]] (d/db conn) bid email)
       (map (fn [[rid rating email]] {:rid rid, :rating rating, :email email}))))

(defn add-rating [ bid email rating ]
  (let [cast-bid (Long. bid)]
    (do (d/transact conn [{:db/id (d/tempid :db.part/user),
                           :author/email email,
                           :rating/blurb bid,
                           :rating/val rating}])
        ;;give participation to blurb author
        (give-rating-participation cast-bid email rating))))

(defn remove-rating [ rid rating ]
  (d/transact conn [[:db/retract rid :rating/val rating]]))
    ;(if (not-empty rating-result-set)
    ;  (let [rid (get rating-result-set :rid)
    ;        rating (get rating-result-set :rating)]
    ;    ;remove old rating
    ;    (d/transact conn [:db/retract rid, 
    ;                      :rating/val rating]))))
       
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
  (second (first (frequencies (map :bid (get-all-ratings-for-bid bid))))))


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
    

;(defn is-tag-verified-by-email [ tag blurb-eid email ]
  ;;if the email in question submitted the tag, then return true.

  ;;if the user has elected the tag to be correct, return true.

  ;; otherwise, return false
;  (false)
;)

;add commenting to blurbs (=
;(defn add-comment-to-blurb [eid, content, tags, useremail]
;  (d/transact conn [{:db/id (d/tempid :db.part/user),
;                     :comment/parent eid,
;                     :comment/content content,
;                     :comment/tag tags,
;                     :author/email useremail}]))

;; /// retrieving stuff from the db \\\\
(defn get-all-blurbs []
  (->> (d/q '[:find ?name ?content ?email ?b
              :where 
              [?b blurb/title ?name ]
              [?b blurb/content ?content]
              [?b author/email ?email]] (d/db conn))
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
              [?tid tag/blurb ?bid]
              [?tid tag/value ?tags]] (d/db conn) bid)
       (map (fn [[tags]] {:tags tags}))

       (partition-by :bid)
       (map #(assoc (first %) 
                   :tags (clojure.string/join ", " (map :tags %))))))

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

(defn get-all-blurb-history-by-eid [bid]
  (->> (d/q '[:find ?title ?content ?tags ?email ?bid
              :in $ ?bid
              :where
              [?bid blurb/title ?title]
              [?bid blurb/content ?content]
              [?tid tag/blurb ?bid]
              [?tid tag/value ?tags]
              [?tid author/email ?email]] (d/db conn) bid)
       (map (fn [[title content tags email bid]]
              {:title title
               :content content
               :tags tags
               :email email
               :bid bid}))
       (sort-by :email)))

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


;(defn add-tag-to-blurb [tag, blurbID, useremail]
;  (let [conn (create-empty-in-mem-db)]
;    @(d/transact conn [{:db/id (d/tempid :db.part/user),
;                        :blurb/tag tag,
;                        :author/email useremail}])))
;;;;in progress: how do i get blurb entity IDs to update them with the appropriate
;;stuff in the db?  


;; ----- Query functions -----
;(defn add-blurb-content [title, content, useremail] ...)

;(defn add-blurb-tag [blurbID, tag, useremail] ...)

;(defn add-brief-content [content, useremail] ...)

;(defn add-brief-tag [briefID, tag, useremail] ...)

