(ns dar.assets
  (:refer-clojure :exclude [read])
  (:require [clojure.java.io :as io]
            [clojure.edn :as edn])
  (:import (java.net URL)
           (java.io File)
           (java.lang String Exception)))

;;
;; Build engine
;;

(defn read [name]
  (if-let [url (io/resource (str name "/assets.edn"))]
    (try
      (merge (edn/read-string (slurp url))
             {:dir (io/resource name)
              :name name})
      (catch Throwable ex
        (throw (ex-info (str "Failed to read assets.edn from " name)
                        {::url url}
                        ex))))
    (throw (Exception. (str name "/assets.edn not found on classpath")))))

(defn- visit-pkg [[visited list :as env] name]
  (if (visited name)
    env
    (let [pkg (read name)]
      [(conj visited name) (conj list pkg)])))

(defn packages [names]
  (second (reduce visit-pkg [#{} []] names)))

(declare copy) ;; default builders

(defn build
  ([names opts]
   (build names opts [(copy :files)]))
  ([names opts builders]
  (let [env (assoc opts :packages (packages names))]
    (reduce #(%2 %1) env builders))))

;;
;; Utils
;;

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

;;
;; Builders
;;

(defn copy [type]
  (fn [env]
    (doseq [pkg (:packages env)
            file (get pkg type)]
      (let [url (get-url pkg file)
            p (resource-path pkg file)
            out (target env p)]
        (when (outdate? out url)
          (cp url out))))
    env))
