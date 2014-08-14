(ns dar.assets.builders.copy
  (:require [dar.assets :as assets]
            [dar.assets.utils :as util]
            [clojure.java.io :as io]))

(defn copy [type]
  (fn [env]
    (doseq [pkg (:packages env)
            file (get pkg type)]
      (let [path (assets/resource-path pkg file)
            src (io/resource path)
            target (assets/target-file env path)]
        (when (util/outdate? target src)
          (util/cp src target))))
    env))
