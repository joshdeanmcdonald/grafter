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
    (let [header-names (map name (tab/column-names dataset))]
      (let [^String header-csv (csv/write-csv [header-names])]
        (.write w header-csv))
      (doseq [row (:rows dataset)]
        (let [row-strings (map (fn[col] (str (get row col))) header-names)
              ^String row-csv (csv/write-csv [row-strings])]
          (.write w row-csv))))))
