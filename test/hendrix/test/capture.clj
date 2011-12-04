(ns hendrix.test.capture)

(defn ignore
  "Ignores the inputs and returns the outputs.  Useful as mock target."
  [& args] nil)

; Capture

(defn capture-invoke [{:keys [f captures]} args]
  (let [r (apply f args)]
    (swap! captures conj args)))

(defrecord capture-t [f captures]
  clojure.lang.IFn
  (invoke [this] (capture-invoke this []))
  (invoke [this a] (capture-invoke this [a]))
  (invoke [this a b] (capture-invoke this [a b]))
  (invoke [this a b c] (capture-invoke this [a b c]))
  (invoke [this a b c d] (capture-invoke this [a b c d]))
  (applyTo [this args]
    (clojure.lang.AFn/applyToHelper this args)))

(defn new-capture [f]
  (new capture-t f (atom [])))

(defn to-capture [[v f]]
  (new-capture (if (= f :v) (var-get v) f)))

(defn to-capture-map [h]
  (zipmap (keys h) (->> h (map to-capture))))

(defn captures [c]
  (-> c :captures deref))

(defn with-captures-fn [bindings action]
  "Like with-redefs-fn, only you can call 'captures' on the redefined functions."
  (-> bindings
      to-capture-map
      (with-redefs-fn action)))

; Code ripped off from with-redefs
(defmacro with-captures
  "Like with-redefs, only you can call 'captures' on the redefined functions."
  [bindings & body]
  `(with-captures-fn ~(zipmap (map #(list `var %) (take-nth 2 bindings))
                              (take-nth 2 (next bindings)))
     (fn [] ~@body)))

(defn add-two [x] (+ x 2))

(defn example []
  (with-captures [identity :v
                  add-two ignore]
    (identity 3)
    (identity 6)
    (add-two 7)
    {:add-two (captures add-two)
     :identity (captures identity)}))
