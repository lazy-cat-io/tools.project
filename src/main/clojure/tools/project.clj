(ns tools.project
  (:require
    [clojure.edn :as edn]
    [clojure.java.io :as io]
    [clojure.string :as str]
    [clojure.tools.build.util.file :as file]
    [clojure.walk :as walk]
    [selmer.parser :as selmer]
    [tools.datetime :as datetime]
    [tools.datetime.formatter :as formatter]
    [tools.path :as path]
    [tools.print :as print]
    [tools.process :as process])
  (:import
    (java.io
      File)))


(defn root-directory
  []
  (path/user-dir))


(defn read-edn
  [^File file]
  (when (and file (.exists file))
    (-> file slurp edn/read-string)))


(defn read-default-config
  []
  (some->> (io/resource "tools.project/config.edn")
           (io/file)
           (read-edn)))


(defn read-user-config
  []
  (some->> (or (io/file (root-directory) ".tp" "config.edn")
               (io/file (root-directory) ".tools.project" "config.edn"))
           (read-edn)))


(defn with-config-defaults
  [config]
  (let [now (datetime/zoned-date-time)]
    (update config :variables assoc
            :build/created-at (datetime/format now)
            :datetime/year (datetime/format now (formatter/of-pattern "YYYY"))
            :datetime/month (datetime/format now (formatter/of-pattern "MM"))
            :datetime/day (datetime/format now (formatter/of-pattern "dd"))
            :datetime/hour (datetime/format now (formatter/of-pattern "HH"))
            :datetime/minute (datetime/format now (formatter/of-pattern "mm"))
            :datetime/second (datetime/format now (formatter/of-pattern "SS")))))


(defn build-config
  [config]
  (update config :variables
          (fn [variables]
            (reduce-kv
              (fn [acc variable command]
                (let [value (cond
                              (string? command) (process/execute command)
                              (vector? command) (let [[command & {:keys [default]}] command]
                                                  (or (process/execute command) default)))]
                  (assoc acc variable value)))
              {} variables))))


(defn read-config
  []
  (-> (merge-with
        merge
        (read-default-config)
        (read-user-config))
      (build-config)
      (with-config-defaults)))


(def export-keys
  [:build/created-at
   :build/number
   :git/url
   :git/branch
   :git/sha
   :git/commit-message
   :git/committer-timestamp
   :git/committer-name
   :git/committer-email
   :git/author-timestamp
   :git/author-name
   :git/author-email
   :git/commits-count])


(defn with-project-defaults
  [config project]
  (let [project-name (:name project)
        extra-data   (-> config (:variables) (select-keys export-keys))
        git-tag      (:version project)
        build        (cond-> {}
                       (qualified-symbol? project-name) (assoc :mvn/group-id (-> project-name (namespace) (symbol)))
                       (symbol? project-name) (assoc :mvn/artifact-id (-> project-name (name) symbol))
                       :always (-> (assoc :git/tag git-tag) (merge extra-data)))]
    (assoc project :build build)))


(defn build-project
  [config project]
  (walk/postwalk
    (fn [form]
      (if-not (string? form)
        form
        (selmer/render form (:variables config))))
    project))


(defn read-project
  ([]
   (read-project (io/file (root-directory) "project.edn")))
  ([path]
   (read-project path (read-config)))
  ([path config]
   (some->> path
            (io/file)
            (read-edn)
            (build-project config)
            (with-project-defaults config))))



(defn build-path
  [{{:mvn/keys [group-id artifact-id]} :build}]
  (let [path (if (= group-id artifact-id)
               [(path/symbol->path artifact-id)]
               (keep path/symbol->path [group-id artifact-id]))]
    (->> ["target" "tools.project" "META-INF" path "build.edn"]
         (flatten)
         (str/join path/file-separator))))


(defn write-build-file
  [project]
  (let [file (io/file (build-path project))]
    (->> project
         (print/pretty)
         (with-out-str)
         (file/ensure-file file))))


(comment
  (read-config)
  (read-project)
  (write-build-file (read-project))
  )
