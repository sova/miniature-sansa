
(ns pachax.algorithms.hgm
  ;heymann garcia-molina
  ;collaborative creation of communal hierarchical taxonomies in social tagging systems
  ; 2006, Stanford
;  (:require [clojurewerkz.titanium.graph    :as tg]
;            [clojurewerkz.titanium.edges    :as te]
;            [clojurewerkz.titanium.vertices :as tv]
;            [clojurewerkz.titanium.types    :as tt]
;            [clojurewerkz.titanium.query    :as tq]))
)
(defn dot
  "returns the dot product of two vectors (u, v)"
  [u v]
  (apply + (map * u v)))

(defn mag
  "returns magnitude of a vector (square each vector val, add them up, take sqrt)"
  [u]
  (Math/sqrt (apply + (map #(Math/pow % 2) u))))

(defn cosine-similarity 
  "returns the cos(theta) of 2 vectors ... returns 0 if either magnitude is 0 to avoid DIV/BY0"
  [u v]
  (if (= (mag u) 0.0)
    0
    (if (= (mag v) 0.0)
      0
      (/ (dot u v) (* (mag u) (mag v))))))



;(defn- main
;  [& args]
  ;; opens a BerkeleyDB-backed graph database in a temporary directory
;  (tg/open (System/getProperty "java.io.tmpdir"))
;  "Graph business goes here")
;(main)
