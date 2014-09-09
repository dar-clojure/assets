(ns dar.assets.cljs
  (:refer-clojure :exclude [compile])
  (:require [cljs.closure :as closure]
            [cljs.env :as cljs-env]
            [clojure.string :as string]
            [clojure.java.io :as io]
            [dar.container :refer :all]
            [dar.assets.util :as util]))

(defn ns->path [ns]
  (string/replace (namespace-munge ns) \. \/))

(defn- compile-js [path opts]
  (when-let [js (io/resource (str path ".js"))]
    (let [target (io/file (:output-dir opts) (str path ".js"))]
      (get-in (swap! (::env opts) update-in [::cache path]
                (fn [script]
                  (if (or
                        (when (util/outdate? target js)
                          (util/cp js target)
                          true)
                        (not script))
                    (closure/read-js target)
                    script)))
        [::cache path]))))

(defn- compile-clj [path opts]
  (when-let [clj (io/resource (str path ".cljs"))]
    (closure/-compile clj
      (assoc opts :output-file (str path ".js")))))

(defn- compile-ns [ns opts]
  (let [path (ns->path ns)]
    (or
      (compile-js path opts)
      (compile-clj path opts))))

(defn compile [namespaces opts]
  (util/topo-visit
    :requires
    #(compile-ns % opts)
    (fn [scripts js]
      (if js
        (conj scripts js)
        scripts))
    []
    namespaces))

(application development)

(define :cljs/options nil)

(define ::env
  :level :env
  :fn #(cljs-env/default-compiler-env))

(define :cljs/build-dir
  :args [:assets/public-dir]
  :fn identity)

(define ::default-options {:optimizations :none
                           :source-map true})

(define ::opts
  :args [:cljs/build-dir :cljs/options ::default-options]
  :fn (fn [dir opts defaults]
        (merge
          defaults
          opts
          {:output-dir dir})))

(define ::build
  :args [::opts ::env :cljs/main]
  :fn (fn [opts env main]
        (closure/build (reify closure/Compilable
                         (-compile [_ opts] (compile [main] opts)))
          (assoc opts ::env env)
          env)))

(define :cljs/main-script
  :args [:cljs/main]
  :fn (fn [main]
        (when main
          [:script (str (namespace-munge (str main)) "._main()")])))

(define ::scripts
  :args [::build :cljs/main :assets/public-url]
  :fn (fn [deps-js main prefix]
        [[:script {:src (util/join prefix "goog/base.js")}]
         [:script deps-js]
         [:script (str "goog.require('" (namespace-munge (name main)) "')")]]))

(define :cljs/scripts
  :args [:cljs/main :dar.container/self]
  :fn (fn [main app]
        (when main
          (evaluate app ::scripts))))

(application production-patch)

(define :cljs/build-dir)

(define ::default-options {:optimizations :advanced})

(define ::scripts
  :args [::build]
  :fn (fn [js]
        [[:script js]]))
