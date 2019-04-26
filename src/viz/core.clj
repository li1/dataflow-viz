(ns viz.core
  (:gen-class)
  (:require [loom.graph :as g]
            [loom.io :as v]
            [loom.label :as l]
            [clojure.edn :as edn]))

(defn- read-one
  [r]
  (try
    (read r)
    (catch java.lang.RuntimeException e
      (if (= "EOF while reading" (.getMessage e))
        ::EOF
        (throw e)))))

(defn read-seq-from-file
  "Reads a sequence of top-level objects in file at path."
  [path]
  (with-open [r (java.io.PushbackReader. (clojure.java.io/reader path))]
    (binding [*read-eval* false]
      (doall (take-while #(not= ::EOF %) (repeatedly #(read-one r)))))))

(defn create-nodes [g nodes]
  (reduce
   (fn [graph node]
     (l/add-labeled-nodes graph (:addr node) (str (:id node) ", " (:name node))))
   g nodes))

(defn create-edges [g edges]
  (reduce
   (fn [graph edge]
     (l/add-labeled-edges
      graph
      [(:src edge) (:target edge)]
      (:id edge)))
   g edges))

(defn -main [& args]
  (let [ops-file (or (first args) "ops.edn")
        ch-file  (or (second args) "chs.edn")]
    (println "creating graph...")
    (-> (g/digraph)
        (create-nodes (read-seq-from-file ops-file))
        (create-edges (read-seq-from-file ch-file))
        (v/view))
    (println "done.")))
