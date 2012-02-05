(ns hendrix.core
  (:use [clojure.java.shell :only [sh]]
        [clojure.java.io :only [file]]
        [clojure.core.match :only [match match-1]])
  (:require [hendrix.file :as file])
  (:import [java.io File]
           [java.lang String]))

(defmulti last-updated class)
(defmulti resolve-items class)
(defmulti evaluate class)

(defrecord FileRule [last-evaluated
                     inputs all-inputs
                     output action])

(defn to-source [f]
  (if (string? f) (file f) f))

(defn output-time [{:keys [last-evaluated]} output]
  (or (-> last-evaluated deref)
      (-> output to-source last-updated)
      nil))

(defn nice-max [coll]
  (if (second coll)
    (apply max coll)
    (first coll)))

(defn input-time [inputs all-inputs]
  (->> (if (and all-inputs (-> all-inputs empty? not))
         all-inputs
         inputs)
       (map last-updated)
       nice-max))

(defn execute-rule
  [{:keys [action inputs all-inputs output last-evaluated] :as rule}]
  (let [inputs (resolve-items inputs)
        all-inputs (resolve-items all-inputs)
        ot (output-time rule output)
        it (input-time inputs all-inputs)
        _ (println (str "IT" it "OT" ot))
        ]
    (when (or (nil? ot) (< it ot))
      (action inputs output)
      (reset! last-evaluated it))))

(defn to-finder [input]
  (if (string? input)
    (file/new-directory-match {:glob input})
    input))

(defn new-rule
  ([{:keys [inputs all-inputs output action]}]
     (new-rule inputs output action all-inputs))
  ([inputs output action]
     (new-rule inputs output action nil))
  ([inputs output action all-inputs]
     (FileRule. (atom nil)
            (to-finder inputs)
            (to-finder all-inputs)
            output
            action)))

(defn execute [rules]
  (doseq [r rules] (execute-rule r)))

(defmethod last-updated
  File
  [^File file]
  (.lastModified file))

(defmethod resolve-items
  File
  [^File file]
  (if (.exists file) [file] []))

(defmethod resolve-items
  hendrix.file.DirectoryMatch
  [dm]
  (file/get-matching-files dm))

(defmethod resolve-items
  nil
  [file]
  [])

; DEBUG ONLY

(comment  (def compile-bootstrap
            (new-rule "assets/temp/bootstrap/bootstrap.less"
                      "assets/temp/bootstrap/*.less"
                      "resources/public/site.css"
                      lessc)))
