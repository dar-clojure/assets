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

(defn packages [names]
  (loop [visited #{}
         ret nil
         todo names]
    (if-let [name (first todo)]
      (if (visited name)
        (recur visited ret (next todo))
        (let [pkg (read name)]
          (recur
            (conj visited name)
            (conj ret pkg)
            (concat (:dependencies pkg) (next todo)))))
      ret)))

(defn resource-path [pkg ^String path]
  (if (= (first path) \/)
    (subs path 1)
    (str (:name pkg) "/" path)))

(defn resource [pkg ^String path]
  (io/resource (resource-path pkg path)))

(defn ^java.io.File target-file [env path]
  (io/file (:build-dir env) path))

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
