(ns dar.assets
  (:refer-clojure :exclude [read])
  (:require [clojure.java.io :as io]
            [clojure.edn :as edn]
            [dar.assets.util :as util]))

(defn assets-edn-url [name]
  (io/resource (str name "/assets.edn")))

(defn read [name]
  (if-let [url (assets-edn-url name)]
    (try
      (merge (edn/read-string (slurp url))
             {:dir (io/resource name)
              :name name})
      (catch Throwable ex
        (throw (ex-info (str "Failed to read assets.edn from " name)
                        {::url url}
                        ex))))
    (throw (Exception. (str name "/assets.edn not found on classpath")))))

(defn packages [roots]
  (util/topo-visit
    :dependencies
    read
    conj
    []
    roots))

(defn resource-path [pkg ^String path]
  (if (= (first path) \/)
    (subs path 1)
    (str (:name pkg) "/" path)))

(defn resource [pkg ^String path]
  (io/resource (resource-path pkg path)))

(defn ^java.io.File target-file [env path]
  (io/file (:build-dir env) path))

(defn delete-build-dir [env]
  (util/rmdir (:build-dir env)))

(defn build [main builders opts]
  (let [pkg (read main)
        env (assoc opts
              :main pkg
              :packages (packages
                          (concat
                            (:pre-include opts)
                            [main]
                            (:post-include opts))))]
    (reduce #(%2 %1) env builders)))
