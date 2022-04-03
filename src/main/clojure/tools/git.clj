(ns tools.git
  (:require
    [tools.process :as process]))


(defn commits-count
  ([]
   (commits-count "HEAD"))
  ([branch]
   (->> branch
        (format "git rev-list %s --count")
        (process/execute))))


(defn url
  []
  (process/execute "git config --get remote.origin.url"))


(defn branch
  []
  (process/execute "git rev-parse --abbrev-ref HEAD"))


(defn sha
  ([]
   (sha :short))
  ([type]
   (case type
     :short (process/execute "git rev-parse --short HEAD")
     (process/execute "git rev-parse HEAD"))))
