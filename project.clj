(defproject tileserverstresstest "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [clj-http "1.1.2"]
                 [org.clojure/data.json "0.2.4"]
                 [incanter "1.5.6"]]
  :main ^:skip-aot tileserverstresstest.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
