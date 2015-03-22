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
(def set-schema (d/transact conn schema))

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

(defn rating-to-keyword [ rating ]
  "Takes a string ['doubleplus' 'needswork'] and returns a keyword ++/+/- for datomic to use in insertion of rating attribute"
  (if (= rating "doubleplus")
    (keyword "rating.value/++")
    (if (= rating "needswork")
      (keyword "rating.value/-")
      (keyword "rating.value/+")))) ;default is "+"

(defn add-rating-to-blurb [ blurb-eid email rating ]
  (let [cast-bid (Long. blurb-eid)]
    (d/transact conn [{:db/id (d/tempid :db.part/user),
                       :author/email email,
                       :rating/blurb blurb-eid,
                       :rating/value (rating-to-keyword rating)}])))

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
       (map (fn [[name content email eid]] {:title name :content content :email email :eid eid}))
       (sort-by :title)))

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

(defn get-rating-by-bid [bid]
  (->> (d/q '[:find ?bid ?rating ?email
              :in $ ?bid
              :where
              [?rid rating/blurb ?bid]
              [?rid rating/value ?rating]
              [?rid author/email ?email]] (d/db conn) bid)
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
               :bid bid}))
       (sort-by :bid)))

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


(defn get-tags-by-author-and-eid [ email eid ]
  (->> (d/q '[:find ?tags
              :in $ ?email ?eid
              :where
              [?eid author/email ?email]
              [?eid blurb/tag ?tags]] (d/db conn) email eid)))
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

