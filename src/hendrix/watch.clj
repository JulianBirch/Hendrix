(ns hendrix.watch)

(def processes (ref {}))

(def sleep-time 1000) ; Sleep time for processes in milliseconds.

(defn execute-repeatedly [command enabled]
  (when @enabled
    (command)
    (Thread/sleep sleep-time) ; I promise this is the very last time I
                              ; rewrite circumspec's watch code 20111119
    (recur command enabled)))

(defn start [command]
  (let [enabled (atom true)]
    (dosync (when-not (get processes command)
              (alter processes assoc command enabled)
              (future (execute-repeatedly command enabled))))))

(defn finish [command]
  (let [enabled (get processes command)]
    (reset! enabled false)
    (dosync (alter processes dissoc command))))
