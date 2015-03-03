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

;; putting stuff into the DB.
(defn add-blurb [title, content, useremail]
  (d/transact conn [{:db/id (d/tempid :db.part/user),
                        :blurb/title title,
                        :blurb/content content,
                        :author/email useremail}]))

(defn get-all-blurbs []
  (->> (d/q '[:find ?name ?content
              :where 
              [?c blurb/title ?name ]
              [?c blurb/content ?content]] (d/db conn))
       (map (fn [[name content]] {:title name :content content}))
       (sort-by :title)))

(defn get-blurbs-by-author [useremail]
  (d/q '[:find ?n ?eid
         :where
         [?a author/email useremail]
         [?eid blurb/title ?n]] (d/db conn)))



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

