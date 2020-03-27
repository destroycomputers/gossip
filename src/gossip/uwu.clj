(ns gossip.uwu
  (:require [clojure.string :as string]))

;;
;; Ported from https://github.com/zuzak/owo
;;

(def ^:private prefixes
  ["<3 "
   "0w0 "
   "H-hewwo?? "
   "HIIII! "
   "Haiiii! "
   "Huohhhh. "
   "OWO "
   "OwO "
   "UwU "])

(def ^:private suffixes
  [" :3"
   " UwU"
   " ÙωÙ"
   " ʕʘ‿ʘʔ"
   " ʕ•̫͡•ʔ"
   " >_>"
   " ^_^"
   ".."
   " Huoh."
   " ^-^"
   " ;_;"
   " ;-;"
   " xD"
   " x3"
   " :D"
   " :P"
   " ;3"
   " XDDD"
   ", fwendo"
   " ㅇㅅㅇ"
   " (人◕ω◕)"
   "（＾ｖ＾）"
   " Sigh."
   " x3"
   " ._."
   " (　\"◟ \")"
   " (• o •)"
   " (；ω；)"
   " >_<"])

(def ^:private words
  [["no" "nu"]
   ["has" "haz"]
   ["have" "haz"]
   ["you" "uu"]
   ["the " "da "]
   ["The " "Da "]])

(def ^:private n-or-m? #{\N \n \M \m})
(def ^:private h? #{\H \h})

(defn- affixes [text]
  (str (rand-nth prefixes)
       text
       (rand-nth suffixes)))

(defn- translate-chars [[switches prev r] chr]
  (case chr
    (\L \R)
    [switches chr (conj r \W)]
    (\l \r)
    [switches chr (conj r \w)]
    (\O \o)
    [switches chr (conj r (if (n-or-m? prev) "yo" chr))]
    (\U \u)
    [switches chr (conj r (if (h? prev) "oo" chr))]

    [switches chr (conj r chr)]))

(defn- translate-words [text]
  (reduce #(string/replace %1 (first %2) (second %2))
          text
          words))

(defn twanswate [text]
  (as-> text it
    (reduce translate-chars [#{} nil []] it)
    (nth it 2)
    (string/join it)
    (translate-words it)
    (affixes it)))