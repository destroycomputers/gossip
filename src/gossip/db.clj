(ns gossip.db
  (:require [clojure.edn :as edn]
            [clojure.string :as string]
            [mount.core :refer [defstate]]))

(def ^:private tables #{::gossips ::insults ::comebacks ::cats})

(def ^:private ^:const +db-storage-file+ "db.edn")

(defn- persist
  [state]
  (spit +db-storage-file+ state))

(defn- restore
  []
  (->> (slurp +db-storage-file+)
       (edn/read-string)))

(defstate memory-storage
  :start (atom (restore))
  :stop  (persist @memory-storage))

(defn- db-name
  [table]
  (if (contains? tables table)
    (str (name table) ".txt")
    (throw (ex-info "Invalid table name"
                    {:given table
                     :accepts tables}))))

(defn all
  "Returns all templates from the DB."
  [table]
  (table @memory-storage))

(defn insert
  "Inserts a single row into a DB."
  [table template]
  (-> (swap! memory-storage update table conj template)
      (persist)))

(defn replace
  "Replaces the given table with the news set of values"
  [table templates]
  (-> (swap! memory-storage assoc table templates)
      (persist)))

(defn delete
  [table]
  (-> (swap! memory-storage dissoc table)
      (persist)))

(defn- store
  "Stores provided templates to a DB replacing current content."
  [table templates]
  (->> templates
       (string/join "\n")
       (spit (db-name table))))

(defn random
  "Returns a random template from the DB."
  [table]
  (-> (all table)
      rand-nth))

(defn random'
  "Returns a random template from the DB.
  Can be supplied with additional filtering function that is applied
  before the random element is selected. If in the result of filtering
  no elements is left, the random element will be chosen from the initial,
  non-filtered, set."
  [table & {filter-by :filter-by :or {filter-by (constantly true)}}]
  (let [rows (all table)]
    (-> (filterv filter-by rows)
        (as-> vs (if (empty? vs) rows vs))
        rand-nth)))

(defn random-with-idx
  "Returns a random template and its index from the DB.
  Can be supplied with additional filtering function that is applied
  before the random element is selected. If in the result of filtering
  no elements is left, the random element will be chosen from the initial,
  non-filtered, set."
  [table & {filter-by :filter-by :or {filter-by (constantly true)}}]
  (let [rows (all table)]
    (-> (filterv filter-by rows)
        (as-> vs (if (empty? vs) rows vs))
        (as-> vs
              (let [idx (-> vs count rand-int)]
                [idx (nth vs idx)])))))
