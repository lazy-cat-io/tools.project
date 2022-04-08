(ns tools.project
  (:require
    [aero.core :as aero]
    [clojure.edn :as edn]
    [clojure.java.io :as io]
    [clojure.string :as str]
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


(defmethod aero/reader 'sh
  [_ _ x]
  (try
    (process/execute x)
    (catch Exception _)))


(defn root-directory
  []
  (path/user-dir))


(defn read-edn
  [^File file]
  (when (and file (.exists file))
    (aero/read-config file)))


(defn read-default-config
  []
  (some->> (io/resource "io/lazy-cat/tools/project/config.edn")
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
            :year (datetime/format now (formatter/of-pattern "YYYY"))
            :month (datetime/format now (formatter/of-pattern "MM"))
            :day (datetime/format now (formatter/of-pattern "dd"))
            :hour (datetime/format now (formatter/of-pattern "HH"))
            :minute (datetime/format now (formatter/of-pattern "mm"))
            :second (datetime/format now (formatter/of-pattern "SS")))))


(defn read-config
  []
  (-> (merge-with
        merge
        (read-default-config)
        (read-user-config))
      (with-config-defaults)))


(defn with-project-defaults
  [config project]
  (let [project-name (:name project)
        tag          (:version project)
        export-keys  (get-in config [:build :export-keys] [])
        build        (cond-> (:variables config)
                       (qualified-symbol? project-name) (assoc :mvn/group-id (-> project-name (namespace) (symbol)))
                       (symbol? project-name) (assoc :mvn/artifact-id (-> project-name (name) symbol))
                       :always (-> (assoc :git/tag tag) (select-keys export-keys)))]
    (-> project
        (assoc :build build)
        (with-meta {::config config}))))


(defn resolve-project-variables
  [config project]
  (walk/postwalk
    (fn [form]
      (if-not (string? form)
        form
        (selmer/render form (:variables config))))
    project))


(defn read-project
  ([]
   (read-project (io/file (root-directory) "project.edn") (read-config)))
  ([path]
   (read-project path (read-config)))
  ([path config]
   (some->> path
            (io/file)
            (read-edn)
            (resolve-project-variables config)
            (with-project-defaults config))))


(defn build-info-file-path
  [{:as project {:mvn/keys [group-id artifact-id]} :build}]
  (let [resource-dirs (some-> project (meta) ::config :build :resource-dirs)
        path          (if (= group-id artifact-id)
                        [(path/symbol->path artifact-id)]
                        (keep path/symbol->path [group-id artifact-id]))]
    (->> [resource-dirs path "build.edn"]
         (flatten)
         (str/join path/file-separator))))


(defn write-build-info
  [project]
  (let [file (io/file (build-info-file-path project))]
    (->> project
         (print/pretty)
         (with-out-str)
         (path/ensure-file file))))


(defn project->tools-build-opts
  [project]
  (let [lib     (:name project)
        version (:version project)
        url     (or (get-in project [:repository :url])
                    (get-in project [:build :git/url]))
        scm     {:url url, :tag version}]
    (some-> project
            (meta)
            (::config)
            (:build)
            (dissoc :export-keys)
            (assoc :lib lib :version version :scm scm))))


(comment
  (read-config)
  (read-project)
  (write-build-info (read-project))
  (project->tools-build-opts (read-project))
  )
