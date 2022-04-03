(ns tools.print
  (:require
    [clojure.pprint :as pprint]))


(defn pretty
  ([x]
   (pretty x {}))
  ([x {:keys [right-margin]
       :or   {right-margin 130}}]
   (binding [pprint/*print-right-margin* right-margin]
     (pprint/pprint x))))
