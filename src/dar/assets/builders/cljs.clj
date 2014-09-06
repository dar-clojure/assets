(ns dar.assets.builders.cljs
  (:refer-clojure :exclude [compile])
  (:require [cljs.closure :as closure]
            [cljs.env :as cljs-env]
            [clojure.string :as string]
            [clojure.java.io :as io]
            [dar.assets :as assets]
            [dar.assets.util :as util]))

(def ^:dynamic *compiler-env* (cljs-env/default-compiler-env))

(defn clear-env []
  (alter-var-root #'*compiler-env*
    (fn [_]
      (cljs-env/default-compiler-env))))

(defn ns->path [ns]
  (string/replace (namespace-munge ns) \. \/))

(defn- compile-js [path opts]
  (when-let [js (io/resource (str path ".js"))]
    (let [target (io/file (:output-dir opts) (str path ".js"))]
      (get-in (swap! *compiler-env* update-in [::cache path]
                (fn [script]
                  (if (or
                        (when (util/outdate? target js)
                          (util/cp js target)
                          true)
                        (not script))
                    (closure/read-js target)
                    script)))
        [::cache path]))))

(defn- compile-ns [ns opts]
  (let [path (ns->path ns)]
    (or
      (compile-js path opts)
      (when-let [clj (io/resource (str path ".cljs"))]
        (closure/-compile clj
          (assoc opts :output-file (str path ".js")))))))

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

(defn build [env]
  (if-let [main (-> env :main :main-ns str)]
    (let [out (closure/build
                (reify closure/Compilable
                  (-compile
                    [_ opts]
                    (compile [main] opts)))
                (assoc (:cljs env)
                  :output-dir (:build-dir env))
                *compiler-env*)]
      (update-in env [:build :cljs] assoc
        :js out
        :require-call (str "goog.require('" (namespace-munge main) "');")
        :main-call (str (namespace-munge main) "._main();\n")))
    env))

(defn set-development-options [env]
  (update-in env [:cljs] (partial merge {:source-map true
                                         :optimizations :none
                                         :warnings false})))

(defn set-production-options [env]
  (update-in env [:cljs] (partial merge {:optimizations :advanced
                                         :warnings true})))
