(ns grafter.pipeline-test
  (:refer-clojure :exclude [ns-name])
  (:require [grafter.pipeline :refer :all]
            [clojure.test :refer :all]
            [clojure.java.io :as io])
  (:import [grafter.pipeline Pipeline]
           [java.io File]))


(deftest ns-name-test
  (is (= 'my.grafter.pipeline (ns-name '(ns my.grafter.pipeline
                                          (:require [foo.bar :as foo]))))))

(def test-graft-form (->Pipeline 'foo.bar
                                 'my-graft
                                 nil ; args
                                 "My docstring"
                                 nil ; meta
                                 `(comp foo.bar/make-graph foo.bar/pipe)
                                 :graft))

(deftest form->Pipeline-test
  (let [namespace 'foo.bar
        ->Pipeline (partial ->Pipeline namespace)
        pipe-form->Pipeline (partial pipe-form->Pipeline namespace)]
    (testing "with defpipe"
      (is (= (->Pipeline 'my-pipeline
                         '[a b c]
                         "My Docstring"
                         {:meta true :doc "My Docstring"}
                         '((println "hello world"))
                         :pipe)

             (pipe-form->Pipeline
              '(defpipe my-pipeline
                 "My Docstring"
                 {:meta true}
                 [a b c]
                 (println "hello world"))))

          "Parses fully specified pipeline (function) definition")

      (is (= (->Pipeline 'my-pipeline
                         '[a b c]
                         "My Docstring"
                         {:meta true :doc "My Docstring"}
                         '((println "hello")
                           (println "world"))
                         :pipe)

             (pipe-form->Pipeline
              '(defpipe my-pipeline
                 "My Docstring"
                 {:meta true}
                 [a b c]
                 (println "hello")
                 (println "world"))))
          "Parses implicit do in body")

      (is (= (->Pipeline 'my-pipeline
                         '[a b c]
                         "My Docstring"
                         {:doc "My Docstring"}
                         '((println "hello world"))
                         :pipe)

             (pipe-form->Pipeline
              '(defpipe my-pipeline
                 "My Docstring"
                 [a b c]
                 (println "hello world"))))
          "Parses when no metadata is supplied")

      (is (= (->Pipeline 'my-pipeline
                         '[a b c]
                         nil
                         {}
                         '((println "hello world"))
                         :pipe)

             (pipe-form->Pipeline
              '(defpipe my-pipeline [a b c]
                 (println "hello world"))))
          "Parses when no metadata or docstring are supplied"))

    (testing "with defgraft"
      (is (= test-graft-form
             (graft-form->Pipeline namespace '(defgraft my-graft "My docstring" foo.bar/pipe foo.bar/make-graph)))
          "with docstring")

      (is (= (assoc test-graft-form
                    :doc
                    "Calls foo.bar/pipe and transforms data into graph data by calling foo.bar/make-graph")
             (graft-form->Pipeline namespace '(defgraft my-graft foo.bar/pipe foo.bar/make-graph)))
          "without docstring")

      (testing "with docstring and many graphfns"
        (is (= (assoc test-graft-form
                      :body '(clojure.core/comp another/filter-function a/filter-function foo.bar/make-graph foo.bar/pipe))
               (graft-form->Pipeline namespace '(defgraft my-graft "My docstring" foo.bar/pipe foo.bar/make-graph a/filter-function another/filter-function)))))

      (testing "with no docstring and a graphfn"
        (is (= (assoc test-graft-form
                      :body '(clojure.core/comp a/filter-function foo.bar/make-graph foo.bar/pipe)
                      :doc "Calls foo.bar/pipe and transforms data into graph data by calling foo.bar/make-graph")
               (graft-form->Pipeline namespace '(defgraft my-graft foo.bar/pipe foo.bar/make-graph a/filter-function)))))

      (is (= (assoc  test-graft-form
                     :body 'foo.bar/pipe
                     :doc "Calls foo.bar/pipe on data and transforms it into to graph data.")
             (graft-form->Pipeline namespace '(defgraft my-graft foo.bar/pipe)))
          "with just 2 macro args"))))

(deftest find-pipelines-test
  (let [forms-seq '((if true "true" "false")
                    (defpipe invalid-pipeline)
                    (defpipe invalid 10)
                    (defpipe valid-pipeline [a b c])
                    (defgraft test-graft "test graft" valid-pipeline make-graph))
        [error another-error pipeline graft] (find-pipelines forms-seq)]

    (is (instance? Exception error))
    (is (instance? Exception another-error))
    (is (instance? grafter.pipeline.Pipeline pipeline))
    (is (= 'test-graft (:name graft)))
    (is (= "test graft" (:doc graft)))
    (is (= (:args pipeline) (:args graft))
        "Should inherit args from earlier pipeline definition")))

(defn write-forms-to [forms dest]
  (with-open [writer (io/writer dest)]
    (doseq [f forms]
      (.write writer (pr-str f)))))

(deftest find-resource-pipelines-test
  (let [pipeline-forms '((defpipe pfirst [a b c] (println "Hello world!"))
                         (defpipe psecond "docs" {:meta true} [d e] (println "Goodbye world!")))
        tmp (File/createTempFile "test" "pipelines.clj")]
    (try
      (write-forms-to pipeline-forms tmp)
      (let [tmp-url (.. tmp toURI toURL)
            read-pipeline-forms (find-resource-pipelines tmp-url)]
        (is (= (count pipeline-forms) (count read-pipeline-forms))))
      (finally
        (.delete tmp)))))
