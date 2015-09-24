(ns tileserverstresstest.core
  (:gen-class)
  (:require
    [clj-http.client :as client]
    [incanter.core :refer :all]
    [incanter.charts :refer :all]
    [incanter.stats :refer :all]
    [clojure.tools.logging :as log]))

(defn- nano-to-millis [nanos]
  (/ (double nanos) 1000000.0))

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


(defn graph-for [endpoint req-count]
  (let [responses (->> endpoint
                       (partial run-async client/get)
                       (run-times (if (string? req-count) (Integer/parseInt req-count) req-count))
                       (filter #(= 200 (:status %))))
        elapsed-times (map :elapsed-time responses)
        request-time (map :request-start-time responses)]
    (view (line-chart request-time elapsed-times :legend true :x-label "Request start time(ms)" :y-label "Request duration(ms)"))))

(defn -main
  "Application entry point"
  [& args]
  (do
    (log/info (str "args=" args))
    (apply (resolve (symbol (first args))) (rest args))))


(comment

  (def res @(run-async client/get "http://localhost:4003/api/tms/12/4036/2564.png"))
  (def res (run-times 100 (partial run-async client/get "http://localhost:4003/api/tms/12/4036/2564.png")))
  (def request-durations (map :elapsed-time (filter #(= 200 (:status %)) res)))
  (def request-start-times (map :start-time res))

  (use '(incanter core stats charts datasets))
  (view (line-chart (range 0 (count times)) times))
  (view (line-chart request-start-times request-durations))

  (graph-for "http://localhost:4003/api/tms/12/4036/2564.png" 100)

  (def args ["graph-for" "http://localhost:4003/api/tms/12/4036/2564.png" "100"])

  )