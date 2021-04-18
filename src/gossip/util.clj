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
  ([req] (from-query req nil))
  ([req key]
   (some-> (:query-string req)
           (string/split #"=" 2)
           (as-> [k v]
               (if (or (nil? key)
                       (= key (keyword k)))
                 v
                 nil))
           codec/url-decode)))

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
  (letfn [(val-decode [[k v]] [k (and v (codec/url-decode v))])]
    (-> (:query-string req)
        (some-> (string/split #"&")
                (->> (into {} (comp (map #(string/split % #"="))
                                    (map val-decode))))
                (walk/keywordize-keys))
        (or {}))))

(s/fdef from-query*
  :args ::request
  :ret map?)

(defn count-if
  "Counts amount of elements for which pred evaluates to true."
  [pred coll]
  (reduce #(if (pred %2) (inc %1) %1)
          0
          coll))

(defn map-keys
  "Transform keys of the map with mapping function.
  When called with two arguments (mapper and a map) returns a new map
  with keys transformed by the mapper.
  When called with a single argument (mapper) returns a transducer."
  ([f]
   (map (fn [[k v]] [(f k) v])))
  ([f m]
   (into {} (map-keys f) m)))

(defn map-vals
  "Transform values of the map with mapping function.
  When called with two arguments (mapper and a map) returns a new map
  with values transformed by the mapper.
  When called with a single argument (mapper) returns a transducer."
  ([f]
   (map (fn [[k v]] [k (f v)])))
  ([f m]
   (into {} (map-vals f) m)))

(defn trim-extension
  "Trims extension (if any) from the filename."
  [file-name]
  (-> file-name
      (some-> (string/last-index-of "."))
      (some->> (subs file-name 0))
      (or file-name)))

(defn decomp
  "Decomposes vector in three parts [head [mid] last].
  If the passed vector is less than 2, nil is returned."
  [v]
  (let [c (count v)]
    (case c
      (0 1) nil
      [(first v) (subvec v 1 (dec c)) (last v)])))

(defn relative-uri
  "Relativises URI of given file against uri of given base directory."
  [f base]
  (->> (.toURI f)
       (.relativize (.toURI base))))

(defn uri-components
  "Splits URI in separate components."
  [uri]
  (-> (. uri getRawPath)
      (string/split #"/")))


(defmacro apply-if
  [v p f]
  `(if ~p (~f ~v) ~v))
