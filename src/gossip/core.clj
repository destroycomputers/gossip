(ns gossip.core
  (:require [clojure.string :as string]
            [org.httpkit.server :refer [run-server]]
            [compojure.core :refer [defroutes routes GET]]
            [compojure.route :as r]
            [mount.core :as m :refer [defstate]]
            [gossip.intl :as intl]
            [gossip.util :as u]
            [gossip.template :as template]
            [gossip.db :as db]
            [gossip.uwu :as uwu])
  (:gen-class))

;;
;; TODO
;; 1. [WIP] If possible, add supplied/random usernames distinction
;;    (prefixes like u:username for supplied and r:username for random?).
;; 2. [SEMIDONE] Refactor/decompose further.
;; 3. [WIP] Add tests/specs.
;; 4. Consider SQLite instead of file to store templates.
;; 5. If (1) implemented, add possibility to choose template with matching
;;    number of supplied parameters.
;; 6. Do not return 500/exception details on exceptions.
;; 7. Insults from https://monkeyisland.fandom.com/wiki/Insult_Sword_Fighting.

;; Route handlers

(defn gossip-add
  [req]
  (let [template (u/from-query req)]
    (if-let [errors (seq (template/check-errors template))]
      (u/response
       (str (intl/render :gossip.generic/errors
                         :locale ::intl/en_GB)
            ": "
            (intl/render-join errors
                              :locale ::intl/en_GB)))
      (do
        (db/insert ::db/gossips template)
        (u/response "Gossip saved.")))))

(defn uwu
  [req]
  (u/response
   (-> (u/from-query req)
       uwu/twanswate)))

(defn rng
  [table]
  (fn [req]
    (u/response
     (-> (db/random table)
         (template/populate (u/from-query* req))))))

(defn all
  [table]
  (fn [_req]
    (u/response
     (->> (db/all table)
          (string/join "\n")))))

(defroutes app
  (GET "/gossip" [] (all ::db/gossips))
  (GET "/gossip/rng" [] (rng ::db/gossips))
  (GET "/gossip/add" [] gossip-add)
  (GET "/insult" [] (all ::db/insults))
  (GET "/insult/rng" [] (rng ::db/insults))
  (GET "/comeback" [] (all ::db/comebacks))
  (GET "/comeback/rng" [] (rng ::db/comebacks))
  (GET "/uwu" [] uwu)
  (r/not-found "404 Not Found"))

;; Main entry point.

(defn run-app [port]
  (-> (routes app)
      (run-server {:port port})))

(declare server-stop)
(defstate server-stop
  :start (run-app 80)
  :stop (server-stop))

(defn -main [& _args]
  (m/start))
