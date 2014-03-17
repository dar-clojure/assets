(ns dar.assets
  (:refer-clojure :exclude [read])
  (:require [clojure.java.io :as io]
            [clojure.edn :as edn]
            (dar.assets.builders
             [css :as css]
             [copy :as copy]
             [scripts :as scripts]))
  (:import (java.lang Exception)))

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
    (let [pkg (read name)]
      [(conj visited name) (conj list pkg)])))

(defn packages [names]
  (second (reduce visit-pkg [#{} []] names)))

(def std-builders [scripts/build
                   css/build
                   (copy/builder :files)])

(defn build
  ([names opts]
   (build names opts std-builders))
  ([names opts builders]
  (let [env (assoc opts :packages (packages names))]
    (reduce #(%2 %1) env builders))))
