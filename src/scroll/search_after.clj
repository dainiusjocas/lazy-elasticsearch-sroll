(ns scroll.search-after
  (:require [clojure.tools.logging :as log]
            [scroll.batch :as batch]
            [scroll.request :as request]))

(def default-query {:sort ["_doc"]})

(defn start [es-host index-name query opts]
  (request/execute-request
    {:url  (format "%s/%s/_search" es-host (or index-name "*"))
     :body (batch/set-batch-size (or query default-query) opts)
     :opts opts}))

(defn continue [es-host index-name query search-after opts]
  (request/execute-request
    {:url  (format "%s/%s/_search" es-host (or index-name "*"))
     :body (batch/set-batch-size (assoc (or query default-query) :search_after search-after) opts)
     :opts opts}))

(defn extract-hits [batch keywordize?]
  (get-in batch (if keywordize? [:hits :hits] ["hits" "hits"])))

(defn extract-search-after [batch keywordize?]
  (if keywordize?
    (->> batch :hits :hits first :sort)
    (->> (get-in batch ["hits" "hits"])
         first
         (get "sort"))))

(defn fetch [{:keys [es-host index-name query search-after opts] :as req}]
  (try
    (let [batch (if search-after
                  (continue es-host index-name query search-after opts)
                  (start es-host index-name query opts))]
      (log/debugf "Fetching a batch took: %s ms" (or (get batch :took) (get batch "took")))
      (when-let [current-hits (seq (extract-hits batch (get opts :keywordize?)))]
        (lazy-cat current-hits
                  (fetch (assoc req :search-after (extract-search-after batch (get opts :keywordize?)))))))
    (catch Exception e
      (.printStackTrace e)
      (log/errorf "Failed to search_after: %s" e)
      [])))
