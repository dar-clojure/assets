(ns dar.assets.builders.cljs
  (:refer-clojure :exclude [compile])
  (:require [cljs.closure :as closure]
            [cljs.env :as cljs-env]
            [clojure.string :as string]
            [clojure.java.io :as io]
            [dar.assets :as assets]
            [dar.assets.utils :as util]))

(def ^:dynamic *compiler-env* (cljs-env/default-compiler-env))

(defn ns->path [ns]
  (string/replace (namespace-munge ns) \. \/))

(defn path->ns [path]
  (-> path
      (string/replace #"\.[^\.]+$" "")
      (string/replace \/ \.)
      symbol))

(defn- compile-ns [ns opts]
  (let [path (ns->path ns)
        target (io/file (:output-dir opts) (str path ".js"))]
    (if-let [js (io/resource (str path ".js"))]
      (do
        (when (util/outdate? target js)
          (util/cp js target)
          (swap! *compiler-env* assoc-in [::cache path] (closure/read-js target)))
        (get-in @*compiler-env* [::cache path]))
      (when-let [clj (io/resource (str path ".cljs"))]
        (closure/compile-file
          (io/file clj)
          (assoc opts :output-file (str path ".js")))))))

(defn compile [namespaces opts]
  (loop [visited #{}
         scripts ()
         todo namespaces]
    (if-let [ns (first todo)]
      (if (visited ns)
        (recur visited scripts (next todo))
        (if-let [js (compile-ns ns opts)]
          (recur
            (conj visited ns)
            (conj scripts js)
            (concat (:requires js) (next todo)))
          (recur
            (conj visited ns)
            scripts
            (next todo))))
      scripts)))

(defn build [env]
  (if-let [main (-> env :main :main-ns str)]
    (let [out (closure/build
                (reify closure/Compilable
                  (-compile
                    [_ opts]
                    (compile [main] opts)))
                {:source-map true
                 :optimizations :none
                 :warnings false
                 :output-dir (:build-dir env)}
                *compiler-env*)]
      (assoc env :js (str out
                       "\ngoog.require('" (namespace-munge main) "');\n"
                       (namespace-munge main) "._main();\n")))
    env))
