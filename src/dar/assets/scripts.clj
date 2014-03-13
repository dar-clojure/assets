(ns dar.assets.scripts
  (:require [cljs.closure :as cljs]
            [cljs.env :as cljs-env]
            [clojure.string :as string]
            [clojure.java.io :as io]
            [dar.assets.utils :refer :all]))

;; TODO: write our own ClojureScript compiler from scratch

(def ^:dynamic *compiler-env* (cljs-env/default-compiler-env))

(defn ns->path [ns]
  (string/replace (namespace-munge ns) \. \/))

(defn path->ns [path]
  (-> path
      (string/replace #"\.[^\.]+$" "")
      (string/replace \/ \.)
      symbol))

(defn ns-src-lookup [ns]
  (let [path (ns->path ns)
        names (map #(str path %) [".cljs" ".clj"])]
    (some identity (map io/resource names))))

(def ^:private ^:dynamic *opts* nil)

(defn- compile-ns [ns]
  (if-let [url (ns-src-lookup ns)]
    (let [out (str (ns->path ns) ".js")]
      (cljs/-compile url (assoc *opts* :output-file out)))))

(defn- do-compile [[visited scripts :as env] ns js]
  (if (visited ns)
    env
    (let [visited (conj visited ns)]
      (if js
        [visited (cons {:provides [ns] :url (io/as-url js)} scripts)]
        (if-let [js (compile-ns ns)]
          (reduce #(do-compile %1 %2 nil)
                  [visited (cons js scripts)]
                  (cljs/-requires js))
          [visited scripts])))))

(defn- compile-package [acc env pkg]
  (as-> acc &
        (reduce (fn [acc file]
                  (let [url (get-url pkg file)
                        p (resource-path pkg file)
                        out (target env p)]
                    (when (outdate? out url)
                      (cp url out))
                    (do-compile acc (path->ns p) out)))
                &
                (:js pkg))
        (reduce #(do-compile %1 %2 nil)
                &
                (:cljs pkg))))

(defn build [env]
  (let [out (cljs/build
             (reify cljs/Compilable
               (-compile
                [_ opts]
                (binding [*opts* opts]
                  (second (reduce #(compile-package %1 env %2)
                                  [#{} ()]
                                  (:packages env))))))
             {:source-map true
              :optimizations :none
              :warnings false
              :output-dir (:out-dir env)}
             *compiler-env*)]
    (assoc env :js (if-let [main (:main-ns env)]
                     (str out "\ngoog.require('" (namespace-munge main) "');\n")
                     out))))
