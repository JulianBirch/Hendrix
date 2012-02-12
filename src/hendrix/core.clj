(ns hendrix.core
  (:use [clojure.java.shell :only [sh]]
        [clojure.java.io :only [file copy delete-file]]
        [clojure.core.match :only [match match-1]]
        [clojure pprint])
  (:require [hendrix.file :as file])
  (:import [java.io File]
           [java.lang String]))

(defmulti last-updated class)
(defmulti resolve-items class)
(defmulti evaluate-rule class)

(defrecord FileRule [last-evaluated
                     inputs all-inputs
                     output action])

(defn to-source [f]
  (if (string? f) (file f) f))

(defn output-time [{:keys [output last-evaluated]}]
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

(defn new-merge-rule [& inputs]
  (file/->MergeRule inputs
                    (-> "hendrix" file/create-temp-directory file)))

(defn execute [& rules]
  (doseq [r rules] (evaluate-rule r)))

(defmethod resolve-items
  hendrix.file.DirectoryMatch
  [dm]
  (file/get-matching-files dm))

(defmethod resolve-items
  hendrix.file.MergeRule
  [mr]
  (filter file/file? (-> mr :output file-seq)))

(defmethod resolve-items
  nil
  [file]
  [])

(defn evaluate-file-rule
  [{:keys [action inputs all-inputs output last-evaluated] :as rule}]
  (let [inputs (resolve-items inputs)
        all-inputs (resolve-items all-inputs)
        ot (output-time rule)
        it (input-time inputs all-inputs)]
    (when (or (nil? ot) (> it ot))
      (action inputs output)
      (reset! last-evaluated it))))

(defmethod evaluate-rule
  FileRule
  [rule]
  (evaluate-file-rule rule))

(defmethod evaluate-rule
  hendrix.file.MergeRule
  [{:keys [inputs output] :as rule} ]
  (let [input-files (->> inputs
                         (map to-finder)
                         (mapcat resolve-items)
                         (group-by file/get-file-name)
                         (mapcat (fn unpick [[k [v]]] [k v]))
                         (apply hash-map))
        output-files (resolve-items output)
        copies (for [[file-name input-file] input-files
                     :let [output-file (rule file-name)]
                     :when (or (-> output-file file/file-exists not)
                               (> (last-updated input-file) (last-updated output-file)))]
                 (copy input-file output-file))
        deletes (for [o output-files
                      :when (-> o file/get-file-name input-files nil?)]
                  (delete-file o))]
    (dorun copies)
    (dorun deletes)))
; One of these days I should unify the "should update" rules between
; the two.  Maybe,  The generality might kill readability

(defmethod last-updated
  File
  [^File file]
  (if (file/file-exists file)
    (.lastModified file)))

(defmethod resolve-items
  File
  [^File file]
  (cond
   (-> file file/file-exists not)
   []

   (.isDirectory file)
   (.listFiles file)

   :else [file]))
; move across to standard record functions
; make execute take varargs
