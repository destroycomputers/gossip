(ns gossip.openai
  (:require [cheshire.core :as json]
            [clojure.string :as string]
            [clojure.walk :as walk]
            [gossip.util :as u]
            [org.httpkit.client :as http]
            [org.httpkit.sni-client :as sni-client]))

(def ^:dynamic system-prompt nil)
(def ^:dynamic api-key nil)
(def ^:dynamic model-name "gpt-3.5-turbo")

(def ^:private interactions (atom []))

(alter-var-root #'http/*default-client* (fn [_] sni-client/default-client))

(defn complete
  [text]
  (-> (http/post "https://api.openai.com/v1/completions"
                 {:headers {"Content-Type" "application/json"}
                  :oauth-token api-key
                  :body (json/encode {:model "text-davinci-003"
                                      :prompt text
                                      :max_tokens 700
                                      :temperature 0})})
      (deref)
      (:body)
      (json/decode)))

(defn chat-complete
  [text context source]
  (-> (http/post "https://api.openai.com/v1/chat/completions"
                 {:headers {"Content-Type" "application/json"}
                  :oauth-token api-key
                  :body (json/encode {:model model-name
                                      :messages (conj (reduce conj [{:role "system" :content system-prompt}] context)
                                                      {:role "user" :content text})

                                      :temperature 1.0
                                      :user source})})
      (deref)
      (:body)
      (json/decode)
      (walk/keywordize-keys)))

(defn chat-complete-with-memory
  [text source]
  (let* [response (chat-complete text @interactions source)
         message (-> (:choices response) first :message)]
    (if (or (nil? (:role message))
            (nil? (:content message)))
      " > Luigi tripped and couldn't reply"
      (do (swap! interactions #(vec (if (> (count %) 40)
                                      (drop 2 (conj %
                                                    {:role "user" :content text}
                                                    {:role (:role message) :content (:content message)}))
                                      (conj %
                                            {:role "user" :content text}
                                            {:role (:role message) :content (:content message)}))))
          (:content message)))))

(defn generate-response
  [{:keys [prefs user source]}]
  (let [prefs (string/replace-first (str prefs) #"^!\w+" "")]
    (chat-complete-with-memory (format "Customer named %1$s comes to Luigi. %1$s: Recommend me a pizza %2$s. Luigi:"
                                       user prefs)
                               source)))

(defn image [description]
  (-> (http/post "https://api.openai.com/v1/images/generations"
                 {:headers {"Content-Type" "application/json"}
                  :oauth-token api-key
                  :body (json/encode {:prompt description
                                      :size "512x512"})})
      (deref)
      (:body)
      (json/decode)))
