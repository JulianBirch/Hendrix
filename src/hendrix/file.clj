(ns hendrix.file
  (:use [clojure.java.io]
        [clojure.core.match :only [match match-1]])
  (:import [java.io File]))

(defprotocol IFile
  (get-canonical-path [f])
  (get-files [f]))

(defrecord DirectoryMatch [path glob regex])

(extend File IFile
        {:get-canonical-path (fn [^File file] (.getCanonicalPath file))
         :get-files file-seq})

(defn get-current-directory [] (file "."))

(defn to-regex-component [char]
  (case char
    \* "[^\\/]+"
    \? "."
    \. "[.]"
    (\\ \/) "[\\/]"
    (str char)))

(defn to-regex-components [glob]
  (match-1 glob
           ([] :seq) []
           ([\* \* & r] :seq) (cons ".*" (to-regex-components r))
           ([c & r] :seq) (cons (to-regex-component c) (to-regex-components (seq r)))))

(defn to-regex-pattern [glob]
  (->> glob seq to-regex-components (apply str)))

(defn to-regex [glob]
  (-> glob to-regex-pattern re-pattern))

(defn create-temp-directory [prefix]
  (let [tmpDir (System/getProperty "java.io.tmpdir")
        prefix (str prefix "-" (System/currentTimeMillis) "-")
        to-file (fn [n] (file tmpDir (str prefix n)))
        result (-> (range 10)
                 (map to-file)
                 (filter #(.mkdir %))
                 first)]
    result))

(defn split-glob-internal [glob]
  (loop [[c & remaining :as glob] glob path [] file []]
    (case c
      \* [path (concat file glob)]
      (\\ \/) (recur remaining (concat path file [c]) [])
      nil [path file]
      (recur remaining path (conj file c)))))

(defn split-glob [glob]
  "Splits a glob pattern into a fixed directory part and a glob part.  Check the tests for its exact behaviour."
  (let [[path glob] (split-glob-internal glob)]
    {:path (apply str path)
     :glob (apply str glob)}))

(defn is-wildcarded [glob]
  (some #{\*} glob))

(defn new-directory-match [{:keys [path glob regex] :or {path "" glob "*"}}]
  (if-let [regex (or regex
                  (if (is-wildcarded glob) (to-regex glob)))]
    (DirectoryMatch.
     (file path)
     glob
     regex)
    (file (str path "/" glob)) ; thankfully, java is forgiving enough
                               ; that this works on windows
    ))

(defn project-files-match [glob]
  (let [{:keys [path glob]} (split-glob glob)]
    (new-directory-match {:glob glob :path path})))

(defn temp-directory-match [prefix glob]
  (new-directory-match {:path (create-temp-directory prefix)
                  :glob glob}))

(defn make-to-relative [path]
  (let [root-length (-> path get-canonical-path count)]
    (fn to-relative [f]
      (println f)
      (->> f
           get-canonical-path
           (drop root-length)
           str))))

(defn dotoprintln [f]
  (println f)
  f)

(defn get-matching-files [{:keys [path regex] :as exact}]
  (if path
    (let [to-relative (make-to-relative path)]
      (->> (get-files path)
           (map to-relative)
           dotoprintln
           (filter #(re-matches regex %))))
    (get-files exact)))
