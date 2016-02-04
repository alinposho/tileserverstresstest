(ns uwsgi.smoke-tests
  (:require
    [request.util :refer :all]
    [clj-http.client :as client]))


(defn post-file
  "Posts the specified file to the specified endpoint"
  [endpoint file]
  (client/post endpoint
               {:multipart [["title" "Foo"]
                            ["Content/type" "text/plain"]
                            ["file" (clojure.java.io/file file)]]}))

(comment
  (post-file "http://192.168.200.4:9090" "resources/firehtdrants.zip")
  (client/get "http://192.168.200.4:9090/api/v1/layer")

  (client/get "http://192.168.200.4:9090/api/v1/layer/gis_ff8455cac51642b08e979b9bf41214b4.json")

  (map :request-time
       (get-async-times "http://192.168.200.4:9090/api/v1/layer/gis_ff8455cac51642b08e979b9bf41214b4.json" 20))
  (map :request-time
       (get-async-times "http://192.168.200.4:9090/api/v1/layer/gis_b6c42f9ee41b4c21a8bfaf51f646f1e8.json" 20))

  )
