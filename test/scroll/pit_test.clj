(ns scroll.pit-test
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.tools.logging :as log]
            [scroll.pit :as pit]
            [utils :as utils]))

(deftest ^:integration pit-client
  (let [es-host (or (System/getenv "ES_HOST") "http://localhost:9200")
        index-name "pit-test-index"
        semantic-version (utils/semantic-es-version es-host)]
    (when (and (<= 7 (:major semantic-version))
               (<= 10 (:minor semantic-version)))
      (log/infof "Index recreated %s" (utils/recreate-index es-host index-name))
      (let [pit-resp (pit/init es-host index-name)]
        (is (string? (:id pit-resp)))
        (is (true? (:succeeded (pit/terminate es-host pit-resp))))

        (testing "Terminating PIT second time is not successful"
          (is (false? (:succeeded (pit/terminate es-host pit-resp)))))))))
