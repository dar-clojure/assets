(ns dar.assets.utils
  (:require [clojure.java.io :as io])
  (:import (java.lang String)
           (java.io File)))

(defn get-url [pkg ^String file]
  (io/as-url (str (:dir pkg) "/" file)))

(defn resource-path [pkg ^String file]
  (str (:name pkg) "/" file))

(defn target [env path]
  (io/file (:out-dir env) path))

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
    (boolean (some #(> (last-modified %) mtime)
                   sources))))