(ns hendrix.core
  (:use [clojure.java.shell :only [sh]]
        [clojure.java.io :only [file]]
        [clojure.core.match :only [match match-1]])
  (:require [hendrix.file :as file])
  (:import [java.io File]
           [java.lang String]))

(defprotocol Source
  (last-updated [source]))

(defprotocol Finder
  (resolve-items [finder]))

(defprotocol Rule
  (evaluate [rule]))

(defrecord FileRule [last-evaluated
                     inputs all-inputs
                     output action])

(defn output-time [{:keys [last-evaluated]} output]
  (or (-> last-evaluated deref)
      (-> output last-updated)
      0))

(defn input-time [inputs all-inputs]
  (-> (or all-inputs inputs)
      (map last-updated)
      (reduce max)))

(defn execute-rule
  [{:keys [action inputs all-inputs output last-evaluated] :as rule}]
  (let [inputs (resolve-items inputs)
        all-inputs (resolve-items all-inputs)
        ot (output-time rule output)
        it (input-time inputs all-inputs)]
    (if (> it ot)
      (action inputs output)
      (reset! last-evaluated it))))

(defn to-finder [input]
  (cond
   (satisfies? Finder input) input
   (string? input) (file/new-directory-match { :glob input})

   )
  )
(defn new-rule
  ([{:keys [inputs all-inputs output action]}]
     (new-rule inputs output action all-inputs))
  ([inputs output action]
     (new-rule inputs output action nil))
  ([inputs output action all-inputs]
     (FileRule. (atom nil)
            (to-finder inputs)
            output
            action
            (to-finder all-inputs))))

(defn execute [rules]
  (doseq [r rules] (execute-rule r)))

(def processes (ref {}))

(def sleep-time 100) ; Sleep time for processes in milliseconds.

(defn execute-repeatedly [rules enabled]
  (when @enabled
    (execute rules)
    (Thread/sleep sleep-time) ; I promise this is the very last time I
                              ; rewrite circumspec's watch code 20111119
    (recur rules enabled)))

(defn start [rules]
  (let [enabled (atom true)]
    (dosync (when-not (get processes rules)
              (alter processes assoc rules enabled)
              (future execute-repeatedly rules enabled)))))

(defn finish [rules]
  (let [enabled (get processes rules)]
    (reset! enabled false)
    (dosync (alter processes dissoc rules))))

(extend File
  Source {:last-updated (fn [file] (.lastModified file))}
  Finder {:resolve-items (fn [file] (if (.exists file) [file] []))})

(extend hendrix.file.DirectoryMatch
  Finder {:resolve-items file/get-matching-files})

(extend nil
  Finder {:resolve-items (fn [file] [])})

; DEBUG ONLY

(comment  (def compile-bootstrap
            (new-rule "assets/temp/bootstrap/bootstrap.less"
                      "assets/temp/bootstrap/*.less"
                      "resources/public/site.css"
                      lessc)))
