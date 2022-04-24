(ns gossip.core
  (:require [clojure.edn :as edn]
            [clojure.string :as string]
            [clojure.tools.cli :refer [parse-opts]]
            [clout.core :refer [route-compile]]
            [compojure.core :refer [defroutes make-route routes GET]]
            [compojure.route :as r]
            [gossip.db :as db]
            [gossip.intl :as intl]
            [gossip.streamelements :as se]
            [gossip.template :as template]
            [gossip.util :as u]
            [gossip.uwu :as uwu]
            [mount.core :as m :refer [defstate]]
            [org.httpkit.server :refer [run-server]]
            [ring.util.response :as resp])
  (:gen-class))

;;
;; TODO
;; 1. [WIP] Add tests/specs.
;; 2. [WIP] Insults from https://monkeyisland.fandom.com/wiki/Insult_Sword_Fighting.
;; 3. Get insults from streamelements website.
;;

;;; Route handlers

(defn gossip-add
  [req]
  (let [template (u/from-query req)]
    (if-let [errors (seq (template/check-errors template))]
      (u/response
       (str (intl/render [:gossip.generic/errors])
            ": "
            (string/join " "
                         (mapv intl/render errors))))
      (do
        (db/insert ::db/gossips template)
        (u/response "Gossip saved.")))))

(defn uwu
  [req]
  (u/response
   (-> (u/from-query req)
       uwu/twanswate)))

;; Used by `rng` to store the latest index generated.
;; This index then used by `reply-from` to reply with an appropriate response.
(def rng-index (atom {}))

(defn rng
  "Helper that returns random element of the resource and populates template vars."
  [table]
  (fn [req]
    (let [m (u/from-query* req)
          c (u/count-if #(-> % (string/starts-with? "r:") not)
                        (vals (dissoc m :uwu)))]
      (u/response
       (-> table
           (db/random-with-idx :filter-by #(>= (template/greatest %) c))
           (as-> [idx tmpl]
                 (do
                   (swap! rng-index #(assoc % table idx))
                   (template/populate tmpl m)))
           (u/apply-if (contains? m :uwu) uwu/twanswate))))))

(defn reply-from
  [table & {idx-source :using-rng}]
  (fn [_req]
    (u/response
     (-> (db/all table)
         (nth (idx-source @rng-index 0))))))

(defn all
  "Helper that implements `list` operation on a resource."
  [table]
  (fn [_req]
    (u/response
     (->> (db/all table)
          (string/join "\n")))))

(defroutes app
  (GET "/gossip" [] (all ::db/gossips))
  (GET "/gossip/rng" [] (rng ::db/gossips))
  (GET "/gossip/add" [] gossip-add)
  (GET "/taunt" [] (all ::db/taunts))
  (GET "/taunt/rng" [] (rng ::db/taunts))
  (GET "/comeback" [] (all ::db/comebacks))
  (GET "/comeback/rng" [] (rng ::db/comebacks))
  (GET "/comeback/rep" [] (reply-from ::db/comebacks :using-rng ::db/taunts))
  (GET "/uwu" [] uwu))

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

(defstate app-config
  :start (try
           (-> (m/args)
               :config
               (slurp)
               (edn/read-string))
           (catch Exception e {})))

(defn routes-from-config
  [cfg]
  (let [commands (-> cfg :streamelements :commands)]
    (->> (for [{:keys [endpoint prefix]} commands]
           (make-route :get
                       (route-compile endpoint)
                       (se/make-handler prefix)))
         (apply routes))))

(defn run-app
  "Runs the application server at a given port."
  [port locale]
  (println "Listening on" port)
  (-> (routes app
              (routes-from-config app-config)
              (r/not-found "404 Not Found"))
      (handle-5xx)
      (localise locale)
      (run-server {:port port})))

(defn app-port
  []
  (or (:port (m/args))
      (some-> "PORT" System/getenv Integer/parseInt)
      8080))

(defstate server-stop
  :start (run-app (app-port) ::intl/en_GB)
  :stop (server-stop))

(defstate service-thread
  :start (let [cfg (-> app-config :streamelements)]
           (se/spawn-service (->> cfg :id)
                             (->> cfg :commands (mapv :prefix))
                             (->> cfg :delay)))
  :stop (.stop service-thread))

(def options
  [["-p" "--port PORT" "Port to listen on. If unspecified, will also be looked up in the PORT env variable."
    :default nil
    :default-desc "8080"
    :parse-fn #(Integer/parseInt %)
    :validate [#(< 0 % 65536) "Must be a number between 0 and 65536"]]
   ["-c" "--config PATH" "Path to a config file."
    :default "gossip.edn"]
   ["-h" "--help" "Print this message."]])

(defn usage
  ([summary] (usage summary nil))
  ([summary errors]
   (let [errors (some->> errors
                         (map #(format "  - %s" %))
                         (cons "\nERRORS:"))]
     (->> (concat ["USAGE: gossip [FLAGS]"
                   ""
                   "FLAGS:"
                   summary]
                  errors)
          (string/join \newline)))))

;; Main entry point.

(defn -main [& args]
  (let [opts (parse-opts args options)]
    (cond
      (-> opts :options :help)
      (println (usage (:summary opts)))
      (:errors opts)
      (println (usage (:summary opts) (:errors opts)))
      :else
      (m/start-with-args (:options opts)))))
