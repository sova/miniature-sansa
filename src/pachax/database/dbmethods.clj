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

(defn add-tag-to-blurb [eid tags]
  (d/transact conn [{:db/id eid,
                     :blurb/tag tags}]))




;; /// retrieving stuff from the db \\\\
(defn get-all-blurbs []
  (->> (d/q '[:find ?name ?content ?tags
              :where 
              [?b blurb/title ?name ]
              [?b blurb/content ?content]
              [?b blurb/tag ?tags]] (d/db conn))
       (map (fn [[name content tags]] {:title name :content content :tags tags}))
       (sort-by :title)))

;;nonfunctioning for some reason..
(defn get-blurbs-by-author [useremail]
  (d/q '[:find ?title ?a
         :where
         [?a blurb/title ?title]
         [?a author/email useremail]] (d/db conn)))

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
