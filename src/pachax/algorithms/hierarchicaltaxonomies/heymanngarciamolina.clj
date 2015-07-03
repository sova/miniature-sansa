(ns heirarchicaltaxonomies.heymanngarciamolina
  (:gen-class))

(comment
  (def objects
  ["tomato"
   "starfish"
   "apple"
   "leopard"])

(def users
  ["vaso"
   "ryan"
   "wendy"])

(def tags
  ["fruit"
   "animal"
   "tasty"
   "round"
   "spotted"])
)
;(def annotations ;object, user, tag
;  ["tomato" "vaso" "fruit"]
;  [object, user, tag1, tag2, tag3]
;  [object, user, tag])

(def bicycle [0 5])
(def world [2 3])

;(defn make-tag-map
;  "makes a map where each tag (key) has the number of times it annotates given object")

(defn dot
  "returns the dot product of two vectors (u, v)"
  [u v]
  (apply + (map * u v)))

(defn mag
  "returns magnitude of a vector (square each vector val, add them up, take sqrt)"
  [u]
  (Math/sqrt (apply + (map #(Math/pow % 2) u))))

(defn cosine-similarity 
  "returns the cos(theta) of 2 vectors"
  [u v]
  (/ (dot u v) (* (mag u) (mag v))))

(defn -main
  "potential we all have"
  [& args]
  (println "i'm about to organize some awesome tags yo"))
