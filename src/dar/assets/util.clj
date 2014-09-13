(ns dar.assets.util
  (:refer-clojure :exclude [read])
  (:require [clojure.java.io :as io]
            [clojure.edn :as edn]
            [clojure.string :as string])
  (:import (java.io File))
  (:import (java.nio.file Files LinkOption Path)))

(set! *warn-on-reflection* true)

(defn assets-edn-url [name]
  (io/resource (str name "/assets.edn")))

(defn read [name]
  (if-let [url (assets-edn-url name)]
    (try
      (merge (edn/read-string (slurp url))
        {:dir (io/resource name)
         :name name})
      (catch Throwable ex
        (throw (Exception. (str "Failed to read assets.edn from " name) ex))))
    (throw (Exception. (str name "/assets.edn not found on classpath")))))

(defn resource-path [pkg ^String path]
  (if (= (first path) \/)
    (subs path 1)
    (str (:name pkg) "/" path)))

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
        (find visited k) (recur
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

(defn mkdirs [file]
  (.. (io/as-file file)
    getCanonicalFile
    mkdirs))

(defn mkdirs-for [file]
  (.. (io/as-file file)
    getCanonicalFile
    getParentFile
    mkdirs))

(defn write [s out]
  (mkdirs-for out)
  (io/copy (str s) (io/as-file out)))

(defn cp [src out]
  (mkdirs-for out)
  (let [url (io/as-url src)]
    (if (= "file" (.getProtocol url))
      (io/copy (io/as-file src) (io/as-file out))
      (io/copy (slurp url) (io/as-file out)))))

(defn rmdir [dir]
  (let [file (io/as-file dir)
        path (.toPath file)]
    (when (Files/isDirectory path (into-array [LinkOption/NOFOLLOW_LINKS]))
      (doseq [item (.listFiles file)]
        (rmdir item)))
    (Files/deleteIfExists path)))

(defn last-modified [file]
  (let [url (io/as-url file)]
    (if (= "file" (.getProtocol url))
      (.lastModified (io/as-file url))
      0)))

(defn outdate? [out & sources]
  (let [mtime (last-modified out)]
    (boolean
      (some #(>= (last-modified %) mtime)
        sources))))

(defn join
  ([seg1 seg2]
   (cond
     (nil? seg1) seg2
     (= \/ (first seg2)) seg2
     :else (let [seg1 (if (= "/" seg1)
                        ""
                        (string/replace seg1 #"/$" ""))]
             (str seg1 "/" seg2))))
  ([seg1 seg2 seg3 & rest]
   (apply join (join seg1 seg2) seg3 rest)))

(defn fs-join [& segs]
  (.getPath ^java.io.File (apply io/file segs)))

(defn absolute? [^String path]
  (or
    (.contains path "://")
    (= \/ (first path))))
