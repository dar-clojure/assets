(defproject dar/assets "0.0.1-SNAPSHOT"
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/clojurescript "0.0-2322"]
                 [dar/container "0.2.0"]
                 [hiccup "1.0.5"]]
  :profiles {:dev {:source-paths ["examples"]}}
  :target-path "build/target"
  :clean-targets ["build"])
