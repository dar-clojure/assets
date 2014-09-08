(ns dar.assets
  (:require [dar.container :refer :all]
            [dar.assets.util :as util]
            [dar.assets.cljs :as cljs]
            [clojure.java.io :as io]
            [hiccup.core :as hiccup]
            [hiccup.page :refer [html5]]))

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
  :doc
  "A dir to store public files to be served by HTTP server"
  :args [:assets/build-dir]
  :fn identity)

(define :assets/public-url "/"
  :doc "URL under which public dir will be served")

(defn files [type packages]
  (for [pkg packages
        file (get pkg type)
        :let [path (util/resource-path pkg file)
              src (io/resource path)]]
    (do
      (when-not src
        (throw
          (Exception. (str type " file " file " not found in package " name))))
      {:path path
       :src src
       :pkg pkg})))

(defn- copy-file [{:keys [path src]} dir]
  (let [target (io/file dir path)]
    (when (util/outdate? target src)
      (util/cp src target))))

(define :css/links
  :doc "Builds Css and returns a list of link tags to include in HTML page"
  :args [:assets/packages :assets/public-dir]
  :fn (fn [packages dir]
        (mapv (fn [file]
                (copy-file file dir)
                [:link {:href (:path file)
                        :rel "stylesheet"
                        :type "text/css"}])
          (files :css packages))))

(define :files
  :doc "Copies :files to public-dir"
  :args [:assets/packages :assets/public-dir]
  :fn (fn [packages dir]
        (doseq [file (files :files packages)]
          (copy-file file dir))))

(include cljs/development)

(define :cljs/main
  :args [:assets/main-pkg]
  :fn :main-ns)

(define :page
  :args [:css/links :cljs/scripts :cljs/main-script :page/content :page/title]
  :fn (fn [css scripts main content title]
        (hiccup/html
          (html5
            [:html
             [:head
              [:title title]
              (seq css)
              (seq scripts)]
             [:body content main]]))))

(define :page/title
  :args [:assets/main]
  :fn identity)

(define :page/content
  :args [:assets/main-pkg]
  :fn (fn [{html :main-html :as pkg}]
        (cond
          (nil? html) nil
          (string? html) (if-let [url (io/resource (util/resource-path pkg html))]
                           (slurp url)
                           (throw
                             (Exception.
                               (str "Html file " html " not found in package " (:name pkg)))))
          :else (hiccup/html html))))

(define :page/file
  :args [:page :assets/main :assets/public-dir]
  :fn (fn [html main dir]
        (util/write html (io/file dir main "index.html"))))

(define :build
  :pre [:page/file]
  :fn noop)

(application production)

(include development)

(include cljs/production-patch)

(define :assets/public-dir
  :args [:assets/build-dir]
  :fn (fn [dir]
        (util/fs-join dir "pages")))

(define :cljs/build-dir
  :args [:assets/build-dir]
  :fn (fn [dir]
        (util/fs-join dir "cljs")))

(defn build
  ([opts]
   (build production opts))
  ([app opts]
   (build app :build opts))
  ([app what opts]
   (let [ret (evaluate (start app opts) what)]
     (when (instance? Throwable ret)
       (throw ret))
     ret)))
