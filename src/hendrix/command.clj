(ns hendrix.command
  (:use
   [clojure.java.shell :only [sh]]
   [hendrix.file :only [get-canonical-path]]))

(defn tap [f result]
  (doto result f))

(defn command [args]
  (let [{:keys [out err exit]}
        (->> args
             (map get-canonical-path)
             (interpose " ")
             (tap println)
             sh)]
    (println "OUTPUT")
    (println out)
    (println "ERROR")
    (println err)
    exit))

(defn lessc [inputs output]
  (command (concat ["lessc"] inputs [output])))
