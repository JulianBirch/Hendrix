(ns hendrix.test.file
  (:require [clojure.java.io])
  (:use [hendrix.file]
        [clojure.test]))

(deftest glob-splitting
  (let [{:keys [path glob]} (split-glob "assets/temp/bootstrap.less")]
    (is (= "assets/temp/" path))
    (is (= "bootstrap.less" glob)))
  (let [{:keys [path glob]} (split-glob "assets/temp/*.less")]
    (is (= "assets/temp/" path))
    (is (= "*.less" glob)))
  (let [{:keys [path glob]} (split-glob "bootstrap.less")]
    (is (= "" path))
    (is (= "bootstrap.less" glob))))
            ; "assets/temp/bootstrap/*.less"

(deftest regex-generation
  (is (= (to-regex-pattern "*.less") "[^\\/]+[.]less"))
  )

(defn starts-with [[s & ss] [l & ls]]
  (or (nil? s)
      (and (= s l) (starts-with ss ls))))

(defn with-fake-file-system [files action]
  (with-redefs [clojure.java.io/file identity
                file-seq (fn [dir] (-> files (filter #(starts-with dir (:name %)))))
                ]
    (action)))

(comment  (deftest bootstrap
            (let [b (new-rule "assets/temp/bootstrap/bootstrap.less"
                              "assets/temp/bootstrap/*.less"
                              "resources/public/site.css"
                              lessc)]
              (execute rules)
              )))

(def primary {:name "assets/temp/primary.less" :last-updated 2})
(def implicit {:name "assets/temp/implict.less" :last-updated 2})

(def fake-files [primary implicit
                 {:name "assets/temp/ignore.css" :last-updated 10}
                 {:name "assets/ignore.less" :last-updated 11}])

(defn name-starts-with [{:keys [name]} path]
  (starts-with path name))

(extend java.lang.String IFile
        {:get-canonical-path identity
         :get-files (fn [f] (->> fake-files (filter #(name-starts-with % f))))})

(extend clojure.lang.PersistentHashMap IFile
        {:get-canonical-path :name})

(defn scan [glob]
  (with-fake-file-system
    fake-files
    #(-> glob
        (doto println)
        project-files-match
        (doto println)
        get-matching-files
        (doto println)
        seq
        set)))

(deftest globbing
  (is (= #{primary implicit}) (scan "assets/temp/*.less"))
  (is (= #{primary}) (scan "assets/temp/primary.less")))

(defn run [])
