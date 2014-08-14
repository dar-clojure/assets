(ns dar.assets
  (:refer-clojure :exclude [read])
  (:require [clojure.java.io :as io]
            [clojure.edn :as edn]))

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

(defn- visit-pkg [[visited list :as env] name]
  (if (visited name)
    env
    (let [pkg (read name)
          [v l] (reduce visit-pkg [(conj visited name) list] (:dependencies pkg))]
      [v (conj l pkg)])))

(defn packages [names]
  (second (reduce visit-pkg [#{} []] names)))

(defn resource-path [pkg ^String path]
  (if (= (first path) \/)
    (subs path 1)
    (str (:name pkg) "/" path)))

(defn resource [pkg ^String path]
  (io/resource (resource-path pkg path)))

(defn ^java.io.File target-file [env path]
  (io/file (:build-dir env) path))

(defn build [builders opts names]
  (let [env (assoc opts :packages (packages names))]
    (reduce #(%2 %1) env builders)))
