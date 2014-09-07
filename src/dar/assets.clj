(ns dar.assets
  (:require [dar.container :refer :all]
            [dar.assets.util :as util]
            [dar.assets.cljs :as cljs]
            [clojure.java.io :as io]))

(application development)

(define :assets/main
  :doc "Name of the main package")

(define :assets/packages
  :doc "List of packages to build (in dependency order)"
  :args [:assets/main]
  :fn (fn [main]
        (util/topo-visit
          :dependencies
          util/read
          conj
          []
          [main])))

(define :assets/main-pkg
  :args [:assets/main]
  :fn (fn [main]
        (util/read main)))

(define :assets/build-dir
  :doc "A place to store build products")

(define :assets/public-dir
  :doc "A dir to store public files to be served by HTTP server"
  :args [:assets/build-dir]
  :fn identity)

(define :assets/prefix "")

(defn files [type packages]
  (for [pkg packages
        file (get pkg type)
        :let [path (util/resource-path pkg file)
              src (io/resource path)]]
    (when-not src
      (throw
        (Exception. (str type " file " file " not found in package " name))))
    {:path path
     :src src
     :pkg pkg}))

(defn- copy-file [{:keys [path src]} dir]
  (let [target (io/file dir path)]
    (when (util/outdate? target src)
      (util/cp src target))))

(define :css/links
  :doc "TODO: url rewriting"
  :args [:assets/packages :assets/public-dir]
  :fn (fn [packages dir]
        (mapv (fn [file]
                (copy-file file)
                [:link {:href (:path file)
                        :rel "stylesheet"
                        :type "text/css"}])
          (files :css packages))))

(define :files
  :args [:assets/packages :assets/public-dir]
  :fn (fn [packages dir]
        (doseq [file (files :files packages)]
          (copy-file file dir))))

(include cljs/development)

(define :cljs/main
  :args [:assets/main-pkg]
  :fn :main-ns)
