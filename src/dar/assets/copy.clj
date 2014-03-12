(ns dar.assets.copy
  (:require [dar.assets.utils :refer :all]))

(defn builder [type]
  (fn [env]
    (doseq [pkg (:packages env)
            file (get pkg type)]
      (let [url (get-url pkg file)
            p (resource-path pkg file)
            out (target env p)]
        (when (outdate? out url)
          (cp url out))))
    env))
