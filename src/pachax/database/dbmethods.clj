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

(defn add-tag-to-blurb [eid email tags]
  (let [cast-eid (Long. eid)]
    (d/transact conn [{:db/id cast-eid,
                       :author/email email,
                       :blurb/tag tags}])))

(defn is-tag-verified-by-email [ tag blurb-eid email ]
  ;;if the email in question submitted the tag, then return true.

  ;;if the user has elected the tag to be correct, return true.

  ;; otherwise, return false
  (false)
)

;add commenting to blurbs (=
(defn add-comment-to-blurb [eid, content, tags, useremail]
  (d/transact conn [{:db/id (d/tempid :db.part/user),
                     :comment/parent eid,
                     :comment/content content,
                     :comment/tag tags,
                     :author/email useremail}]))

;; /// retrieving stuff from the db \\\\
(defn get-all-blurbs []
  (->> (d/q '[:find ?name ?content ?tags ?email ?b
              :where 
              [?b blurb/title ?name ]
              [?b blurb/content ?content]
              [?b author/email ?email]
              [?b blurb/tag ?tags]] (d/db conn))
       (map (fn [[name content tags email eid]] {:title name :content content :tags tags :email email :eid eid}))
       (sort-by :title)))

(defn get-blurb-by-eid [eid]
  (let [tag-multiplicity-result (->> (d/q '[:find ?title ?content ?tags ?eid
                           :in $ ?eid
                           :where
                           [?eid blurb/title ?title]
                           [?eid blurb/content ?content]
                           [?eid blurb/tag ?tags]] (d/db conn) eid)
                    (map (fn [[title content tags eid]]
                           {:title title
                            :content content
                            :tags tags
                            :eid eid}))
                    (sort-by :eid))]
        (assoc (first tag-multiplicity-result) 
                   :tags (clojure.string/join ", " (map :tags tag-multiplicity-result)))))

(defn get-all-blurb-history-by-eid [eid]
  (->> (d/q '[:find ?title ?content ?tags ?email ?eid
              :in $ ?eid
              :where
              [?eid blurb/title ?title]
              [?eid blurb/content ?content]
             ; [?eid tag/author]
              [?eid author/email ?email]
              [?eid blurb/tag ?tags]] (d/db conn) eid)
       (map (fn [[title content tags email eid]]
              {:title title
               :content content
               :tags tags
               :email email
               :eid eid}))
       (sort-by :email)))

;;nonfunctioning for some reason..
(defn get-blurbs-by-author [useremail]
  (->> (d/q '[:find ?title ?a
              :in $ ?useremail
              :where
              [?a blurb/title ?title]
              [?a author/email ?useremail]] (d/db conn) useremail)
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

