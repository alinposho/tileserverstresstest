(ns request.util
  (:require [clj-http.client :as client]))


(defn nano-to-millis [nanos]
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
  "Run the function on a separate thread. Returns the future running the code "
  (future (apply fnct args)))

(defn run-times
  "Runs the function in separate threads. The number of runs is determined by n.
  This function blocks for all threads to complete!"
  [n fnc]
  (let [results (for [_ (range 0 n)] (fnc))]
    (map (fn [f] @f) results)))

(defn get-async-times
  "Performs a get request asynchronously, repeating it a number of times."
  [endpoint req-count]
  (->> endpoint
       (partial run-async-with-timing client/get)
       (run-times (if (string? req-count) (Integer/parseInt req-count) req-count))))

(defn async-get
  "Performs an asynchronous GET request to the specified endpoint. It returns a future.
  'Implementation detail'
  For some reason the clojure http library ignores the socket timeout and connection timeout set when calling GET
  and raises an exception much sooner than it should."
  [endpoint]
  (run-async-with-timing client/get endpoint {:socket-timeout ten-minutes-in-millis :conn-timeout ten-minutes-in-millis}))
