(ns tools.path
  (:require
    [clojure.string :as str]
    [tools.system :as system])
  (:import
    (java.io
      File)))


(def file-separator File/separator)


(defn user-dir
  []
  (system/get-property "user.dir"))


(defn symbol->path
  [sym]
  (some-> sym
          (str/replace "." file-separator)))
