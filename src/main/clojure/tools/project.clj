(ns tools.project
  (:require
    [clojure.edn :as edn]
    [clojure.java.io :as io]
    [tools.datetime :as datetime]
    [tools.datetime.formatter :as formatter]
    [tools.git :as git]
    [tools.path :as path]))


(def filename "project.edn")


(defn root
  []
  (path/user-dir))


(defn read-manifest
  ([]
   (read-manifest (io/file (root) filename)))
  ([path]
   (when-some [file (io/file path)]
     (when (.exists file)
       (-> file slurp edn/read-string)))))


(defn build-at
  []
  (datetime/format (datetime/zoned-date-time)))


(defn build-number
  ([]
   (git/commits-count "HEAD"))
  ([branch]
   (git/commits-count branch)))


(defn git-url
  [manifest]
  (or (get-in manifest [:repository :url])
      (git/url)))


(defn git-branch
  []
  (git/branch))


(defn git-sha
  []
  (git/sha))


(defn metadata
  ([]
   (metadata {}))
  ([manifest]
   (let [now       (datetime/zoned-date-time)
         variables {:build-at     (build-at)
                    :build-number (build-number)
                    :git-url      (git-url manifest)
                    :git-branch   (git-branch)
                    :git-sha      (git-sha)
                    :year         (datetime/format now (formatter/of-pattern "YYYY"))
                    :month        (datetime/format now (formatter/of-pattern "MM"))
                    :day          (datetime/format now (formatter/of-pattern "dd"))}]
     (assoc manifest :variables variables))))
