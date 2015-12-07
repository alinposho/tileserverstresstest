(ns pgrestapi.stress-tests
  (:require
    [clj-http.client :as client]
    [incanter.core :refer :all]
    [incanter.charts :refer :all]
    [incanter.stats :refer :all]
    [clojure.tools.logging :as log]))

(defn- nano-to-millis [nanos]
  (/ (double nanos) 1000000.0))

(def ^:const ten-minutes-in-millis (* 10 60 1000))

(defn run-async-with-timing
  "Run the function on a separate thread of execution.
  Exception are materialized and returned in the response.
  Returns a future."
  [fnct & args]
  (future
    (try
      (Thread/sleep 200)
      (let [start (. System (nanoTime))
            res (apply fnct args)]
        (assoc res :elapsed-time (nano-to-millis (- (. System (nanoTime)) start)) :request-start-time (nano-to-millis start)))
      (catch Throwable e
        {:status 500, :exception e}))))

(defn run-async [fnct & args]
  (future (apply fnct args)))


(defn run-times [n fnc]
  (let [results (for [_ (range 0 n)] (fnc))]
    (map (fn [f] @f) results)))

(defn get-async-times [endpoint req-count]
  (->> endpoint
       (partial run-async-with-timing client/get)
       (run-times (if (string? req-count) (Integer/parseInt req-count) req-count))))

(defn async-get [endpoint]
  (run-async-with-timing client/get endpoint {:socket-timeout 6000000 :conn-timeout 6000000}))


(defn read-input [file]
  (with-open [rdr (clojure.java.io/reader file)]
    (into [] (line-seq rdr))))

(defn make-parallel-requests
  ([file] (make-parallel-requests file 0))
  ([file ms-delay-in-between-requests]
   (let [futures (for [endpoint (read-input file)]
                   (do
                     (Thread/sleep ms-delay-in-between-requests)
                     (async-get endpoint)))
         results (for [res futures] @res)]
     results)))

(defn execution-times-parallel-requests
  ([file] (execution-times-parallel-requests file 0))
  ([file ms-delay-in-between-requests]
   (map #(/ (:elapsed-time %) 1000.0)
        (filter #(not (nil? (:elapsed-time %)))
                (make-parallel-requests file ms-delay-in-between-requests)))))

(defn run-with-timing [fnct & args]
  (try
    (let [start (. System (nanoTime))
          res (apply fnct args)]
      (assoc res :elapsed-time (nano-to-millis (- (. System (nanoTime)) start))
                 :request-start-time (nano-to-millis start)))
    (catch Throwable e
      {:status 500, :exception e})))

(defn run-sequential [file]
  (let [results (for [endpoint (read-input file)]
                  (run-with-timing client/get endpoint))]
    (map #(/ % 1000.0) (filter (comp not nil?) (map :elapsed-time results)))))

(defn parallel-clients-get-requests
  "Call an endpoint sequnetially 'times' times by 'clients' number of parallel clients"
  [endpoint times clients]
  (let [call-endpoint (fn [times]
                        (for [_ (range 0 times)]
                          (run-with-timing client/get endpoint)))
        clients-requests (for [_ (range 0 clients)]
                           (run-async call-endpoint times))]
    (map (fn [f] @f) clients-requests)))

(comment

  (def first-place-europe-endpoint "http://192.241.252.92:3001/services/postgis/placeeuropepolygons/geom/vector-tiles/12/2078/1410.pbf")

  (:elapsed-time @(async-get first-place-europe-endpoint))

  (execution-times-parallel-requests "resources/paris_placeeuropepolygons/12.tiles")
  (execution-times-parallel-requests "resources/paris_placeeuropepolygons/12.tiles" 100)
  (:elapsed-time @(async-get (second (read-input "resources/paris_placeeuropepolygons/12.tiles"))))

  (execution-times-parallel-requests "resources/paris_waterworldpolygons_2/12.tiles")
  (def results (make-parallel-requests "resources/paris_waterworldpolygons_2/12.tiles"))
  (first results)


  (execution-times-parallel-requests "resources/paris_poieuropepolygons_2/12.tiles" 100)
  (:elapsed-time @(async-get (second (read-input "resources/paris_poieuropepolygons_2/12.tiles"))))


  (def res1 (async-get (first (read-input "resources/paris_waterworldpolygons_2/12.tiles"))))
  (def res2 (async-get (second (read-input "resources/paris_waterworldpolygons_2/12.tiles"))))
  (:elapsed-time @res1)
  (:elapsed-time @res2)


  (map :elapsed-time (get-async-times first-place-europe-endpoint 20))

  (:elapsed-time (run-with-timing client/get first-place-europe-endpoint))
  (for [_ (range 0 20)] (:elapsed-time (run-with-timing client/get first-place-europe-endpoint)))


  (def results (for [endpoint (read-input "resources/paris_placeeuropepolygons/12.tiles")]
                 (run-with-timing client/get endpoint)))
  (filter (comp not nil?) (map :elapsed-time results))

  (run-sequential "resources/paris_poieuropepolygons_2/12.tiles")
  (execution-times-parallel-requests "resources/paris_poieuropepolygons_2/12.tiles")

  (map (fn [col] (map :elapsed-time col))
       (parallel-clients-get-requests first-place-europe-endpoint 10 5))

  (map #(:elapsed-time @%)
       (for [_ (range 0 10)]
         (run-async-with-timing client/get first-place-europe-endpoint)))

  )
