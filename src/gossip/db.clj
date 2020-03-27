(ns gossip.db
  (:require [clojure.string :as string]))

(def ^:private tables #{::gossips ::insults ::comebacks})

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
  (-> (db-name table)
      slurp
      string/split-lines))

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

(defn insert
  "Inserts a single row into a DB."
  [table template]
  (-> (all table)
      (conj template)
      (->> (store table))))