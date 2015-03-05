(ns pachax.database.joxadecimal
  (:require [datomic.api :only [db q] :as d]))

;;; Joxadecimal is a shorthand ID for accessing blurbs.
;;; It uses 52 alpha-numeric digits 
;;; excluding easily conflated/confused ones such as i and l and 1.

;;from specsheet

; .|. update in the evening:
;     so thanks to justin_smith on #Clojure, seems best to generate a random shortID for blurbs/briefs when putting them into the DB and incrementally raise it, otherwise, trying to hash the eid of entities will lead to collisions and then wht.  Simpler to add an Id as we go, and it can be 6-9 alphanumeric [applehash] meaning that it won't have confusing duplicate symbols like 1 and L, I, l, q, p, 0, O, o.  Notebook count leads me to 52 unique "digits" [think #ab22e9 hexadecimal] ... call it... joxadecimal?  hex times steve jobs = jox? shrug.
; A B C D E F G H J K L M N P Q R S T U V W X Y Z = 24
; a b c d e f g h   k   m n     r s t u v w x y z = 20
;                                 2 3 4 5 6 7 8 9 =  8
;                                     ----------------   
;                                            52 digits

;So, even something short like 3 joxadecimal digits can be 52^3 in variety 
;                                                   = (* 52 52 52) = 140608

;                                              6^52 = 19 770 609 664      or 19.7 billion

;so a nice way to do a "nexthashavailable" function [is desirable].
;  maybe it looks up the most recent joxadecimal write to the database and then increments it by 1.



;;like hexadecimal, could simply re-encode the eid to jox

;;for now maybe i'll just enjoy the use of eids (=
