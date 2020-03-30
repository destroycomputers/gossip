(ns gossip.util
  (:require [clojure.string :as string]
            [clojure.spec.alpha :as s]
            [clojure.walk :as walk]
            [ring.util.codec :as codec]))

(defn response
  "Constructs a successful (200 OK) response
  with content type set to plain text."
  [body]
  {:status 200
   :headers {"Content-Type" "text/plain; charset=utf-8"}
   :body body})

(s/def ::status int?)
(s/def ::headers (s/map-of string? string?))
(s/def ::body string?)

(s/def ::response
  (s/keys :req-un [::status ::headers ::body]))

(s/fdef response
  :args (s/cat :body string?)
  :ret ::response
  :fn #(= (:ret %) {:status 200
                    :headers {"Content-Type" "text/plain; charset=utf-8"}
                    :body (-> % :args :body)}))

(defn from-query
  "Retrieves a single value from ?key=value query-string."
  [req]
  (-> (:query-string req)
      (some-> (string/split #"=" 2)
              second)
      (or "")
      codec/url-decode))

(s/def ::query-string string?)
(s/def ::request
  (s/cat :req (s/keys :req-un [::query-string])))

;; TODO: Needs custom generator.
(s/fdef from-query
  :args ::request
  :ret string?
  :fn #(string/includes? (-> % :args :req) (:ret %)))

(defn from-query*
  "Retrieves a map of {key value} from ?key=value&key=value query-string."
  [req]
  (letfn [(url-decode [[k v]] [k (codec/url-decode v)])]
    (-> (:query-string req)
        (some-> (string/split #"&")
                (->> (map #(string/split % #"="))
                     (map url-decode)
                     (into {}))
                (walk/keywordize-keys))
        (or {}))))

(defn count-if
  [pred coll]
  (reduce #(if (pred %2) (inc %1) %1)
          0
          coll))
