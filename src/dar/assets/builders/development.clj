(ns dar.assets.builders.development
  (:require [dar.assets.builders.css :as css]
            [dar.assets.builders.copy :as copy]
            [dar.assets.builders.cljs :as cljs]))

(defn build [env]
  (-> env
    ((copy/copy :files))
    (css/build)
    (cljs/build)))
