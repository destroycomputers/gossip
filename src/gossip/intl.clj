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

(defstate messages
  :start (load-resource "localisation"))

;; TODO: what would be nice is to parse `fmt` and recognise some special
;; syntax for handling parameters.
;;
;; One thing I'd like to see is
;; { :template-key "error list: ${0:, *}" }
;;
;; (render [:template-key "error 1", "error 2"])
;; => "error list: error 1, error 2"
(defn- format-args [fmt args]
  (apply format fmt args))

(defn render
  "Render a `message` in a given `locale`.

  Message for rendering must be provided as a vector with its first element
  being a namespaced keyword referencing the desired message template and the
  remainder being the parameters to this template. Each parameter is rendered
  recursively.

  Parameters that do not match the expected format or do not resolve to a
  defined message are rendered as is.

  Example:
  ;; :app/greeting is mapped to \"Hello, %s!\"
  (render ::intl/en_GB [:app/greeting \"John\"])"
  ([message] (render *locale* message))
  ([locale message]
   (if (and (vector? message)
            (keyword? (first message)))
     (let [[key & args] message]
       (-> messages
           (get locale)
           (get key)
           (some-> (format-args (mapv #(render locale %) args)))
           (or message)))
     message)))
