(ns tools.project
  (:require
    [clojure.edn :as edn]
    [clojure.java.io :as io]
    [selmer.parser :as selmer]
    [tools.datetime :as datetime]
    [tools.datetime.formatter :as formatter]
    [tools.git :as git]
    [tools.path :as path])
  (:import
    (clojure.lang
      PersistentArrayMap
      PersistentHashMap)))


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


;; TODO: [2022-04-04, ilshat@sultanov.team] Add :major, :minor, :patch variables from git tags
;; $ git tag --list --sort=-version:refname *

(defn variables
  ([]
   (variables {}))
  ([manifest]
   (let [now (datetime/zoned-date-time)]
     {:build-at     (build-at)
      :build-number (build-number)
      :git-url      (git-url manifest)
      :git-branch   (git-branch)
      :git-sha      (git-sha)
      :year         (datetime/format now (formatter/of-pattern "YYYY"))
      :month        (datetime/format now (formatter/of-pattern "MM"))
      :day          (datetime/format now (formatter/of-pattern "dd"))})))


(defprotocol Versionable
  (build-version [this] [this variables]))


(extend-protocol Versionable
  nil
  (build-version
    ([_] nil)
    ([_ _] nil))

  String
  (build-version
    ([s] (build-version s (variables)))
    ([s variables] (selmer/render s variables)))

  PersistentArrayMap
  (build-version
    ([m] (build-version m (variables)))
    ([m variables] (some-> m :template (build-version variables))))

  PersistentHashMap
  (build-version
    ([m] (build-version m (variables)))
    ([m variables] (some-> m :template (build-version variables)))))



;; TODO: Add helpers
;; - to show previous versions
;; - to calculate the next version

(defn version
  ([]
   (version {}))
  ([manifest]
   (version manifest (variables manifest)))
  ([manifest variables]
   (build-version (:version manifest) variables)))


(defn metadata
  ([manifest]
   (metadata manifest (variables manifest)))
  ([manifest {:as   variables
              :keys [build-at build-number git-url git-branch git-sha]}]
   (let [metadata {:version      (version manifest variables)
                   :build-at     build-at
                   :build-number build-number
                   :git-url      git-url
                   :git-branch   git-branch
                   :git-sha      git-sha}]
     (merge manifest metadata))))
