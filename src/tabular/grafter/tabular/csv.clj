(ns grafter.tabular.csv
  {:no-doc true}
  (:require [clojure-csv.core :as csv]
            [clojure.java.io :as io]
            [grafter.tabular.common :as tab]))

(defmethod tab/open-tabular-file :csv
  [f & {:as opts}]
  (tab/mapply csv/parse-csv (io/reader f) opts))

(defmethod tab/write-tabular-file! :csv
  [dataset filename]
  (with-open [^java.io.BufferedWriter w (io/writer filename)]
    (let [^String header-string (csv/write-csv [(map name (tab/column-names dataset))])]
      (.write w header-string))
    (doseq [row (:rows dataset)]
      (let [^String row-string (csv/write-csv [(vals row)])]
          (.write w row-string)))))
