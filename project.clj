(defproject spyscope "0.1.7-SNAPSHOT"
  :description "Trace-oriented debugging tools for Clojure(Script)"

  :url "http://github.com/dgrnbrg/spyscope"

  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :deploy-repositories  [["releases" :clojars]]

  :dependencies [[org.clojure/clojure "1.9.0"]
                 [org.clojure/clojurescript "1.10.238"]
                 [clj-time "0.14.3"]
                 [com.andrewmcveigh/cljs-time "0.5.2"]
                 [net.cgrand/macrovich "0.2.1"]
                 ;[mvxcvi/puget "1.0.2"]
                 [mvxcvi/puget "1.0.2" :exclusions [org.clojure/core.rrb-vector]]
                 [quantum/org.clojure.core.rrb-vector "0.0.12"]]

  :plugins [[lein-cljsbuild "1.1.7"]
            [lein-doo "0.1.10" :exclusions [org.clojure/clojurescript]]]

  :eval-in-leiningen true

  :profiles
  {:test {:dependencies [[lein-doo "0.1.10"]]}}

  :doo {:build "test"
        :source-paths ["src" "test"]
        :alias {:default [:chrome-headless]}}

  :cljsbuild
  {:builds
   [{:id "test"
     :source-paths ["src" "test"]
     :compiler
       {:output-dir "target/cljs/out"
        :output-to "target/cljs/tests.js"
        :main spyscope.test-runner
        :optimizations :whitespace}}]})

