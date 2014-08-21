(ns dar.assets.builders.css
  (:require [dar.assets :as assets]
            [dar.assets.util :as util]
            [clojure.java.io :as io]))

(defn build [env]
  (let [files (for [pkg (:packages env)
                    file (:css pkg)]
                (let [path (assets/resource-path pkg file)
                      src (io/resource path)
                      target (assets/target-file env path)]
                  (when (util/outdate? target src)
                    (util/cp src target)) ;; TODO: url rewriting
                  path))
        css (apply str
              (map #(str "@import \"/" % "\";\n")
                files))]
    (assoc env :css-out css)))
