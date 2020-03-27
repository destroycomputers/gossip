(ns gossip.template
  (:require [clojure.string :as string]))

(def ^:private param-rx #"\$\{([-+]?\d+)\}")

(declare select-params)

(defn populate
  "Populates placeholders with user names from a supplied map."
  [template m]
  (letfn [(user-provider [k]
            (if-let [name (get m k)]
              (if (string/starts-with? name "@")
                name
                (str "@" name))
              ""))]
    (string/replace template
                    param-rx
                    #(-> % second keyword user-provider))))

(defn- v:analyse
  [ps]
  (reduce (fn [[{mn :min mx :max} missing] x]
            (if (not= (inc mx) x)
              [{:min (min mn x), :max (max mx x)}
               (conj missing {:from (inc mx) :to (dec x) :for x})]
              [{:min (min mn x), :max (max mx x)}
               missing]))
          [{:min 1 :max 0} []]
          ps))

(defn- v:at-most-five
  [{m :max} _]
  (when (> m 5)
    [::at-most-five m]))

(defn- v:at-least-one
  [{m :min} _]
  (when (< m 1)
    [::at-least-one m]))

(defn- v:contiguous
  [_ missing]
  (let [params (filter #(> (:for %) 0) missing)]
    (when (seq params)
      [::contiguous
       (string/join ", "
                    (map (fn [{from :from to :to}]
                           (if (= from to)
                             (format "%d" from)
                             (format "%d to %d" from to)))
                         params))])))

(def ^:private validators
  [v:contiguous
   v:at-least-one
   v:at-most-five])

(defn- apply-validators [ps]
  (let [vs (apply juxt validators)]
    (->> (v:analyse ps)
         (apply vs)
         (filter some?))))

(defn- select-params [template]
  (->> template
       (re-seq param-rx)
       (map last)
       (map #(Long/parseLong %))
       (into (sorted-set))))

(defn check-errors [template]
  (-> template
      select-params
      apply-validators))