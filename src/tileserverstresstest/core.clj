(ns tileserverstresstest.core
  (:gen-class)
  (:require
    [clj-http.client :as client]
    [clojure.data.json :as json]))

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
        (assoc res :elapsed-time (/ (double (- (. System (nanoTime)) start)) 1000000.0)))
      (catch Throwable e
        {:status 500, :exception e}))))

(defn run-times [n fnc]
  (let [results (for [_ (range 0 n)] (fnc))]
    (map (fn [f] @f) results)))

(defn -main
  "Application entry point"
  [fct & args]
  (apply (resolve (symbol fct)) args))


(comment

  (def res @(run-async client/get "http://localhost:4003/api/tms/12/4036/2564.png"))
  (def res (run-times 1000 (partial run-async client/get "http://localhost:4003/api/tms/12/4036/2564.png")))
  (def times (map :elapsed-time (filter #(= 200 (:status %)) res)))

  (use '(incanter core stats charts datasets))
  (view (line-chart (range 0 (count times)) times))

  )