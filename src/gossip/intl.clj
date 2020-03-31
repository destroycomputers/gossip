(ns gossip.intl
  (:require [clojure.java.io :as io]
            [clojure.edn :as edn]
            [clojure.string :as string]
            [mount.core :refer [defstate]])
  (:import java.io.PushbackReader))

(def ^:dynamic *locale*)

;; FIXME: refactor this ugliness.
(defn- locale&ns-from-uri [uri]
  (let [components (-> uri .getRawPath (string/split #"/"))
        locale (->> components first (keyword "gossip.intl"))
        file-name (last components)]
    (when (string/ends-with? file-name ".edn")
      (let [ns-components (subvec components 1 (dec (count components)))
            complete-ns (conj ns-components
                              (subs file-name 0 (- (count file-name)
                                                   4)))
            ns-keyword (->> complete-ns (string/join "."))]
        [locale ns-keyword]))))

;; FIXME: refactor this ugliness.
(defn- from-dir
  [dir]
  (for [f (file-seq dir)]
    (when-let [[locale ns-locale]
               (->> f
                    .toURI
                    (.relativize (.toURI dir))
                    locale&ns-from-uri)]
      {locale
       (into {}
             (map (fn [[k v]] [(->> k name (keyword ns-locale)) v]))
             (-> f io/reader (PushbackReader.) edn/read))})))

(defn- from-resource
  [dir]
  (-> dir
      io/resource
      io/file
      from-dir))

(defmacro load-resource
  [name]
  (apply merge-with merge (from-resource name)))

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
