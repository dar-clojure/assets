(ns dar.assets.css
  (:require [dar.assets.utils :refer :all]))

(defn build [env]
  (let [files (for [pkg (:packages env)
                    file (:styles pkg)]
                (let [url (get-url pkg file)
                      p (resource-path pkg file)
                      out (target env p)]
                  (when (outdate? out url)
                    (cp url out)) ;; TODO: url rewriting
                  p))
        css (apply str (map #(str "@import \"" % "\";\n") files))]
    (write css (target env "build.css"))
    env))
