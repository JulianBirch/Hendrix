(ns hendrix.command)

(defn tap [f result]
  (doto result f))


(defn command [args]
  (let [{:keys [out err exit]} (->> args (interpose " ") (tap println) sh)]
    (println "OUTPUT")
    (println out)
    (println "ERROR")
    (println err)
    exit))

(defn lessc [inputs output]
  (command (concat ["lessc"] inputs [output])))
