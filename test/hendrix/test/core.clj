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
      (is (instance? hendrix.file.DirectoryMatch all-inputs)
          "all-inputs should have resolved to a directory match.")
      (execute [b])
      (let [sh-args (-> sh captures first)]
        (is sh-args "Command should have been sent out.")
        (is (= ["lessc" "assets/temp/primary.less" "resources/public/site.css"]
               (first sh-args))
            "Incorrect command was sent out.")))))
