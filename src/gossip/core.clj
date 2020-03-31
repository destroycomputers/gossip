(ns gossip.core
  (:require [clojure.string :as string]
            [org.httpkit.server :refer [run-server]]
            [compojure.core :refer [defroutes routes GET]]
            [compojure.route :as r]
            [ring.util.response :as resp]
            [mount.core :as m :refer [defstate]]
            [gossip.intl :as intl]
            [gossip.util :as u]
            [gossip.template :as template]
            [gossip.db :as db]
            [gossip.uwu :as uwu])
  (:gen-class))

;;
;; TODO
;; 1. [DONE] If possible, add supplied/random usernames distinction
;;    (prefixes like u:username for supplied and r:username for random?).
;; 2. [WIP] Refactor/decompose further.
;; 3. [WIP] Add tests/specs.
;; 4. Consider SQLite instead of file to store templates.
;; 5. [DONE] If (1) implemented, add possibility to choose template with matching
;;    number of supplied parameters.
;; 6. Do not return 500/exception details on exceptions.
;; 7. [WIP] Insults from https://monkeyisland.fandom.com/wiki/Insult_Sword_Fighting.

;; Route handlers

(defn gossip-add
  [req]
  (let [template (u/from-query req)]
    (if-let [errors (seq (template/check-errors template))]
      (u/response
       (str (intl/render :gossip.generic/errors)
            ": "
            (intl/render-join errors)))
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
    (let [m (u/from-query* req)
          c (u/count-if #(-> % (string/starts-with? "r:") not)
                        (vals m))]
      (u/response
       (-> table
           (db/random' :filter-by #(>= (template/greatest %) c))
           (template/populate m))))))

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

(defn localise
  [handler]
  (fn [req]
    (binding [intl/*locale* ::intl/en_GB]
      (handler req))))

(defn handle-5xx
  [handler]
  (fn [req]
    (try
      (handler req)
      (catch Throwable t
        (-> (resp/response "500 Internal Server Error")
            (resp/status 500))))))

(defn run-app [port]
  (-> (routes app)
      (handle-5xx)
      (localise)
      (run-server {:port port})))

(declare server-stop)
(defstate server-stop
  :start (run-app 80)
  :stop (server-stop))

;; Main entry point.

(defn -main [& _args]
  (m/start))
