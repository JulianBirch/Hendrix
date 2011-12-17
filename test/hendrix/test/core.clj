(ns hendrix.test.core
  (:require [hendrix.file])
  (:use [hendrix core command]
        [hendrix.test fakefiles capture]
        [clojure.test]
        [clojure.java.shell :only [sh]]))

(deftest compile-bootstrap
  (with-ffs fake-files
    [sh ignore]
    (let [{:keys [inputs all-inputs] :as b}
          (new-rule "assets/temp/primary.less"
                    "resources/public/site.css"
                    lessc
                    "assets/temp/*.less")]
      (is (-> inputs resolve-items first) "Inputs should have resolved")
      (comment (do  (println "INPUTS")
                     (println inputs)
                     (println (class inputs))
                     (println (= hendrix.test.fakefiles.FakeFile (class inputs)))))
      (comment (is (instance? hendrix.test.fakefiles.FakeFile inputs)
                    "inputs should have resolved to a FakeFile."))
      (is (instance? hendrix.file.DirectoryMatch all-inputs)
          "all-inputs should have resolved to a directory match.")
      (execute [b])
      (println (captures sh)))))
