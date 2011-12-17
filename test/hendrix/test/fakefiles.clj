(ns hendrix.test.fakefiles
  (:require [clojure.java.io])
  (:use [hendrix.core]
        [hendrix.file]
        [hendrix.test.capture]
        [clojure.test]))

(defn starts-with [[s & ss] [l & ls]]
  (or (nil? s)
      (and (= s l) (starts-with ss ls))))

(defn to-name [{:keys [name] :as file}]
  (or name file))

(defn name-starts-with [short long]
  (starts-with (to-name short) (to-name long)))

(def primary {:name "assets/temp/primary.less" :last-updated 2})
(def implicit {:name "assets/temp/implict.less" :last-updated 2})

(def fake-files [primary implicit
                 {:name "assets/temp/ignore.css" :last-updated 10}
                 {:name "assets/ignore.less" :last-updated 11}])

(defrecord FakeFile [name fake-files])

(defmethod resolve-items FakeFile
  [{:keys [name fake-files]}]
  (->> fake-files (filter #(name-starts-with name %))))

(defmethod last-updated FakeFile
  [{:keys [name fake-files]}]
  (->> fake-files
       (filter #(= name (:name %)))
       (map :last-updated)
       first))

(defmethod get-canonical-path clojure.lang.PersistentHashMap
  [f] (:name f))

(defmethod last-updated clojure.lang.PersistentHashMap
  [f] (:last-updated f))

(defn new-fake-file [file fake-files]
  (FakeFile. file fake-files))

(defn fake-file-function [fake-files file]
  {:pre (seq? fake-files)}
  (if (string? file)
    (new-fake-file file fake-files)
    file))

(defn fake-file-seq [files dir]
  (filter #(name-starts-with % dir) files))

(defn with-fake-file-system [files action]
  (with-redefs
    [clojure.java.io/file (partial fake-file-function files)
     file-seq (partial fake-file-seq files)
     get-canonical-path :name]
    (action)))

(defmacro with-ffs [files binding body]
  `(with-captures ~binding
     (with-fake-file-system ~files (fn [] ~body))))

(deftest test-fake-files
  (with-ffs fake-files []
    (is (-> "assets/temp/primary.less"
            clojure.java.io/file
            :fake-files
            empty?
            not)
        "Fake file should not have empty fake file list.")))
