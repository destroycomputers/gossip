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
;; 1. [WIP] Add tests/specs.
;; 2. [WIP] Insults from https://monkeyisland.fandom.com/wiki/Insult_Sword_Fighting.
;;

;; Route handlers

(def comeback-index (atom 0))

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
  "Helper that returns random element of the resource and populates template vars."
  ([table] (rng table (constantly 1)))
  ([table action]
   (fn [req]
     (let [m (u/from-query* req)
           c (u/count-if #(-> % (string/starts-with? "r:") not)
                         (vals m))]
       (u/response
         (-> table
             (db/random-with-idx :filter-by #(>= (template/greatest %) c))
             (as-> [idx tmpl]
               (do
                 (action idx tmpl)
                 (template/populate tmpl m)))))))))

(defn all
  "Helper that implements `list' operation on a resource."
  [table]
  (fn [_req]
    (u/response
     (->> (db/all table)
          (string/join "\n")))))

(defn comeback-reply
  [req]
  (u/response
    (-> (db/all ::db/comebacks)
        (nth @comeback-index))))

(defroutes app
  (GET "/gossip" [] (all ::db/gossips))
  (GET "/gossip/rng" [] (rng ::db/gossips))
  (GET "/gossip/add" [] gossip-add)
  (GET "/insult" [] (all ::db/insults))
  (GET "/insult/rng" [] (rng ::db/insults (fn [idx _] (swap! comeback-index (constantly idx)))))
  (GET "/comeback" [] (all ::db/comebacks))
  (GET "/comeback/rng" [] (rng ::db/comebacks))
  (GET "/comeback/rep" [] comeback-reply)
  (GET "/cats" [] (all ::db/cats))
  (GET "/cats/rng" [] (rng ::db/cats))
  (GET "/uwu" [] uwu)
  (r/not-found "404 Not Found"))

(defn localise
  "Localisation middleware, sets up given locale as a locale of the request."
  [handler locale]
  (fn [req]
    (binding [intl/*locale* locale]
      (handler req))))

(defn handle-5xx
  "Unhandled exception catching middleware. If unhandled exception occurs
  catches it and returns HTTP 500 Internal Server Error."
  [handler]
  (fn [req]
    (try
      (handler req)
      (catch Throwable t
        (println t)
        (-> (resp/response "500 Internal Server Error")
            (resp/status 500)
            (resp/content-type "text/plain"))))))

(defn run-app
  "Runs the application server at a given port."
  [port locale]
  (-> (routes app)
      (handle-5xx)
      (localise locale)
      (run-server {:port port})))

(declare server-stop)
(defstate server-stop
  :start (run-app 80 ::intl/en_GB)
  :stop (server-stop))

;; Main entry point.

(defn -main [& _args]
  (m/start))
