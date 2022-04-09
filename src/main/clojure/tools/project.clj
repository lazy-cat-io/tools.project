(ns tools.project
  (:require
    [aero.core :as aero]
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


(defmacro safe
  ([body] `(try ~body (catch Exception _#)))
  ([body handler] `(try ~body (catch Exception e# (~handler e#)))))


(defmethod aero/reader 'git
  [_ _ x]
  (process/execute (format "git %s" x)))


(defmethod aero/reader 'zoned-date-time
  [_ _ x]
  (datetime/format
    (datetime/zoned-date-time)
    (or (get formatter/formatters x)
        (formatter/of-pattern x))))


(defmethod aero/reader 'case
  [_ _ [expression {:as clauses :keys [default]}]]
  (reduce
    (fn [acc [k v]]
      (or (and (= expression k) (reduced v))
          (and (set? k) (contains? k expression) (reduced v))
          (safe ; quote regex?
           (and (string? k) (re-matches (re-pattern k) expression) (reduced v)))
          acc))
    default (dissoc clauses :default)))


(defn read-edn
  [^File file]
  (when (and file (.exists file))
    (aero/read-config file)))


(def project-filename "project.edn")
(def config-dirname ".tools.project")
(def config-filename "config.edn")
(def build-filename "build.edn")


(defn read-default-config
  []
  (some->> "io/lazy-cat/tools/project/config.edn"
           (io/resource)
           (io/file)
           (read-edn)))


(defn read-user-config
  []
  (some->> config-filename
           (io/file (path/user-dir) config-dirname)
           (read-edn)))


(defn read-user-home-config
  []
  (some->> config-filename
           (io/file (path/user-home) config-dirname)
           (read-edn)))


(defn read-config
  []
  (merge-with
    merge
    (read-default-config) ; remove this?
    (read-user-home-config)
    (read-user-config)))


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
   (read-project (io/file (path/user-dir) project-filename) (read-config)))
  ([path]
   (read-project path (read-config)))
  ([path config]
   (some->> path
            (io/file)
            (read-edn)
            (resolve-project-variables config))))


(defn build-info-file-path
  [{:as project {:keys [lib resource-dirs]} :build}]
  (let [lib           (or lib (:name project))
        group-id      (some-> lib (namespace) (symbol))
        artifact-id   (some-> lib (name) (symbol))
        lib-path      (if (= group-id artifact-id)
                        [(path/symbol->path artifact-id)]
                        (keep path/symbol->path [group-id artifact-id]))
        resource-dirs (first resource-dirs)]
    (->> [resource-dirs lib-path build-filename]
         (flatten)
         (str/join path/file-separator))))


(defn write-build-info
  [project]
  (let [file (io/file (build-info-file-path project))]
    (->> (dissoc project :build)
         (print/pretty)
         (with-out-str)
         (path/ensure-file file))))


(defn project->tools-build-opts
  [project]
  (let [lib     (:name project)
        version (:version project)]
    (some-> project
            (:build)
            (assoc :lib lib :version version))))


(comment
  (read-config)
  (read-project)
  (build-info-file-path (read-project))
  (write-build-info (read-project))
  (project->tools-build-opts (read-project))
  )
