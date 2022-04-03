(ns tools.git
  (:require
    [tools.process :as process]))


(defn commits-count
  ([]
   (commits-count "HEAD"))
  ([branch]
   (or (->> branch
            (format "git rev-list %s --count")
            (process/execute))
       "0")))


(defn url
  []
  (process/execute "git config --get remote.origin.url"))


(defn branch
  []
  (process/execute "git rev-parse --abbrev-ref HEAD"))


(defn sha
  []
  (process/execute "git rev-parse HEAD"))
