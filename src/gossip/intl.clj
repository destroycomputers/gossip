(ns gossip.intl
  (:require [clojure.java.io :as io]
            [clojure.edn :as edn]
            [clojure.string :as string]
            [mount.core :refer [defstate]]
            [gossip.util :as u]))

(def ^:dynamic *locale*)

(def ^:private current-namespace (namespace ::ns))

(defn- from-file
  [f base]
  (let [uri (u/relative-uri f base)
        components (u/uri-components uri)]
    (if-let [[locale-str ns-components file-name] (u/decomp components)]
      (when (string/ends-with? file-name ".edn")
        (let [locale (keyword current-namespace locale-str)
              ns-vec (conj ns-components (u/trim-extension file-name))
              ns-str (string/join "." ns-vec)]
          [locale ns-str])))))

(defn- from-dir
  [dir]
  (for [f (file-seq dir)]
    (when-let [[locale ns-locale] (from-file f dir)]
      {locale
       (u/map-keys #(keyword ns-locale (name %))
                   (edn/read-string (slurp f)))})))

(defn- from-resource-dir
  [dir]
  (-> dir
      io/resource
      io/file
      from-dir))

(defmacro ^:private load-resource
  [name]
  (apply merge-with merge (from-resource-dir name)))

(declare messages)
(defstate messages
  :start (load-resource "localisation"))

(defn- format-args [fmt args]
  (apply format fmt args))

;; TODO: Consider making recursive to render inner messages.
;;         * Use-case: Ranges in ::contiguous message.
(defn render
  [v & {locale :locale :or {locale *locale*}}]
  (let [[key & args] (if (sequential? v) v [v])]
    (-> messages
        (get locale)
        (get key)
        (some-> (format-args args)))))

(defn render-join
  [msgs & {:keys [locale separator] :or {locale *locale*, separator " "}}]
  (string/join separator
               (map #(render % :locale locale) msgs)))
