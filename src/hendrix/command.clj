(ns hendrix.command
  (:use
   [clojure.java.shell :only [sh]]
   [hendrix.file :only [get-canonical-path]]))

(defn tap [f result]
  (doto result f))

(defn log-output [text] (println text))

(defn log-error [text] (println text))

(defn command [args]
  (let [{:keys [out err exit]} (sh args)]
    (log-output "OUTPUT")
    (log-output out)
    (log-error "ERROR")
    (log-error err)
    exit))

(defn lessc [inputs output]
  (command (concat ["lessc"]
                   (map get-canonical-path inputs)
                   [(get-canonical-path output)])))
