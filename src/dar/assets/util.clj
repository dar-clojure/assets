(ns dar.assets.util
  (:require [clojure.java.io :as io])
  (:import (java.io File)))

(set! *warn-on-reflection* true)

(defn mkdirs-for [^File f]
  (.mkdirs (.getParentFile (.getCanonicalFile f))))

(defn write [^String s ^File out]
  (mkdirs-for out)
  (io/copy s out))

(defn cp [src ^File out]
  (mkdirs-for out)
  (let [url (io/as-url src)]
    (if (= "file" (.getProtocol url))
      (io/copy (io/as-file src) out)
      (io/copy (slurp url) out))))

(defn last-modified [file]
  (let [url (io/as-url file)]
    (if (= "file" (.getProtocol url))
      (.lastModified (io/as-file url))
      0)))

(defn outdate? [out & sources]
  (let [mtime (last-modified out)]
    (boolean (some #(>= (last-modified %) mtime)
                   sources))))

(defn topo-visit [dependencies visit post init col]
  (loop [visited {}
         ret init
         stack nil
         todo col]
    (if-let [k (first todo)]
      (cond
        (= (peek stack) k) (recur
                             visited
                             (post ret (get visited k))
                             (pop stack)
                             (next todo))
        (visited name) (recur
                         visited
                         ret
                         stack
                         (next todo))
        :else (let [v (visit k)]
                (recur
                  (assoc visited k v)
                  ret
                  (conj stack k)
                  (concat (dependencies v) todo))))
      ret)))
