(ns hendrix.test.file
  ; (:require [clojure.java.io])
  (:use [hendrix.file]
        [hendrix.test.fakefiles]
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

(deftest regex-generation
  (let [dotless-pattern (to-regex-pattern "x/*.less")
        dotless (to-regex "x/*.less")]
    (is (= dotless-pattern "x[/\\\\\\\\][^/\\\\\\\\]+[.]less"))
    (is (re-matches dotless "x/bootstrap.less"))
    (is (re-matches dotless "x\\bootstrap.less"))))

(defn scan [glob]
  (with-fake-file-system
    fake-files
    #(-> glob
         project-files-match
         get-matching-files
         set)))

(deftest globbing
  (is (= #{primary implicit}) (scan "assets/temp/*.less"))
  (is (= #{primary}) (scan "assets/temp/primary.less")))
