(ns dar.assets
  (:require [dar.container :refer :all]
            [dar.assets.util :as util]
            [dar.assets.cljs :as cljs]
            [clojure.java.io :as io]
            [clojure.string :as string]
            [hiccup.core :as hiccup]
            [hiccup.page :refer [html5]]))

(set! *warn-on-reflection* true)

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

(defn copy-file [{:keys [path src]} dir]
  (let [target (io/file dir path)]
    (when (util/outdate? target src)
      (util/cp src target))))

(defn- trim-quotes [[q :as ^String s]]
  (if (or (= \' q) (= \" q))
    (.substring s 1 (dec (.length s)))
    s))

(defn css-url-rewrite [file url-prefix]
  (let [css (slurp (:src file))]
    (string/replace css #"\burl *\(([^)]+)\)"
      (fn [[_ url]]
        (let [^String url (trim-quotes url)]
          (str
            "url('"
            (cond
              (.contains url "data:") url
              (= "//" (.substring url 2)) (util/join url-prefix (.substring url 2))
              (util/absolute? url) url
              :else (util/join url-prefix (:path file) ".." url))
            "')"))))))

(define :css/links
  :doc "Builds Css and returns a list of link tags to include in HTML page"
  :args [:assets/packages :assets/public-dir :assets/public-url]
  :fn (fn [packages dir prefix]
        (mapv (fn [file]
                (let [target (io/file dir (:path file))]
                  (when (util/outdate? target (:src file))
                    (util/write (css-url-rewrite file prefix) target)))
                [:link {:href (util/join prefix (:path file))
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
  :fn #(-> % :page :main-ns))

(define :page
  :pre [:files]
  :args [:css/links :cljs/scripts :cljs/main-script :page/body :page/title :page/head]
  :fn (fn [css scripts main body title head]
        (hiccup/html
          (html5
            [:html
             [:head
              [:title title]
              head
              (seq css)
              (seq scripts)]
             [:body body main]]))))

(define :page/title
  :args [:assets/main-pkg]
  :fn (fn [pkg]
        (or (-> pkg :page :title) (-> pkg :name))))

(defn- to-html [html pkg]
  (cond
    (nil? html) nil
    (string? html) (if-let [url (io/resource (util/resource-path pkg html))]
                     (slurp url)
                     (throw
                       (Exception.
                         (str "Html file " html " not found in package " (:name pkg)))))
    :else (hiccup/html html)))

(define :page/body
  :args [:assets/main-pkg]
  :fn (fn [pkg]
        (to-html (-> pkg :page :body) pkg)))

(define :page/head
  :args [:assets/main-pkg]
  :fn (fn [pkg]
        (to-html (-> pkg :page :head) pkg)))

(application production)

(include development)

(include cljs/production-patch)

(define :assets/public-url nil)

(define :assets/public-dir
  :args [:assets/build-dir]
  :fn (fn [dir]
        (util/fs-join dir "out")))

(define :cljs/build-dir
  :args [:assets/build-dir]
  :fn (fn [dir]
        (util/fs-join dir "cljs")))

(define :css/links
  :args [:assets/packages :assets/public-url]
  :fn (fn [packages url]
        (mapv (fn [file]
                [:style (css-url-rewrite file url)])
          (files :css packages))))

(define :index.html
  :args [:page :assets/public-dir]
  :fn (fn [html dir]
        (util/write html (io/file dir "index.html"))))

(defn build
  ([main build-dir]
   (<?!evaluate (start production {:assets/main main
                                   :assets/build-dir build-dir})
