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

(defn run-async
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

(defn run-times [n fnc]
  (let [results (for [_ (range 0 n)] (fnc))]
    (map (fn [f] @f) results)))

(defn get-async-times [endpoint req-count]
  (->> endpoint
       (partial run-async client/get)
       (run-times (if (string? req-count) (Integer/parseInt req-count) req-count))))

(defn async-get [endpoint]Add
  (run-async client/get endpoint {:socket-timeout 6000000 :conn-timeout 6000000}))


(defn read-input [file]
  (with-open [rdr (clojure.java.io/reader file)]
    (into [] (line-seq rdr))))

(defn make-requests [file]
  (let [futures (for [endpoint (read-input file)]
                  (async-get endpoint))
        results (for [res futures] @res)]
    results))

(defn execution-times [file]
  (map #(/ (:elapsed-time %) 1000.0) (filter #(not (nil? (:elapsed-time %))) (make-requests file))))

(comment

  (:elapsed-time @(async-get "http://192.241.252.92:3001/services/postgis/placeeuropepolygons/geom/vector-tiles/12/2078/1410.pbf"))

  (def results (let [futures (for [endpoint (read-input "resources/paris_placeeuropepolygons/12.tiles")]
                               (async-get endpoint))]
                 (for [res futures] @res)))
  (count (filter nil? (map :elapsed-time results)))
  (map #(/ (:elapsed-time %) 1000.0) (filter #(not (nil? (:elapsed-time %))) results))

  (execution-times "resources/paris_placeeuropepolygons/12.tiles")

  (execution-times "resources/paris_waterworldpolygons_2/12.tiles")
  (def results (make-requests "resources/paris_waterworldpolygons_2/12.tiles"))
  (first results)



  (def res1 (async-get (first (read-input "resources/paris_waterworldpolygons_2/12.tiles"))))
  (def res2 (async-get (second (read-input "resources/paris_waterworldpolygons_2/12.tiles"))))
  (:elapsed-time @res1)
  (:elapsed-time @res2)

  )
