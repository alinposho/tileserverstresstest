(ns tileserverstresstest.core
  (:gen-class)
  (:require
    [clj-http.client :as client]
    [incanter.core :refer :all]
    [incanter.charts :refer :all]
    [incanter.stats :refer :all]
    [clojure.tools.logging :as log]
    [request.util :refer :all]))


(defn generate-tile [base-endpoint start-lat end-lat start-long end-long]
  (for [lat (range start-lat end-lat)
        long (range start-long end-long)]
    (run-async client/get (str base-endpoint lat "/" long ".png"))))

(defn graph-for
  ([endpoint req-count]
   (let [responses (filter #(= 200 (:status %)) (get-async-times endpoint req-count))
         elapsed-times (map :elapsed-time responses)
         request-time (map :request-start-time responses)]
     (view (line-chart request-time elapsed-times :legend true :x-label "Request start time(ms)" :y-label "Request duration(ms)"))))
  ([{:keys [base-endpoint start-lat end-lat start-long end-long req-count]}]
   (let [results (map (fn [f] @f)
                      (flatten
                        (repeatedly req-count (partial generate-tile base-endpoint start-lat end-lat start-long end-long))))
         request-durations (map :elapsed-time results)
         request-start-times (map :request-start-time results)]
     (view (line-chart request-start-times request-durations :title "Different tiles")))))

(defn graph-for-urls [urls]
  (let [results (map (fn [f] @f) (map (partial run-async client/get) urls))
        request-durations (map :elapsed-time results)
        request-start-times (map :request-start-time results)]
    (view (line-chart request-start-times request-durations :legend true :x-label "Request start time(ms)" :y-label "Request duration(ms)"))))

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
  (def request-start-times (map :request-start-time res))

  (use '(incanter core stats charts datasets))
  (view (line-chart (range 0 (count times)) times))
  (view (line-chart request-start-times request-durations :title "Python Server"))

  (graph-for "http://localhost:4003/api/tms/12/4036/2564.png" 100)


  ;; Generate graph for a list of coordinates
  (def res
    (map (fn [f] @f)
         (flatten (repeatedly 10 (partial generate-tile "http://localhost:4003/api/tms/12/" 4036 4040 2564 2566)))))

  (def request-durations (map :elapsed-time (filter #(= 200 (:status %)) res)))
  (def request-start-times (map :request-start-time res))
  (view (line-chart request-start-times request-durations :title "Different tiles"))

  ;; And all in one function
  (graph-for {:base-endpoint "http://localhost:4003/api/tms/12/"
              :start-lat     4036
              :end-lat       4030
              :start-long    2564
              :end-long      2568
              :req-count     10})


  ;; Call tileserver for a list of endpoints loaded from a file
  #_(def urls (with-open [rdr (clojure.java.io/reader "resources/urls.txt")]
                (line-seq rdr)))
  (def urls (clojure.string/split-lines (slurp "resources/urls.txt")))

  (def results (map (fn [f] @f) (map (partial run-async client/get) urls)))
  (def request-durations (map :elapsed-time (filter #(= 200 (:status %)) results)))
  (def request-start-times (map :request-start-time results))
  (view (line-chart request-start-times request-durations :legend true :x-label "Request start time(ms)" :y-label "Request duration(ms)"))

  (graph-for-urls urls)


  ;; Post files to the shapefile importer web server URL
  (client/post "http://192.168.200.2:4002"
               {:multipart [["title" "Foo"]
                            ["Content/type" "text/plain"]
                            ["file" (clojure.java.io/file "resources/firehtdrants.zip")]]})

  (def res
    (map (fn [f] @f)
         (flatten (repeatedly 10 (partial generate-tile "http://localhost:4003/api/tms/12/" 4036 4040 2564 2566)))))

  (def request-durations (map :elapsed-time (filter #(= 200 (:status %)) res)))
  (def request-start-times (map :request-start-time res))
  (view (line-chart request-start-times request-durations :title "Different tiles"))

  )