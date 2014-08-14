(ns dar.assets.builders
  (:require [dar.assets.builders.css :as css]
            [dar.assets.builders.copy :as copy]
            [dar.assets.builders.cljs :as cljs]
            [dar.assets :as assets]))

(defn dev-build [main opts]
  (assets/build main
    [(copy/copy :files) css/build cljs/build]
    opts))

(dev-build "example_asset"
  {:build-dir "build"})
