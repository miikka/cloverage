(ns cloverage.report.codecov
  (:require
   [clojure.java.io :as io]
   [clojure.data.json :as json]
   [cloverage.report :refer [line-stats with-out-writer]]))

(defn- file-coverage [[file file-forms]]
  ;; https://docs.codecov.io/reference#section-codecov-json-report-format
  ;; 0: not covered
  ;; "1/3": partially covered, 1 form out of 3 covered
  ;; > 0: covered (number of times hit)
  ;; null: skipped/ignored/empty
  (vector file
          (into {}
                (map (fn [line]
                       [(str (:line line))
                        (cond (:blank?   line) nil
                              (:covered? line) (:times-hit line)
                              (:partial? line) (str (:hit line) "/" (:total line))
                              (:instrumented? line) 0
                              :else nil)]))
                (line-stats file-forms)) ))

(defn report [^String out-dir forms]
  (let [output-file (io/file out-dir "codecov.json")
        covdata (->>
                 forms
                 (group-by :file)
                 (filter first)
                 (map file-coverage)
                 (into {}))]

    (println "Writing codecov.io report to:" (.getAbsolutePath output-file))
    (with-out-writer output-file
      (json/pprint {:coverage covdata} :escape-slash false))))
