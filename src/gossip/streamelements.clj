(ns gossip.streamelements
  (:require [gossip.db :as db]
            [gossip.template :as template]
            [gossip.util :as u]
            [gossip.uwu :as uwu]
            [clojure.walk :as walk]
            [cheshire.core :as json]
            [org.httpkit.client :as http]
            [org.httpkit.sni-client :as sni-client]))

(alter-var-root #'http/*default-client* (fn [_] sni-client/default-client))

(defn- commands
  "Get a list of public commands for the given channel `id`."
  [id]
  (-> (format "https://api.streamelements.com/kappa/v2/bot/commands/%s/public" id)
      (http/get)
      (deref)
      (some-> (:body)
              (json/decode)
              (walk/keywordize-keys))))

(defn- group-by-entities
  [cmds prefix]
  (let [pattern (re-pattern (str "^" prefix "(\\w+?)(?:\\d+)?$"))]
    (->> cmds
         (map #(some->> (:command %)
                        (re-find pattern)
                        (second)
                        (assoc % :entity)))
         (filter (comp some? :entity))
         (map #(select-keys % [:entity :reply]))
         (group-by :entity))))

(defn- make-table-name
  [prefix entity]
  (keyword (str "custom." prefix) entity))

(defn- write-to-db
  [cmds prefix]
  (doseq [[entity entries] cmds]
    (db/replace (make-table-name prefix entity)
                (mapv :reply entries))))

(defn refresh-commands
  [id prefixes]
  (if-let [cmds (commands id)]
    (doseq [prefix prefixes]
      (-> cmds
          (group-by-entities prefix)
          (write-to-db prefix)))))

(defn- service
  [id prefixes delay]
  (loop []
    (try
      (refresh-commands id prefixes)
      (catch Exception e
        (printf "Failed to refresh commands: %s" e)))
    (Thread/sleep (* 1000 delay))
    (recur)))

(defn spawn-service
  [id prefixes delay]
  (doto (new Thread #(service id prefixes delay))
    (.setDaemon true)
    (.start)))

(defn make-handler
  [prefix]
  (fn [req]
    (let [m (u/from-query* req)]
      (u/response
       (some-> (:table m)
               (->> (make-table-name prefix))
               (db/random)
               (template/populate m)
               (u/apply-if (contains? m :uwu) uwu/twanswate))))))
