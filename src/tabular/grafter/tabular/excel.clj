(ns grafter.tabular.excel
  {:no-doc true}
  (:require [clj-excel.core :as xls]
            [grafter.tabular.common :as tab]))

(defn- get-sheet [worksheets sheet]
  (if (nil? sheet)
    (first worksheets)
    (get worksheets sheet)))

(defmethod tab/open-dataset :xls
  [filename & {:keys [sheet] :as opts}]
  (-> filename
      xls/workbook-hssf
      xls/lazy-workbook
      (get-sheet sheet)
      tab/make-dataset))

(defmethod tab/open-dataset :xlsx
  [filename & {:keys [sheet] :as opts}]
  (-> filename
      xls/workbook-xssf
      xls/lazy-workbook
      (get-sheet sheet)
      tab/make-dataset))

(defmethod tab/open-datasets :xls
  [filename & opts]
  (-> filename
      xls/workbook-xssf
      xls/lazy-workbook
      (#(zipmap (keys %1) (map tab/make-dataset (vals %1))))))
