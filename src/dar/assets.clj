(ns dar.assets
  (:refer-clojure :exclude [read])
  (:require [clojure.java.io :as io]
            [clojure.edn :as edn]
            [dar.assets.css :as css]
            [dar.assets.copy :as copy]
            [dar.assets.scripts :as scripts])
  (:import (java.lang Exception)))

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

(defn build
  ([names opts]
   (build names opts [scripts/build
                      css/build
                      (copy/builder :files)]))
  ([names opts builders]
  (let [env (assoc opts :packages (packages names))]
    (reduce #(%2 %1) env builders))))
