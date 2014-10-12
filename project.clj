(defproject dar/assets "0.0.3"
  :description "Web resources management framework"
  :url "https://github.com/dar-clojure/assets"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/clojurescript "0.0-2371"]
                 [dar/container "0.2.0"]
                 [hiccup "1.0.5"]]
  :profiles {:dev {:source-paths ["examples"]}}
  :target-path "build/target"
  :clean-targets ["build"])
