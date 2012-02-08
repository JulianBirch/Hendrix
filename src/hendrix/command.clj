(ns hendrix.command
  (:use
   [clojure.java.shell :only [sh]]
   [hendrix.file :only [get-canonical-path]])
  (:import [java.util.regex Pattern]
           [java.io File]))

(defn tap [f result] (doto result f))

(defn log-output [text] (println text))

(defn log-error [text] (println text))

(defn file-exists [^String path ^String file]
  (.exists (File. path file)))

(defn is-windows []
  (= ";" File/pathSeparator))

(defn which
  ([command] (which command
                    (System/getenv "path")
                    File/pathSeparator ))
  ([command path separator]
     (let [items (.split path (Pattern/quote separator))]
       (println separator)
       (println items)
       (->> items
            (filter #(file-exists % command))
            first))))

(defn correct-file-name [command]
  (if (is-windows)
    (->> [".bat" ".exe" ""]
         (map #(str command %))
         (filter which)
         first)
    command))

(defn command [[c & args]]
  (let [
        _ (println "Wiki")
        _ (println File/pathSeparator)
        _ (println (is-windows))
        _ (println (correct-file-name c))
        _ (println args)
        result (apply sh (correct-file-name c) args)
        _ (println "Hello")
        _ (println result)
        _ (println "World")
        {:keys [out err exit]} result]
    (log-output "OUTPUT")
    (log-output out)
    (log-error "ERROR")
    (log-error err)
    exit))

(defn lessc [inputs output]
  (command (concat ["lessc"]
                   (map get-canonical-path inputs)
                   [(get-canonical-path output)])))
