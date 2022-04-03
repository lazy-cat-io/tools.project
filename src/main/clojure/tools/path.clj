(ns tools.path
  (:require
    [tools.system :as system]))


(defn user-dir
  []
  (system/get-property "user.dir"))
